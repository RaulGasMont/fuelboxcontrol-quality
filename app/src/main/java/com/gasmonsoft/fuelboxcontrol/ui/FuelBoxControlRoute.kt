package com.gasmonsoft.fuelboxcontrol.ui

import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.gasmonsoft.fuelboxcontrol.ui.home.HomeScreen
import com.gasmonsoft.fuelboxcontrol.ui.main_observation.MainSensorObservation
import com.gasmonsoft.fuelboxcontrol.ui.permissions.BluetoothPermissionScreen
import com.gasmonsoft.fuelboxcontrol.ui.permissions.LocationPermissionScreen
import com.gasmonsoft.fuelboxcontrol.ui.permissions.PermissionResultScreen

sealed class FuelBoxControlRoute(val route: String) {
    object Welcome : FuelBoxControlRoute("welcome")
    object Bluetooth : FuelBoxControlRoute("bluetooth")
    object Location : FuelBoxControlRoute("location")
    object Result : FuelBoxControlRoute("result")
    object Home : FuelBoxControlRoute("home")
    object Sensor : FuelBoxControlRoute("sensor")
}

@Composable
fun FuelBoxControlFlowNav() {
    val navController = rememberNavController()
    val modifier = Modifier.safeContentPadding()

    NavHost(
        navController = navController,
        startDestination = FuelBoxControlRoute.Welcome.route
    ) {
        composable(FuelBoxControlRoute.Welcome.route) {
            WelcomeScreen {
                navController.navigate(FuelBoxControlRoute.Bluetooth.route)
            }
        }

        composable(FuelBoxControlRoute.Bluetooth.route) {
            BluetoothPermissionScreen {
                navController.navigate(FuelBoxControlRoute.Location.route)
            }
        }

        composable(FuelBoxControlRoute.Location.route) {
            LocationPermissionScreen {
                navController.navigate(FuelBoxControlRoute.Result.route)
            }
        }

        composable(FuelBoxControlRoute.Result.route) {
            PermissionResultScreen(
                onFinish = {
                    navController.navigate(FuelBoxControlRoute.Home.route)
                }
            )
        }

        composable(FuelBoxControlRoute.Home.route) {
            HomeScreen(modifier) {
                navController.navigate(FuelBoxControlRoute.Sensor.route)
            }
        }

        composable(FuelBoxControlRoute.Sensor.route) {
            MainSensorObservation(
                onBack = { navController.navigate(FuelBoxControlRoute.Home.route) }
            )
        }
    }
}