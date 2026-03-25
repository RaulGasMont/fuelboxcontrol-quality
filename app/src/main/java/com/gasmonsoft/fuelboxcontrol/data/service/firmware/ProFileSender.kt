package com.gasmonsoft.fuelboxcontrol.data.service.firmware

import android.bluetooth.BluetoothGattCharacteristic
import android.util.Log
import com.gasmonsoft.fuelboxcontrol.data.model.ble.CONTROL_FIRMWARE_UUID
import com.gasmonsoft.fuelboxcontrol.data.model.ble.DATA_FIRMWARE_UUID
import com.gasmonsoft.fuelboxcontrol.data.model.ble.SERVICE_FIRMWARE_UUID
import com.gasmonsoft.fuelboxcontrol.data.service.ble.BleConnectionManager
import com.gasmonsoft.fuelboxcontrol.data.service.ble.GattOpQueue
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.CRC32
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min

private const val CMD_START_TX: Byte = 0x01
private const val CMD_END_TX: Byte = 0x04
private const val CMD_QUERY_STATUS: Byte = 0x10
private const val CMD_META_END: Byte = 0x13
private const val CMD_META_CHUNK: Byte = 0x12

const val NTF_ACK: Byte = 0x02
const val NTF_NACK: Byte = 0x15
const val NTF_OK: Byte = 0x06
private const val NTF_BAD: Byte = 0x07

private const val TLV_FILENAME: Byte = 0x01
private const val TLV_PATH: Byte = 0x02
private const val TLV_ROLE: Byte = 0x03
private const val TLV_FLAGS: Byte = 0x04


private const val FLAG_MOVE_AFTER = 0x01  // mover a path+filename al terminar OK
private const val FLAG_BOOTMODE_1 = 0x02  // escribir bootMode = 1
private const val FLAG_BOOTMODE_3 = 0x04  // escribir bootMode = 3
private const val FLAG_REBOOT_AFTER = 0x08  // reset al terminar OK
private const val FLAG_REPLACE_EXIST = 0x10  // borrar destino si existe

enum class UpgradeFileType(
    val role: String,
    val route: String,
    val flags: Int,
    val prioridad: Int,
) {
    FIRMWARE("fw", "acFw", FLAG_MOVE_AFTER or FLAG_BOOTMODE_3 or FLAG_REBOOT_AFTER, 3),
    CONFIG("cfg", "config", FLAG_MOVE_AFTER or FLAG_BOOTMODE_1 or FLAG_REBOOT_AFTER, 2),
    DATA("data", "log", FLAG_MOVE_AFTER or FLAG_BOOTMODE_1 or FLAG_REBOOT_AFTER, 1)
}

// =========================================================
// MODELO DE MENSAJE CONTROL
// =========================================================

data class CtrlMsg(
    val type: Byte,
    val nextSeq: Int = 0,
    val receivedBytes: Long = 0L
)

