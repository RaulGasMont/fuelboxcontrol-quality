package com.gasmonsoft.fuelboxcontrol.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.gasmonsoft.fuelboxcontrol.ui.sensor.SensorRoute
import com.gasmonsoft.fuelboxcontrol.ui.vehiculo.ui.VehiculosRoute
import kotlinx.serialization.Serializable

@Serializable
data object SensoresNavGraph

@Serializable
data object VehiculosNavGraph

@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    onGeneralBack: () -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = SensoresNavGraph,
        modifier = modifier
    ) {
        sensoresGraph(navController, onGeneralBack)
        vehiculosGraph(navController)
    }
}

private fun NavGraphBuilder.sensoresGraph(
    navController: NavHostController,
    onGeneralBack: () -> Unit
) {
    navigation<SensoresNavGraph>(startDestination = ScreenRoute.Sensores) {
        composable<ScreenRoute.Sensores> {
            SensorRoute(
                onBack = onGeneralBack,
            )
        }
    }
}

private fun NavGraphBuilder.vehiculosGraph(navController: NavHostController) {
    navigation<VehiculosNavGraph>(startDestination = ScreenRoute.DatosVehiculos) {
        composable<ScreenRoute.DatosVehiculos> { backStackEntry ->
            val parentEntry = remember(backStackEntry) {
                navController.getBackStackEntry<VehiculosNavGraph>()
            }
            VehiculosRoute(
                viewModel = hiltViewModel(parentEntry)
            )
        }
    }
}
