package com.gasmonsoft.fuelboxcontrol.ui.navigation

import com.gasmonsoft.fuelboxcontrol.R
import kotlinx.serialization.Serializable


sealed interface ScreenRoute {
    @Serializable
    data object Sensores : ScreenRoute

    @Serializable
    data object DatosVehiculos : ScreenRoute
}

data class ScreenDestination(
    val route: ScreenRoute,
    val title: String,
    val icon: Int
)

val destinations = listOf(
    ScreenDestination(
        route = ScreenRoute.Sensores,
        title = "Sensores",
        icon = R.drawable.ic_sensor
    ),
    ScreenDestination(
        route = ScreenRoute.DatosVehiculos,
        title = "Datos Vehiculos",
        icon = R.drawable.ic_datos_vehiculos
    )
)