// =========================================================
// PRO FILE SENDER
// =========================================================
@Singleton
class ProFileSender @Inject constructor(
    private val gattQueue: GattOpQueue,
    private val bleConnectionManager: BleConnectionManager,
) {
    private var controlChar: BluetoothGattCharacteristic? = null
    private var dataChar: BluetoothGattCharacteristic? = null
    private var mtu: Int = 23


    private val ctrlChannel = Channel<ByteArray>(Channel.BUFFERED)
    // ---------------------------------------------------------
    // ESTABLECER VARIABLES
    // ---------------------------------------------------------

    fun setMtuAndGatt(mtu: Int) {
        this@ProFileSender.mtu = mtu
        val service = bleConnectionManager.getGatt()?.getService(SERVICE_FIRMWARE_UUID) ?: return
        controlChar = service.getCharacteristic(CONTROL_FIRMWARE_UUID) ?: return
        dataChar = service.getCharacteristic(DATA_FIRMWARE_UUID) ?: return
    }

    fun updateMtu(newMtu: Int) {
        this.mtu = newMtu
        Log.d("ProFileSender", "MTU actualizado a $newMtu")
    }

    // ---------------------------------------------------------
    // CONTROL NOTIFY
    // ---------------------------------------------------------

    fun onControlNotify(value: ByteArray) {
        ctrlChannel.trySend(value) // INDICA QUE LA TRAMA FUE RECIBIDA
    }

    // ---------------------------------------------------------
    // CRC
    // ---------------------------------------------------------

    private fun crc32(data: ByteArray): Long =
        CRC32().apply { update(data) }.value

    // ---------------------------------------------------------
    // AWAIT CONTROL MESSAGE
    // ---------------------------------------------------------

    private suspend fun awaitCtrlMsg(
        timeoutMs: Long = 10_000
    ): CtrlMsg = withTimeout(timeoutMs) {

        val raw = ctrlChannel.receive()

        if (raw.isEmpty()) {
            return@withTimeout CtrlMsg(type = 0)
        }

        val t = raw[0]

        when (t) {

            NTF_ACK, NTF_NACK -> {
                if (raw.size >= 7) {
                    val bb = ByteBuffer
                        .wrap(raw)
                        .order(ByteOrder.BIG_ENDIAN)

                    bb.get() // consume type

                    val seq = bb.short.toInt() and 0xFFFF
                    val rec = bb.int.toLong() and 0xFFFFFFFFL

                    CtrlMsg(t, seq, rec)
                } else {
                    CtrlMsg(t)
                }
            }

            else -> CtrlMsg(t)
        }
    }

    // ---------------------------------------------------------
    // ENVÍO PRO
    // ---------------------------------------------------------

    suspend fun sendFilePro(
        fileName: String,
        fileBytes: ByteArray,
        upgradeType: UpgradeFileType,
        sensorId: String,
        maxRetries: Int = 15
    ): CtrlMsg? {
        val bleGatt = bleConnectionManager.getGatt()
        if (bleGatt == null || controlChar == null || dataChar == null) return null

        val totalSize = fileBytes.size
        val crc = crc32(fileBytes)

        // Usamos el MTU negociado para calcular un chunkSize seguro
        // BLE Overhead es 3 bytes (Opcode + Handle).
        // seq es 2 bytes. 
        // Si MTU=23, chunkSize máximo es 23 - 3 - 2 = 18.
        val safeChunkSize = (mtu - 5).coerceAtMost(240).coerceAtLeast(18)
        val chunkSize = 18 // Mantenemos 18 por compatibilidad si el firmware es estricto, o usamos safeChunkSize

        // Limpiar canal
        while (!ctrlChannel.isEmpty) {
            ctrlChannel.tryReceive().getOrNull()
        }

        // =====================================================
        // START_TX
        // =====================================================

        val sensorRoute = if (sensorId != "") ".$sensorId" else ".0"
        val meta = tlv(TLV_FILENAME, fileName) +
                tlv(TLV_ROLE, upgradeType.role) +
                tlv(TLV_PATH, upgradeType.route + sensorRoute) +
                tlv32(TLV_FLAGS, upgradeType.flags.toLong())

        val start = ByteBuffer
            .allocate(11)
            .order(ByteOrder.BIG_ENDIAN)
            .put(CMD_START_TX)
            .putInt(totalSize)
            .putInt(crc.toInt())
            .putShort(chunkSize.toShort())
            .array()

        val startOk = gattQueue.writeCharacteristicAwait(
            bleGatt,
            controlChar!!,
            start,
            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        )
        if (!startOk) return null

        val metaChunks = meta.asList().chunked(chunkSize).map { it.toByteArray() }
        var mseq = 0
        for (ch in metaChunks) {
            val pkt = byteArrayOf(CMD_META_CHUNK) + byteArrayOf(mseq.toByte()) + ch
            gattQueue.writeCharacteristicAwait(
                bleGatt,
                controlChar!!,
                pkt,
                BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            )
            mseq++
        }

        gattQueue.writeCharacteristicAwait(
            bleGatt,
            controlChar!!,
            byteArrayOf(CMD_META_END),
            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        )


        var msg = awaitCtrlMsg()

        if (msg.type != NTF_ACK && msg.type != NTF_NACK) {
            throw IllegalStateException(
                "Respuesta inesperada START: ${msg.type}"
            )
        }

        var seq = msg.nextSeq
        var sentOffset = seq * chunkSize

        if (sentOffset > totalSize) {
            seq = 0
            sentOffset = 0
        }

        var retries = 0

        // =====================================================
        // DATA LOOP
        // =====================================================

        while (sentOffset < totalSize) {

            val len = min(chunkSize, totalSize - sentOffset)

            val pkt = ByteBuffer
                .allocate(2 + len)
                .order(ByteOrder.BIG_ENDIAN)
                .putShort(seq.toShort())
                .put(fileBytes, sentOffset, len)
                .array()

            gattQueue.writeCharacteristicAwait(
                bleGatt,
                dataChar!!,
                pkt,
                BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
            )

            // Micro throttle
            delay(5)

            val resp = try {
                awaitCtrlMsg(8_000)
            } catch (e: TimeoutCancellationException) {
                retries++
                if (retries > maxRetries) {
                    throw IllegalStateException("Timeout ACK seq=$seq")
                }
                continue
            }

            when (resp.type) {

                NTF_ACK -> {
                    val expected = resp.nextSeq

                    if (expected == ((seq + 1) and 0xFFFF)) {
                        seq++
                        sentOffset += len
                        retries = 0
                    } else {
                        seq = expected
                        sentOffset = seq * chunkSize
                        retries++
                    }
                }

                NTF_NACK -> {
                    seq = resp.nextSeq
                    sentOffset = seq * chunkSize
                    retries++
                }

                else -> {
                    retries++
                }
            }

            if (retries > maxRetries) {
                throw IllegalStateException("Demasiados reintentos")
            }
        }

        // =====================================================
        // END_TX
        // =====================================================

        Log.d("BLE", "ENVIANDO COMANDO DE FINALIZACION")
        gattQueue.writeCharacteristicAwait(
            bleGatt,
            controlChar!!,
            byteArrayOf(CMD_END_TX),
            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        )

        return awaitCtrlMsg(35_000)
    }

    private fun tlv(t: Byte, s: String): ByteArray {
        val b = s.toByteArray(Charsets.UTF_8)
        require(b.size <= 255)
        return byteArrayOf(t, b.size.toByte()) + b
    }

    private fun tlv32(t: Byte, value: Long): ByteArray {
        val bb = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN)
        bb.putInt(value.toInt())
        val v = bb.array()
        return byteArrayOf(t, 4) + v
    }
}
