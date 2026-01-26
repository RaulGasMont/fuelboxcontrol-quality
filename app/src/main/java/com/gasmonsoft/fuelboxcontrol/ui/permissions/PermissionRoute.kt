package com.gasmonsoft.fuelboxcontrol.ui.permissions

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

sealed class PermissionRoute(val route: String) {
    object Welcome : PermissionRoute("welcome")
    object Bluetooth : PermissionRoute("bluetooth")
    object Location : PermissionRoute("location")
    object Result : PermissionRoute("result")
}

@Composable
fun PermissionFlowNav() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = PermissionRoute.Welcome.route
    ) {
        composable(PermissionRoute.Welcome.route) {
            WelcomeScreen {
                navController.navigate(PermissionRoute.Bluetooth.route)
            }

            composable(PermissionRoute.Bluetooth.route) {
                BluetoothPermissionScreen {
                    navController.navigate(PermissionRoute.Location.route)
                }
            }

            composable(PermissionRoute.Location.route) {
                LocationPermissionScreen {
                    navController.navigate(PermissionRoute.Result.route)
                }
            }

            composable(PermissionRoute.Result.route) {
                PermissionResultScreen(
                    onFinish = {
                        // Ir a Home / MainActivity
                    }
                )
            }
        }
    }
}