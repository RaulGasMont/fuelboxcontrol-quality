package com.gasmonsoft.fuelboxcontrol.ui.home

import androidx.lifecycle.ViewModel
import com.gasmonsoft.fuelboxcontrol.data.ble.SensorReceiveManager
import com.gasmonsoft.fuelboxcontrol.utils.NetworkConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val sensorReceiveManager: SensorReceiveManager
) : ViewModel() {

    val connectionStatus = sensorReceiveManager.connectionState
    val availableDevices = sensorReceiveManager.discoveredDevices

    fun scanDevices() {
        sensorReceiveManager.startReceiving()
    }

    fun selectDevice(mac: String) {
        NetworkConfig.nombreconfiguracion = mac
        NetworkConfig.configuracion = "mac"
    }
}