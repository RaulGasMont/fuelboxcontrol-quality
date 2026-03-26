package com.gasmonsoft.fuelboxcontrol.ui.navigation

import androidx.annotation.DrawableRes
import com.gasmonsoft.fuelboxcontrol.R
import kotlinx.serialization.Serializable


sealed interface ScreenRoute {
    @Serializable
    data object Sensores : ScreenRoute

    @Serializable
    data class Calibracion(val idCaja: Int) : ScreenRoute

    @Serializable
    data object DatosVehiculos : ScreenRoute
}

data class ScreenDestination(
    val route: ScreenRoute,
    val title: String,
    @DrawableRes val icon: Int
)

val destinations = listOf(
    ScreenDestination(
        route = ScreenRoute.Sensores,
        title = "Sensores",
        icon = R.drawable.ic_sensor
    ),
//    ScreenDestination(
//        route = ScreenRoute.Calibracion(0),
//        title = "Calibración",
//        icon = R.drawable.dat
//    ),
    ScreenDestination(
        route = ScreenRoute.DatosVehiculos,
        title = "Servidor",
        icon = R.drawable.ic_datos_vehiculos
    )
)