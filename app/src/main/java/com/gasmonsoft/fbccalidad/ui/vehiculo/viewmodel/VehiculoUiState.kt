package com.gasmonsoft.fbccalidad.ui.vehiculo.viewmodel

import com.gasmonsoft.fbccalidad.data.model.login.UserData
import com.gasmonsoft.fbccalidad.data.model.vehicle.VehicleConfiguration
import com.gasmonsoft.fbccalidad.data.model.vehicle.VehicleInfo

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

sealed class SensorSendingEvent(
    open val title: String,
    open val message: String? = null
) {
    data object Idle : SensorSendingEvent(
        title = "No enviado"
    )

    data object Loading : SensorSendingEvent(
        title = "Enviando..."
    )

    data class Error(
        override val message: String,
        override val title: String = "Error del servidor"
    ) : SensorSendingEvent(
        title = title,
        message = message
    )

    data class Success(
        override val message: String,
        override val title: String = "Enviado"
    ) : SensorSendingEvent(
        title = title,
        message = message
    )
}

sealed interface NetworkEvent {
    data object Loading : NetworkEvent
    data class Error(val message: String) : NetworkEvent
    data class Success(val message: String) : NetworkEvent
}
