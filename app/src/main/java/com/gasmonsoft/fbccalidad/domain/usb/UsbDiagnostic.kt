package com.gasmonsoft.fbccalidad.domain.usb

import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbManager
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject

/**
 * Herramienta de diagnóstico para identificar POR QUÉ el ESP32
 * no responde después de enviar un comando.
 *
 * Uso:
 *   val diag = UsbDiagnostic(usbManager)
 *   diag.runFullDiagnostic(device)
 *
 * Revisa Logcat con tag "UsbDiag" para ver los resultados.
 */
class UsbDiagnostic @Inject constructor(private val usbManager: UsbManager) {

    companion object {
        private const val TAG = "UsbDiag"
        private const val REQTYPE_HOST_TO_DEVICE = 0x41
        private const val REQTYPE_DEVICE_TO_HOST = 0xC1
        private const val CP210X_IFC_ENABLE = 0x00
        private const val CP210X_SET_BAUDRATE = 0x1E
        private const val CP210X_GET_BAUDRATE = 0x1D  // Leer baud rate actual
        private const val CP210X_SET_LINE_CTL = 0x03
        private const val CP210X_SET_MHS = 0x07
        private const val CP210X_GET_MDMSTS = 0x08    // Leer estado de señales
    }

    fun runFullDiagnostic(device: UsbDevice) {
        Log.i(TAG, "╔══════════════════════════════════════════════════╗")
        Log.i(TAG, "║         DIAGNÓSTICO USB - ESP32 via CP2102       ║")
        Log.i(TAG, "╚══════════════════════════════════════════════════╝")

        // ── Paso 1: Info del dispositivo ──
        logSection("1. INFORMACIÓN DEL DISPOSITIVO")
        Log.i(TAG, "  Nombre:      ${device.productName}")
        Log.i(
            TAG,
            "  Vendor ID:   0x${device.vendorId.toString(16).uppercase()} (${device.vendorId})"
        )
        Log.i(
            TAG,
            "  Product ID:  0x${device.productId.toString(16).uppercase()} (${device.productId})"
        )
        Log.i(TAG, "  Interfaces:  ${device.interfaceCount}")
        Log.i(TAG, "  Device Class: ${device.deviceClass}")

        for (i in 0 until device.interfaceCount) {
            val iface = device.getInterface(i)
            Log.i(
                TAG, "  Interfaz $i: class=${iface.interfaceClass}, " +
                        "subclass=${iface.interfaceSubclass}, endpoints=${iface.endpointCount}"
            )
            for (j in 0 until iface.endpointCount) {
                val ep = iface.getEndpoint(j)
                val dir = if (ep.direction == UsbConstants.USB_DIR_IN) "IN" else "OUT"
                val type = when (ep.type) {
                    UsbConstants.USB_ENDPOINT_XFER_BULK -> "BULK"
                    UsbConstants.USB_ENDPOINT_XFER_INT -> "INTERRUPT"
                    UsbConstants.USB_ENDPOINT_XFER_CONTROL -> "CONTROL"
                    else -> "ISOCHRONOUS"
                }
                Log.i(
                    TAG, "    EP $j: $type $dir, address=0x${ep.address.toString(16)}, " +
                            "maxPacket=${ep.maxPacketSize}"
                )
            }
        }

        // ── Paso 2: Abrir conexión ──
        logSection("2. ABRIR CONEXIÓN")
        if (!usbManager.hasPermission(device)) {
            Log.e(TAG, "  ✗ Sin permiso USB — solicítalo primero")
            return
        }
        Log.i(TAG, "  ✓ Permiso USB concedido")

        val iface = device.getInterface(0)
        val conn = usbManager.openDevice(device)
        if (conn == null) {
            Log.e(TAG, "  ✗ openDevice() retornó null")
            return
        }
        Log.i(TAG, "  ✓ openDevice() OK")

        if (!conn.claimInterface(iface, true)) {
            Log.e(TAG, "  ✗ claimInterface() falló")
            conn.close()
            return
        }
        Log.i(TAG, "  ✓ claimInterface() OK")

        // Encontrar endpoints
        var epIn: UsbEndpoint? = null
        var epOut: UsbEndpoint? = null
        for (i in 0 until iface.endpointCount) {
            val ep = iface.getEndpoint(i)
            if (ep.type == UsbConstants.USB_ENDPOINT_XFER_BULK) {
                if (ep.direction == UsbConstants.USB_DIR_IN) epIn = ep
                if (ep.direction == UsbConstants.USB_DIR_OUT) epOut = ep
            }
        }
        Log.i(
            TAG,
            "  EP IN:  ${epIn?.let { "0x${it.address.toString(16)}, maxPacket=${it.maxPacketSize}" } ?: "NO ENCONTRADO"}")
        Log.i(
            TAG,
            "  EP OUT: ${epOut?.let { "0x${it.address.toString(16)}, maxPacket=${it.maxPacketSize}" } ?: "NO ENCONTRADO"}")

        if (epIn == null || epOut == null) {
            Log.e(TAG, "  ✗ Faltan endpoints — no se puede continuar")
            conn.close()
            return
        }

        // ── Paso 3: Configurar CP2102 con verificación ──
        logSection("3. CONFIGURAR CP2102")

        // 3a. Habilitar UART
        var res = conn.controlTransfer(
            REQTYPE_HOST_TO_DEVICE,
            CP210X_IFC_ENABLE,
            0x0001,
            0,
            null,
            0,
            2000
        )
        Log.i(TAG, "  IFC_ENABLE:    ${if (res >= 0) "✓ OK ($res)" else "✗ FALLÓ ($res)"}")

        // 3b. Configurar baud rate
        val baudData = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(115200).array()
        res = conn.controlTransfer(
            REQTYPE_HOST_TO_DEVICE,
            CP210X_SET_BAUDRATE,
            0,
            0,
            baudData,
            4,
            2000
        )
        Log.i(TAG, "  SET_BAUDRATE:  ${if (res >= 0) "✓ OK ($res)" else "✗ FALLÓ ($res)"}")

        // 3c. VERIFICAR baud rate — leer lo que realmente quedó configurado
        val baudCheck = ByteArray(4)
        res = conn.controlTransfer(
            REQTYPE_DEVICE_TO_HOST,
            CP210X_GET_BAUDRATE,
            0,
            0,
            baudCheck,
            4,
            2000
        )
        if (res >= 0) {
            val actualBaud = ByteBuffer.wrap(baudCheck).order(ByteOrder.LITTLE_ENDIAN).int
            Log.i(TAG, "  GET_BAUDRATE:  ✓ Baud rate real del CP2102 = $actualBaud")
            if (actualBaud != 115200) {
                Log.w(
                    TAG,
                    "  ⚠ BAUD RATE NO COINCIDE — configuraste 115200 pero el chip reporta $actualBaud"
                )
                Log.w(
                    TAG,
                    "    Esto puede causar datos corruptos o que el ESP32 no entienda los comandos"
                )
            }
        } else {
            Log.w(TAG, "  GET_BAUDRATE:  No se pudo leer ($res) — no crítico")
        }

        // 3d. 8N1
        res = conn.controlTransfer(
            REQTYPE_HOST_TO_DEVICE,
            CP210X_SET_LINE_CTL,
            0x0800,
            0,
            null,
            0,
            2000
        )
        Log.i(TAG, "  SET_LINE_CTL:  ${if (res >= 0) "✓ OK ($res)" else "✗ FALLÓ ($res)"}")

        // 3e. DTR + RTS
        res = conn.controlTransfer(REQTYPE_HOST_TO_DEVICE, CP210X_SET_MHS, 0x0303, 0, null, 0, 2000)
        Log.i(TAG, "  SET_MHS:       ${if (res >= 0) "✓ OK ($res)" else "✗ FALLÓ ($res)"}")

        // 3f. Leer estado de señales del modem
        val modemStatus = ByteArray(1)
        res = conn.controlTransfer(
            REQTYPE_DEVICE_TO_HOST,
            CP210X_GET_MDMSTS,
            0,
            0,
            modemStatus,
            1,
            2000
        )
        if (res >= 0) {
            val status = modemStatus[0].toInt() and 0xFF
            val dsr = (status and 0x20) != 0  // Data Set Ready (ESP32 presente)
            val cts = (status and 0x10) != 0  // Clear To Send
            val ri = (status and 0x40) != 0  // Ring Indicator
            val dcd = (status and 0x80) != 0  // Data Carrier Detect
            Log.i(
                TAG,
                "  MODEM STATUS:  DSR=$dsr, CTS=$cts, RI=$ri, DCD=$dcd (raw=0x${status.toString(16)})"
            )
            if (!dsr && !cts) {
                Log.w(TAG, "  ⚠ DSR y CTS están OFF — esto puede indicar que:")
                Log.w(TAG, "    • El ESP32 no está encendido")
                Log.w(TAG, "    • Los cables TX/RX están desconectados o invertidos")
                Log.w(TAG, "    • El ESP32 no tiene firmware corriendo en el UART")
            }
        }

        // ── Paso 4: Test de loopback (el ESP32 responde?) ──
        logSection("4. TEST DE COMUNICACIÓN")

        // 4a. Vaciar buffer
        val flush = ByteArray(512)
        var flushed = 0
        while (conn.bulkTransfer(epIn, flush, flush.size, 100) > 0) {
            flushed++
        }
        Log.i(TAG, "  Buffer vaciado ($flushed lecturas descartadas)")

        // 4b. Intentar leer SIN enviar nada (ver si el ESP32 envía datos espontáneos)
        Log.i(TAG, "  Leyendo sin enviar nada (1 segundo)...")
        val spontaneous = ByteArray(512)
        val spontRead = conn.bulkTransfer(epIn, spontaneous, spontaneous.size, 1000)
        if (spontRead > 0) {
            val text = String(spontaneous, 0, spontRead, Charsets.UTF_8)
            Log.i(TAG, "  ✓ ESP32 envía datos espontáneos ($spontRead bytes): \"$text\"")
            Log.i(TAG, "    Esto sugiere que el ESP32 está vivo y transmitiendo")
        } else {
            Log.i(
                TAG,
                "  · No hay datos espontáneos (retornó $spontRead) — normal si el ESP32 espera un comando"
            )
        }

        // 4c. Enviar un simple newline y ver si hay eco o respuesta
        Log.i(TAG, "  Enviando '\\n' simple...")
        val newline = "\n".toByteArray()
        val sentNl = conn.bulkTransfer(epOut, newline, newline.size, 1000)
        Log.i(TAG, "  Envío \\n: ${if (sentNl >= 0) "OK ($sentNl bytes)" else "FALLÓ ($sentNl)"}")

        Thread.sleep(200)
        val nlResp = ByteArray(512)
        val nlRead = conn.bulkTransfer(epIn, nlResp, nlResp.size, 2000)
        if (nlRead > 0) {
            val text = String(nlResp, 0, nlRead, Charsets.UTF_8)
            Log.i(TAG, "  ✓ Respuesta a \\n ($nlRead bytes): \"$text\"")
        } else {
            Log.i(TAG, "  · Sin respuesta a \\n ($nlRead)")
        }

        // 4d. Enviar "PING\n" básico
        Log.i(TAG, "  Enviando 'PING\\n'...")
        val ping = "PING\n".toByteArray()
        val sentPing = conn.bulkTransfer(epOut, ping, ping.size, 1000)
        Log.i(
            TAG,
            "  Envío PING: ${if (sentPing >= 0) "OK ($sentPing bytes)" else "FALLÓ ($sentPing)"}"
        )

        Thread.sleep(300)
        val pingResp = ByteArray(512)
        val pingRead = conn.bulkTransfer(epIn, pingResp, pingResp.size, 2000)
        if (pingRead > 0) {
            val text = String(pingResp, 0, pingRead, Charsets.UTF_8)
            Log.i(TAG, "  ✓ Respuesta a PING ($pingRead bytes): \"$text\"")
        } else {
            Log.i(TAG, "  ✗ Sin respuesta a PING ($pingRead)")
        }

        // 4e. Enviar el comando GET real
        val getCmd = "GET 2026/04/18.bin\n"
        Log.i(TAG, "  Enviando '$getCmd'...")
        val getBytes = getCmd.toByteArray()
        val sentGet = conn.bulkTransfer(epOut, getBytes, getBytes.size, 1000)
        Log.i(
            TAG,
            "  Envío GET: ${if (sentGet >= 0) "OK ($sentGet bytes)" else "FALLÓ ($sentGet)"}"
        )

        // Esperar un poco más para el GET (el ESP32 podría necesitar tiempo para abrir archivo)
        Thread.sleep(500)
        val getResp = ByteArray(512)
        val getRead = conn.bulkTransfer(epIn, getResp, getResp.size, 3000)
        if (getRead > 0) {
            val text = String(getResp, 0, getRead, Charsets.UTF_8)
            Log.i(TAG, "  ✓ Respuesta a GET ($getRead bytes): \"${text.take(200)}\"")
        } else {
            Log.i(TAG, "  ✗ Sin respuesta a GET ($getRead)")
        }

        // ── Paso 5: Diagnóstico final ──
        logSection("5. DIAGNÓSTICO")
        val anyResponse = spontRead > 0 || nlRead > 0 || pingRead > 0 || getRead > 0

        if (!anyResponse) {
            Log.e(TAG, "  ✗ EL ESP32 NO RESPONDE A NADA")
            Log.e(TAG, "")
            Log.e(TAG, "  Posibles causas (de más probable a menos):")
            Log.e(TAG, "")
            Log.e(TAG, "  1. CABLES TX/RX INVERTIDOS")
            Log.e(TAG, "     CP2102 TXD debe ir a ESP32 RX (GPIO3)")
            Log.e(TAG, "     CP2102 RXD debe ir a ESP32 TX (GPIO1)")
            Log.e(TAG, "     → Intercambia los cables TX y RX y prueba de nuevo")
            Log.e(TAG, "")
            Log.e(TAG, "  2. BAUD RATE DEL ESP32 NO ES 115200")
            Log.e(TAG, "     Si el firmware del ESP32 usa otro baud rate,")
            Log.e(TAG, "     los bytes llegan corruptos y el ESP32 no entiende")
            Log.e(TAG, "     → Verifica Serial.begin(115200) o uart(0, 115200)")
            Log.e(TAG, "")
            Log.e(TAG, "  3. ESP32 NO TIENE FIRMWARE / NO ESTÁ CORRIENDO")
            Log.e(TAG, "     ¿Flasheaste el MicroPython/Arduino al ESP32?")
            Log.e(TAG, "     ¿El ESP32 tiene alimentación? ¿El LED está encendido?")
            Log.e(TAG, "")
            Log.e(TAG, "  4. ESP32 USA OTRO UART (no UART0)")
            Log.e(TAG, "     Si tu firmware usa UART1 o UART2, los pines son diferentes")
            Log.e(TAG, "     UART0 = GPIO1 (TX) / GPIO3 (RX) ← el del CP2102")
            Log.e(TAG, "")
            Log.e(TAG, "  5. CABLE GND NO CONECTADO")
            Log.e(TAG, "     Sin GND común, la comunicación serial no funciona")
        } else {
            Log.i(TAG, "  ✓ El ESP32 responde — el canal serial funciona")
            if (getRead <= 0) {
                Log.w(TAG, "  ⚠ Pero no responde al comando GET")
                Log.w(TAG, "    El firmware del ESP32 no reconoce: \"$getCmd\"")
            }
        }

        Log.i(TAG, "╔══════════════════════════════════════════════════╗")
        Log.i(TAG, "║              FIN DEL DIAGNÓSTICO                 ║")
        Log.i(TAG, "╚══════════════════════════════════════════════════╝")

        // Cleanup
        conn.releaseInterface(iface)
        conn.close()
    }

    private fun logSection(title: String) {
        Log.i(TAG, "")
        Log.i(TAG, "── $title ──────────────────────────────")
    }
}
 