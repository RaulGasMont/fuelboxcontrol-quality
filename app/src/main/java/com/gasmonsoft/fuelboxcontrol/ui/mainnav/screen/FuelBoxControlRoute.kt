package com.gasmonsoft.fuelboxcontrol.ui.mainnav.screen

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.gasmonsoft.fuelboxcontrol.ui.home.screen.HomeScreen
import com.gasmonsoft.fuelboxcontrol.ui.login.screen.LoginRoute
import com.gasmonsoft.fuelboxcontrol.ui.mainnav.viewmodel.FbcViewModel
import com.gasmonsoft.fuelboxcontrol.ui.permissions.PermissionsScreen
import com.gasmonsoft.fuelboxcontrol.ui.sensorconfig.SensorConfigHome

sealed class FuelBoxControlRoute(val route: String) {
    object Permissions : FuelBoxControlRoute("permissions")
    object Home : FuelBoxControlRoute("home")
    object Sensor : FuelBoxControlRoute("sensor")
    object Login : FuelBoxControlRoute("login")
}

@Composable
fun FuelBoxControlFlowNav(viewModel: FbcViewModel = hiltViewModel()) {
    val state = viewModel.uiState.collectAsState()
    val navController = rememberNavController()
    val modifier = Modifier.safeContentPadding()
    val currentContext = LocalContext.current

    // Recuperar última MAC para ver si debemos saltar a Sensores tras Process Death
    val sharedPrefs = currentContext.getSharedPreferences("ble_prefs", Context.MODE_PRIVATE)
    val lastMac = sharedPrefs.getString("last_mac", "")

    val hasAllPermissions = hasAllPermissions(currentContext)

    val startDest = if (state.value.sessionExpired) {
        FuelBoxControlRoute.Login.route
    } else {
        if (hasAllPermissions) {
            if (!lastMac.isNullOrBlank()) FuelBoxControlRoute.Sensor.route else FuelBoxControlRoute.Home.route
        } else {
            FuelBoxControlRoute.Permissions.route
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDest
    ) {
        composable(FuelBoxControlRoute.Permissions.route) {
            PermissionsScreen(
                onAllGranted = {
                    val targetRoute = if (!lastMac.isNullOrBlank()) {
                        FuelBoxControlRoute.Sensor.route
                    } else {
                        FuelBoxControlRoute.Home.route
                    }
                    navController.navigate(targetRoute) {
                        popUpTo(FuelBoxControlRoute.Permissions.route) { inclusive = true }
                    }
                }
            )
        }

        composable(FuelBoxControlRoute.Home.route) {
            HomeScreen(modifier, onNavToLogin = {
                navController.navigate(FuelBoxControlRoute.Login.route)
            }) {
                navController.navigate(FuelBoxControlRoute.Sensor.route)
            }
        }

        composable(FuelBoxControlRoute.Sensor.route) {
            SensorConfigHome(
                onBack = {
                    val popped = navController.popBackStack(
                        FuelBoxControlRoute.Home.route,
                        inclusive = false
                    )
                    if (!popped) {
                        navController.navigate(FuelBoxControlRoute.Home.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }
            )
        }

        composable(FuelBoxControlRoute.Login.route) {
            LoginRoute(onHome = {
                navController.navigate(FuelBoxControlRoute.Home.route) {
                    popUpTo(FuelBoxControlRoute.Login.route) { inclusive = true }
                }
            })
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