package com.gasmonsoft.fuelboxcontrol.data.ble

import com.gasmonsoft.fuelboxcontrol.utils.Resource
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow

data class Device(
    val mac: String,
    val name: String,
    val rssi: Int
)

interface SensorReceiveManager {
    val data: MutableSharedFlow<Resource<SensorResult>>
    val discoveredDevices: MutableStateFlow<Set<Device>>
    fun reconnect()

    fun disconnect()
    fun writeInitialValuese(valor: String, opcion: Int)
    fun startReceiving()

    fun writesDataHostPost(ssid: String, password: String, isWifiEnabled: Boolean)

    fun closeConnection()
}