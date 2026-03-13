package com.gasmonsoft.fuelboxcontrol.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.gasmonsoft.fuelboxcontrol.ui.sensor.SensorRoute
import com.gasmonsoft.fuelboxcontrol.ui.vehiculo.VehiculosRoute
import kotlinx.serialization.Serializable

@Serializable
data object SensoresRoot

@Serializable
data object VehiculoRoot


@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    onGeneralBack: () -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = SensoresRoot,
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
    navigation<SensoresRoot>(startDestination = SensoresRoot) {
        composable<SensoresRoot> {
            SensorRoute(
                onBack = onGeneralBack,
            )
        }
    }
}

private fun NavGraphBuilder.vehiculosGraph(navController: NavHostController) {
    navigation<VehiculoRoot>(startDestination = VehiculoRoot) {
        composable<VehiculoRoot> {
            VehiculosRoute()
        }
    }
}
