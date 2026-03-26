package com.gasmonsoft.fuelboxcontrol.data.service.ble

import android.bluetooth.BluetoothGatt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

enum class BlePhase {
    DISCONNECTED,
    SCANNING,
    CONNECTING,
    NEGOTIATING_MTU,
    DISCOVERING_SERVICES,
    SUBSCRIBING,
    READY,
    DISCONNECTING,
    ERROR
}

data class BleSession(
    val sessionId: Long = 0L,
    val phase: BlePhase = BlePhase.DISCONNECTED,
    val mac: String? = null,
    val mtu: Int = 23,
    val userRequestedDisconnect: Boolean = false,
    internal val gatt: BluetoothGatt? = null
)

@Singleton
class BleConnectionManager @Inject constructor() {

    private val lock = Any()

    private val _session = MutableStateFlow(BleSession())
    val session = _session.asStateFlow()

    fun currentGatt(): BluetoothGatt? = _session.value.gatt
    fun currentMac(): String? = _session.value.mac
    fun currentPhase(): BlePhase = _session.value.phase
    fun isReady(): Boolean = _session.value.phase == BlePhase.READY

    fun startNewScan(targetMac: String?) = synchronized(lock) {
        val previous = _session.value
        _session.value = BleSession(
            sessionId = previous.sessionId + 1,
            phase = BlePhase.SCANNING,
            mac = targetMac ?: previous.mac,
            mtu = 23,
            userRequestedDisconnect = false,
            gatt = null
        )
    }

    fun attachConnectingGatt(mac: String, gatt: BluetoothGatt) = synchronized(lock) {
        _session.value = _session.value.copy(
            mac = mac,
            gatt = gatt,
            phase = BlePhase.CONNECTING,
            userRequestedDisconnect = false
        )
    }

    fun markNegotiatingMtu() = synchronized(lock) {
        _session.value = _session.value.copy(
            phase = BlePhase.NEGOTIATING_MTU
        )
    }

    fun markDiscoveringServices(mtu: Int) = synchronized(lock) {
        _session.value = _session.value.copy(
            phase = BlePhase.DISCOVERING_SERVICES,
            mtu = mtu
        )
    }

    fun markSubscribing() = synchronized(lock) {
        _session.value = _session.value.copy(
            phase = BlePhase.SUBSCRIBING
        )
    }

    fun markReady() = synchronized(lock) {
        _session.value = _session.value.copy(
            phase = BlePhase.READY,
            userRequestedDisconnect = false
        )
    }

    fun markDisconnectingByUser() = synchronized(lock) {
        _session.value = _session.value.copy(
            phase = BlePhase.DISCONNECTING,
            userRequestedDisconnect = true
        )
    }

    fun markError() = synchronized(lock) {
        _session.value = _session.value.copy(
            phase = BlePhase.ERROR
        )
    }

    fun shouldReconnectAfterDisconnect(): Boolean {
        val current = _session.value
        return !current.userRequestedDisconnect && !current.mac.isNullOrBlank()
    }

    fun clearConnection(clearMac: Boolean) = synchronized(lock) {
        val current = _session.value
        _session.value = current.copy(
            phase = BlePhase.DISCONNECTED,
            mac = if (clearMac) null else current.mac,
            mtu = 23,
            userRequestedDisconnect = false,
            gatt = null
        )
    }
}