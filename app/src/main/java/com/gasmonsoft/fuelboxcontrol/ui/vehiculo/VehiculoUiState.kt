package com.gasmonsoft.fuelboxcontrol.ui.vehiculo

import com.gasmonsoft.fuelboxcontrol.model.login.UserData
import com.gasmonsoft.fuelboxcontrol.model.vehicle.VehicleConfiguration
import com.gasmonsoft.fuelboxcontrol.model.vehicle.VehicleInfo

data class VehiculoUiState(
    val isLoggedIn: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val vehicleInfo: VehicleInfo? = null,
    val vehicleConfiguration: VehicleConfiguration? = null,
    val loginEvent: NetworkEvent? = null,
    val vehicleEvent: NetworkEvent? = null,
    val userData: UserData? = null
)

enum class SensorSendingStatus(val message: String) {
    NOT_SENT("No enviado"),
    SENDING("Enviando..."),
    SENT("Enviado"),
    ERROR("Error al enviar")
}

sealed interface NetworkEvent {
    data object Loading : NetworkEvent
    data class Error(val message: String) : NetworkEvent
    data class Success(val message: String) : NetworkEvent
}
