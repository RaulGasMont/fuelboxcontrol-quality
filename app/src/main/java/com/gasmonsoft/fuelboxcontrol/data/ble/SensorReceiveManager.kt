package com.gasmonsoft.fuelboxcontrol.data.ble

import com.gasmonsoft.fuelboxcontrol.utils.Resource
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

data class Device(
    val mac: String,
    val name: String?,
    val rssi: Int
) {
    override fun equals(other: Any?): Boolean {
        return other is Device && other.mac == mac
    }

    override fun hashCode(): Int {
        return mac.hashCode()
    }
}

interface SensorReceiveManager {
    val data: MutableSharedFlow<Resource<SensorResult>>

    val discoveredDevices: MutableStateFlow<Set<Device>>

    val connectionState: MutableSharedFlow<ConnectionState>

    val sensorData: StateFlow<SensorState>
    val sensorEvents: SharedFlow<SensorEvent>

    fun reconnect()

    fun disconnect()

    fun writeInitialValuese(valor: String, opcion: Int)

    fun startReceiving()

    fun writesDataHostPost(ssid: String, password: String, isWifiEnabled: Boolean)

    fun closeConnection()
}