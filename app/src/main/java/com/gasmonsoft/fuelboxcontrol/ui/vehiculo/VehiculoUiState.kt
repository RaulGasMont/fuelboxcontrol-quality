package com.gasmonsoft.fuelboxcontrol.ui.vehiculo

data class VehiculoUiState(
    val isLoading: Boolean = false,
    val userData: UserData? = null,
    val error: String? = null
)

data class UserData(
    val username: String,
    val password: String,
    val token: String
)