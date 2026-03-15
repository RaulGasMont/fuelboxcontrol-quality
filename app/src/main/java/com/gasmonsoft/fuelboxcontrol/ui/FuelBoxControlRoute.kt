package com.gasmonsoft.fuelboxcontrol.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
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
    val currentContext = LocalContext.current
    
    // Recuperar última MAC para ver si debemos saltar a Sensores tras Process Death
    val sharedPrefs = currentContext.getSharedPreferences("ble_prefs", Context.MODE_PRIVATE)
    val lastMac = sharedPrefs.getString("last_mac", "")
    
    NavHost(
        navController = navController,
        startDestination = FuelBoxControlRoute.Welcome.route
    ) {
        composable(FuelBoxControlRoute.Welcome.route) {
            val hasAllPermissions = hasAllPermissions(currentContext)
            
            LaunchedEffect(hasAllPermissions) {
                if (hasAllPermissions) {
                    val targetRoute = if (!lastMac.isNullOrBlank()) {
                        FuelBoxControlRoute.Sensor.route
                    } else {
                        FuelBoxControlRoute.Home.route
                    }
                    
                    navController.navigate(targetRoute) {
                        popUpTo(FuelBoxControlRoute.Welcome.route) { inclusive = true }
                    }
                }
            }

            if (!hasAllPermissions) {
                WelcomeScreen {
                    navController.navigate(FuelBoxControlRoute.Bluetooth.route)
                }
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
                    val targetRoute = if (!lastMac.isNullOrBlank()) {
                        FuelBoxControlRoute.Sensor.route
                    } else {
                        FuelBoxControlRoute.Home.route
                    }
                    navController.navigate(targetRoute) {
                        popUpTo(FuelBoxControlRoute.Result.route) { inclusive = true }
                    }
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
                onBack = { 
                    navController.popBackStack(FuelBoxControlRoute.Home.route, inclusive = false)
                }
            )
        }
    }
}


private fun hasAllPermissions(context: Context): Boolean {
    val requiredPermission = mutableListOf(
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        requiredPermission.add(Manifest.permission.BLUETOOTH_CONNECT)
        requiredPermission.add(Manifest.permission.BLUETOOTH_SCAN)
    }

    return requiredPermission.all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }
}