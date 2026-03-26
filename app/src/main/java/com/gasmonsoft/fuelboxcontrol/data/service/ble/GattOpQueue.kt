package com.gasmonsoft.fuelboxcontrol.data.service.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothStatusCodes
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GattOpQueue @Inject constructor() {
    private val mutex = Mutex()

    private var pendingChar: CompletableDeferred<Int>? = null
    private var pendingDesc: CompletableDeferred<Int>? = null
    private var pendingRssi: CompletableDeferred<Int>? = null

    private var pendingCharKey: String? = null
    private var pendingDescKey: String? = null

    private fun charKey(gatt: BluetoothGatt, ch: BluetoothGattCharacteristic): String {
        val svc = ch.service?.uuid?.toString() ?: "nosvc"
        return "${System.identityHashCode(gatt)}|$svc|${ch.uuid}"
    }

    private fun descKey(gatt: BluetoothGatt, desc: BluetoothGattDescriptor): String {
        val ch = desc.characteristic
        val svc = ch.service?.uuid?.toString() ?: "nosvc"
        return "${System.identityHashCode(gatt)}|$svc|${ch.uuid}|${desc.uuid}"
    }

    /** Llamar cuando te desconectas o te cambias de dispositivo */
    fun onDisconnected() {
        pendingChar?.complete(BluetoothGatt.GATT_FAILURE)
        pendingDesc?.complete(BluetoothGatt.GATT_FAILURE)
        pendingRssi?.cancel()

        pendingChar = null
        pendingDesc = null
        pendingRssi = null
        pendingCharKey = null
        pendingDescKey = null
    }

    @SuppressLint("MissingPermission")
    suspend fun writeDescriptorAwait(
        gatt: BluetoothGatt,
        desc: BluetoothGattDescriptor,
        value: ByteArray = desc.value ?: byteArrayOf(),
        timeoutMs: Long = 10_000,
        retries: Int = 5,
        retryDelayMs: Long = 250
    ): Boolean = mutex.withLock {

        if (value.isEmpty()) return@withLock false

        val key = descKey(gatt, desc)
        var attempt = 0

        while (attempt < retries) {
            val deferred = CompletableDeferred<Int>()
            pendingDesc = deferred
            pendingDescKey = key

            val started = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    gatt.writeDescriptor(desc, value) == BluetoothStatusCodes.SUCCESS
                } else {
                    desc.value = value
                    gatt.writeDescriptor(desc)
                }
            } catch (_: Throwable) {
                false
            }

            if (!started) {
                attempt++
                Log.w("GattOpQueue", "writeDescriptor() devolvió false (key=$key) reintento ($attempt/$retries)")
                pendingDesc = null
                pendingDescKey = null
                delay(retryDelayMs)
                continue
            }

            val status = try {
                withTimeout(timeoutMs) { deferred.await() }
            } catch (_: TimeoutCancellationException) {
                attempt++
                Log.w("GattOpQueue", "writeDescriptor() TIMEOUT (key=$key) reintento ($attempt/$retries)")
                pendingDesc = null
                pendingDescKey = null
                delay(retryDelayMs)
                continue
            } finally {
                pendingDesc = null
                pendingDescKey = null
            }

            if (status == BluetoothGatt.GATT_SUCCESS) return@withLock true

            attempt++
            Log.w("GattOpQueue", "DescriptorWrite status=$status (key=$key) reintento ($attempt/$retries)")
            delay(retryDelayMs)
        }

        false
    }

    @SuppressLint("MissingPermission")
    suspend fun writeCharacteristicAwait(
        gatt: BluetoothGatt,
        ch: BluetoothGattCharacteristic,
        value: ByteArray,
        writeType: Int = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT,
        timeoutMs: Long = 10_000,
        retries: Int = 5,
        retryDelayMs: Long = 250
    ): Boolean = mutex.withLock {

        val key = charKey(gatt, ch)
        var attempt = 0

        while (attempt < retries) {
            val deferred = CompletableDeferred<Int>()
            pendingChar = deferred
            pendingCharKey = key

            val started = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    gatt.writeCharacteristic(ch, value, writeType) == BluetoothStatusCodes.SUCCESS
                } else {
                    ch.writeType = writeType
                    ch.value = value
                    gatt.writeCharacteristic(ch)
                }
            } catch (_: Throwable) {
                false
            }

            if (!started) {
                attempt++
                Log.w("GattOpQueue", "writeCharacteristic() devolvió false (key=$key) reintento ($attempt/$retries)")
                pendingChar = null
                pendingCharKey = null
                delay(retryDelayMs)
                continue
            }

            if (writeType == BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE) {
                pendingChar = null
                pendingCharKey = null
                return@withLock true
            }

            val status = try {
                withTimeout(timeoutMs) { deferred.await() }
            } catch (_: TimeoutCancellationException) {
                attempt++
                Log.w("GattOpQueue", "writeCharacteristic() TIMEOUT (key=$key) reintento ($attempt/$retries)")
                pendingChar = null
                pendingCharKey = null
                delay(retryDelayMs)
                continue
            } finally {
                pendingChar = null
                pendingCharKey = null
            }

            if (status == BluetoothGatt.GATT_SUCCESS) return@withLock true

            attempt++
            Log.w("GattOpQueue", "CharacteristicWrite status=$status (key=$key) reintento ($attempt/$retries)")
            delay(retryDelayMs)
        }

        false
    }

    @SuppressLint("MissingPermission")
    suspend fun readRemoteRssiAwait(
        gatt: BluetoothGatt,
        timeoutMs: Long = 2000
    ): Int? = mutex.withLock {
        val deferred = CompletableDeferred<Int>()
        pendingRssi = deferred

        if (!gatt.readRemoteRssi()) {
            pendingRssi = null
            return@withLock null
        }

        return try {
            withTimeout(timeoutMs) { deferred.await() }
        } catch (_: Exception) {
            null
        } finally {
            pendingRssi = null
        }
    }

    fun onDescriptorWrite(gatt: BluetoothGatt, desc: BluetoothGattDescriptor, status: Int) {
        val key = descKey(gatt, desc)
        if (pendingDescKey == key) {
            pendingDesc?.complete(status)
        }
    }

    fun onCharacteristicWrite(gatt: BluetoothGatt, ch: BluetoothGattCharacteristic, status: Int) {
        val key = charKey(gatt, ch)
        if (pendingCharKey == key) {
            pendingChar?.complete(status)
        }
    }

    fun onReadRemoteRssi(rssi: Int, status: Int) {
        if (status == BluetoothGatt.GATT_SUCCESS) {
            pendingRssi?.complete(rssi)
        } else {
            pendingRssi?.cancel()
        }
    }
}