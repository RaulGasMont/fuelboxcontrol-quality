package com.gasmonsoft.fuelboxcontrol.ui.navigation

import androidx.annotation.DrawableRes
import com.gasmonsoft.fuelboxcontrol.R
import kotlinx.serialization.Serializable


@Serializable
sealed interface ScreenRoute {
    @Serializable
    data object Permissions : ScreenRoute

    @Serializable
    data object Login : ScreenRoute

    @Serializable
    data object Home : ScreenRoute

    @Serializable
    data object Sensores : ScreenRoute

    @Serializable
    data object Deteccion : ScreenRoute

    @Serializable
    data object DatosVehiculos : ScreenRoute

    @Serializable
    data class SelectTank(val idCaja: Int) : ScreenRoute
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
    ScreenDestination(
        route = ScreenRoute.Deteccion,
        title = "Calibración",
        icon = R.drawable.gas_meter_24px
    ),
//    ScreenDestination(
//        route = ScreenRoute.DatosVehiculos,
//        title = "Servidor",
//        icon = R.drawable.ic_datos_vehiculos
//    )
)