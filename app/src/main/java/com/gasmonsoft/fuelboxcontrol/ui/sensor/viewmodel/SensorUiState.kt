package com.gasmonsoft.fuelboxcontrol.ui.sensor.viewmodel

import com.gasmonsoft.fuelboxcontrol.data.model.ble.ConnectionState

data class SensorCardUiState(
    val volumen: String = "",
    val temperatura: String = "",
    val constante: String = "",
    val fecha: String = "",
    val alerta: String = ""
)

data class SensorUiState(
    val shouldReconnect: Boolean = true,
    val connectionState: ConnectionState = ConnectionState.Uninitialized,
    val initializingMessage: String? = null,
    val errorMessage: String? = null,

    val sensor1: SensorCardUiState = SensorCardUiState(),
    val sensor2: SensorCardUiState = SensorCardUiState(),
    val sensor3: SensorCardUiState = SensorCardUiState(),
    val sensor4: SensorCardUiState = SensorCardUiState(),

    val bateria: String = "",
    val sensorMessage: String = "",

    val sendingState: String = "Esperando hotspot",
    val discoveredServices: String = "",

    val isHotspotConfigured: Boolean = false,
    val isConnectedToHotspot: Boolean = false,
    val isHotspotAvailable: Boolean = false,
    val hotspotConfigurationMessage: String = "",

    val isAutoconsumo: Boolean = false,
    val isSendingCommand: Boolean = false,
    val isDisconnecting: Boolean = false
)

sealed interface SensorUiEvent {
    data class ShowToast(val message: String) : SensorUiEvent
    data class ShowError(val message: String) : SensorUiEvent
}