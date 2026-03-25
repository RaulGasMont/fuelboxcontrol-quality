package com.gasmonsoft.fuelboxcontrol.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.gasmonsoft.fuelboxcontrol.ui.calibracion.ui.CalibracionRoute
import com.gasmonsoft.fuelboxcontrol.ui.sensor.ui.SensorRoute
import com.gasmonsoft.fuelboxcontrol.ui.vehiculo.ui.VehiculosRoute

@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    onGeneralBack: () -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = ScreenRoute.Sensores,
        modifier = modifier
    ) {
        composable<ScreenRoute.Sensores> {
            SensorRoute(
                onBack = onGeneralBack
            )
        }

        composable<ScreenRoute.Calibracion> { backStackEntry ->
            val route = backStackEntry.toRoute<ScreenRoute.Calibracion>()
            CalibracionRoute(idCaja = route.idCaja)
        }

        composable<ScreenRoute.DatosVehiculos> {
            VehiculosRoute(
                onCalibrate = { idCaja ->
                    navController.navigate(ScreenRoute.Calibracion(idCaja))
                }
            )
        }
    }
}