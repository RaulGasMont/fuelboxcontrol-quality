package com.gasmonsoft.fbccalidad.data.datasource.usb

import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.util.Base64
import android.util.Log
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import com.hoho.android.usbserial.util.SerialInputOutputManager
import kotlinx.coroutines.CoroutineScope
import java.util.concurrent.Executors
import javax.inject.Inject
import kotlin.text.Charsets.UTF_8

/**
 * UsbOtgReader — Lee archivos del ESP32 vía USB OTG.
 *
 * Protocolo base64 por print():
 *   "STX:FILE:<nombre>:<tamaño>"   → header
 *   "DAT:<seq>:<base64>"           → chunks del archivo
 *   "ETX"                          → fin de transmisión
 *
 * Cada chunk base64 decodificado puede contener múltiples líneas de sensor.
 * Se emiten línea por línea vía onFrameReady para que el pipeline
 * (BleLineAssembler → purger) las procese correctamente.
 */
class UsbOtgReader @Inject constructor(
    private val usbManager: UsbManager
) {

    private var port: UsbSerialPort? = null
    private var ioManager: SerialInputOutputManager? = null
    private val executor = Executors.newSingleThreadExecutor()

    private val lineBuffer = StringBuilder()
    private val lock = Any()

    // Callbacks
    var onHeaderReceived: ((fileName: String, fileSize: Int) -> Unit)? = null
    var onFrameReady: ((ByteArray) -> Unit)? = null
    var onError: ((String) -> Unit)? = null
    var onTransmissionEnd: (() -> Unit)? = null
    var onProgress: ((bytesReceived: Int, totalBytes: Int) -> Unit)? = null

    // Estado
    private var expectedFileSize = 0
    private var receivedBytes = 0
    private var transferActive = false

    // ─── Conexión ──────────────────────────────────────────────────────────

    fun connect(device: UsbDevice): Boolean {
        try {
            val drivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager)
            val driver = drivers.firstOrNull { it.device.deviceId == device.deviceId }
                ?: run {
                    onError?.invoke("No se encontró driver para el dispositivo USB")
                    return false
                }

            val connection = usbManager.openDevice(driver.device)
                ?: run {
                    onError?.invoke("Sin permiso USB o no se pudo abrir el dispositivo")
                    return false
                }

            val serialPort = driver.ports[0]
            serialPort.open(connection)

            val negotiatedBaud = negotiateBaudRate(serialPort)

            try {
                serialPort.dtr = true
                serialPort.rts = true
            } catch (e: Exception) {
                Log.w(TAG, "DTR/RTS no disponibles: ${e.message}")
            }

            port = serialPort
            resetState()

            Log.i(
                TAG,
                "Conectado: ${driver.javaClass.simpleName}, $negotiatedBaud baud (negociado)"
            )
            return true

        } catch (e: Exception) {
            Log.e(TAG, "Error al conectar: ${e.message}", e)
            onError?.invoke("Error al conectar: ${e.message}")
            return false
        }
    }

    private fun negotiateBaudRate(serialPort: UsbSerialPort): Int {
        Log.i(TAG, "Negociación de baud rate iniciada...")

        try {
            serialPort.setParameters(
                PREFERRED_BAUD_RATE, 8,
                UsbSerialPort.STOPBITS_1,
                UsbSerialPort.PARITY_NONE
            )
            Log.i(TAG, "  → $PREFERRED_BAUD_RATE baud: OK")
            return PREFERRED_BAUD_RATE
        } catch (e: Exception) {
            Log.w(TAG, "  → $PREFERRED_BAUD_RATE baud: FALLÓ (${e.message})")
        }

        try {
            serialPort.setParameters(
                FALLBACK_BAUD_RATE, 8,
                UsbSerialPort.STOPBITS_1,
                UsbSerialPort.PARITY_NONE
            )
            Log.i(TAG, "  → Fallback a $FALLBACK_BAUD_RATE baud: OK")
            return FALLBACK_BAUD_RATE
        } catch (e: Exception) {
            Log.e(TAG, "  → $FALLBACK_BAUD_RATE baud: FALLÓ (${e.message})")
            throw e
        }
    }

    private fun resetState() {
        synchronized(lock) { lineBuffer.clear() }
        expectedFileSize = 0
        receivedBytes = 0
        transferActive = false
    }

    // ─── Lectura ───────────────────────────────────────────────────────────

    fun startReading(scope: CoroutineScope) {
        val serialPort = port ?: run {
            onError?.invoke("Sin conexión activa")
            return
        }

        Log.i(TAG, "Lectura iniciada — esperando datos del ESP32...")

        ioManager = SerialInputOutputManager(
            serialPort,
            object : SerialInputOutputManager.Listener {

                override fun onNewData(data: ByteArray) {
                    if (data.isEmpty()) return
                    processIncomingBytes(data)
                }

                override fun onRunError(e: Exception) {
                    Log.e(TAG, "Error UART: ${e.message}")
                    onError?.invoke("Error UART: ${e.message}")
                }
            }
        ).also {
            executor.submit(it)
        }
    }

    // ─── Procesamiento líneas → decodificación ─────────────────────────────

    private fun processIncomingBytes(data: ByteArray) {
        synchronized(lock) {
            for (b in data) {
                val c = b.toInt() and 0xFF
                if (c == '\n'.code || c == '\r'.code) {
                    if (lineBuffer.isNotEmpty()) {
                        processLine(lineBuffer.toString())
                        lineBuffer.setLength(0)
                    }
                } else {
                    lineBuffer.append(c.toChar())
                }
            }
        }
    }

    private fun processLine(line: String) {
        Log.d(TAG, line)
        when {
            line.startsWith("STX:FILE:") -> {
                val payload = line.removePrefix("STX:FILE:")
                val parts = payload.split(":")
                if (parts.size >= 2) {
                    val fileName = parts[0]
                    expectedFileSize = parts[1].toIntOrNull() ?: 0
                    receivedBytes = 0
                    transferActive = true

                    Log.i(
                        TAG,
                        "Header recibido — archivo: $fileName, tamaño: $expectedFileSize bytes"
                    )
                    onHeaderReceived?.invoke(fileName, expectedFileSize)
                } else {
                    Log.w(TAG, "Header malformado: $line")
                }
            }

            line.startsWith("ETX") && transferActive -> {
                transferActive = false
                Log.i(TAG, "ETX recibido — $receivedBytes / $expectedFileSize bytes")
                stopReading()
                onTransmissionEnd?.invoke()
            }

            line.startsWith("DAT:") && transferActive -> {
                val payload = line.removePrefix("DAT:")
                val sep = payload.indexOf(':')
                if (sep < 0) {
                    Log.w(TAG, "DAT mal formado: $line")
                    return
                }

                val b64 = payload.substring(sep + 1)
                try {
                    val decoded = Base64.decode(b64, Base64.DEFAULT)
                    receivedBytes += decoded.size

                    onFrameReady?.invoke(decoded)

                    val seq = payload.substring(0, sep).toIntOrNull() ?: 0
                    if (seq % 50 == 0 && expectedFileSize > 0) {
                        Log.d(TAG, "Progreso: $receivedBytes / $expectedFileSize bytes (seq=$seq)")
                        onProgress?.invoke(receivedBytes, expectedFileSize)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error decodificando base64: ${e.message}")
                }
            }

            line.startsWith("cal:") -> {
                Log.d(TAG, "Procesando línea de calibración: $line")
                onFrameReady?.invoke(line.toByteArray(UTF_8))
            }

            else -> {
                if (line.isNotBlank()) {
                    Log.d(TAG, "Línea cruda recibida: $line")
                    onFrameReady?.invoke(line.toByteArray(UTF_8))
                }
            }
        }
    }

    // ─── Envío (opcional) ──────────────────────────────────────────────────

    fun sendCommand(command: String): Boolean {
        val serialPort = port ?: return false
        return try {
            serialPort.write(command.toByteArray(UTF_8), 1000)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error al enviar: ${e.message}")
            false
        }
    }

    fun sendData(data: ByteArray): Boolean {
        val serialPort = port ?: return false
        return try {
            serialPort.write(data, 1000)
            true
        } catch (e: Exception) {
            false
        }
    }

    // ─── Limpieza ──────────────────────────────────────────────────────────

    fun stopReading() {
        ioManager?.stop()
        ioManager = null
    }

    fun disconnect() {
        stopReading()
        try {
            port?.close()
        } catch (_: Exception) {
        }
        port = null
        synchronized(lock) { lineBuffer.clear() }
        Log.i(TAG, "Desconectado")
    }

    val isConnected: Boolean
        get() = port != null

    companion object {
        private const val TAG = "Esp32Usb"
        private const val PREFERRED_BAUD_RATE = 115200
        private const val FALLBACK_BAUD_RATE = 115200
    }
}
