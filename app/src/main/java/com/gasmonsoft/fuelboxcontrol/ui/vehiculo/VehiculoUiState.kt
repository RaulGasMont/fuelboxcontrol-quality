package com.gasmonsoft.fuelboxcontrol.ui.vehiculo

import com.gasmonsoft.fuelboxcontrol.model.login.UserData

data class VehiculoUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val loginEvent: NetworkEvent? = null,
    val userData: UserData? = null
)

sealed interface NetworkEvent {
    data object Loading : NetworkEvent
    data class Error(val message: String) : NetworkEvent
    data class Success(val message: String) : NetworkEvent
}
