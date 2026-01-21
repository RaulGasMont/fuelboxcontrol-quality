package com.gasmonsoft.fuelboxcontrol.data.ble

import com.gasmonsoft.fuelboxcontrol.utils.Resource
import kotlinx.coroutines.flow.MutableSharedFlow


interface SensorReceiveManager {
    val data: MutableSharedFlow<Resource<SensorResult>>

    fun reconnect()

    fun disconnect()
    fun writeInitialValuese(valor: String, opcion: Int)
    fun startReceiving()

    fun writesDataHostPost(ssid: String, password: String, isWifiEnabled: Boolean)

    fun closeConnection()
}