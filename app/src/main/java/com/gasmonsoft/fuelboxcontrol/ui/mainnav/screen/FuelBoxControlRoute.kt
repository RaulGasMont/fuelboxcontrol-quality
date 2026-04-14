package com.gasmonsoft.fuelboxcontrol.ui.mainnav.screen

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.gasmonsoft.fuelboxcontrol.ui.detector.screen.DetectorRoute
import com.gasmonsoft.fuelboxcontrol.ui.home.screen.HomeScreen
import com.gasmonsoft.fuelboxcontrol.ui.login.screen.LoginRoute
import com.gasmonsoft.fuelboxcontrol.ui.mainnav.viewmodel.FbcViewModel
import com.gasmonsoft.fuelboxcontrol.ui.navigation.ScreenRoute
import com.gasmonsoft.fuelboxcontrol.ui.navigation.rememberAppState
import com.gasmonsoft.fuelboxcontrol.ui.permissions.PermissionsScreen
import com.gasmonsoft.fuelboxcontrol.ui.selecttank.screen.SelectTankRoute
import com.gasmonsoft.fuelboxcontrol.ui.sensor.ui.SensorRoute
import com.gasmonsoft.fuelboxcontrol.ui.sensorconfig.SensorConfigBottomBar
import com.gasmonsoft.fuelboxcontrol.ui.vehiculo.ui.VehiculosRoute

@Composable
fun FuelBoxControlFlowNav(viewModel: FbcViewModel = hiltViewModel()) {
    val state = viewModel.uiState.collectAsState()
    val appState = rememberAppState()
    val currentContext = LocalContext.current

    if (state.value.isCheckingSession) return

    val hasAllPermissions = hasAllPermissions(currentContext)
    val startDest: ScreenRoute = when {
        state.value.sessionExpired -> ScreenRoute.Login
        !hasAllPermissions -> ScreenRoute.Permissions
        else -> ScreenRoute.Home
    }

    Scaffold(
        bottomBar = {
            if (appState.shouldShowBottomBar()) {
                SensorConfigBottomBar(appState = appState)
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = appState.navController,
            startDestination = startDest,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable<ScreenRoute.Permissions> {
                PermissionsScreen(onAllGranted = {
                    appState.navController.navigate(ScreenRoute.Home) {
                        popUpTo(ScreenRoute.Permissions) { inclusive = true }
                    }
                })
            }

            composable<ScreenRoute.Login> {
                LoginRoute(onHome = {
                    appState.navController.navigate(ScreenRoute.Home) {
                        popUpTo(ScreenRoute.Login) { inclusive = true }
                    }
                })
            }

            composable<ScreenRoute.Home> {
                HomeScreen(onNavToLogin = {
                    appState.navController.navigate(ScreenRoute.Login)
                }) {
                    appState.navController.navigate(ScreenRoute.Sensores)
                }
            }

            composable<ScreenRoute.Sensores> {
                SensorRoute(onBack = {
                    appState.navController.popBackStack()
                })
            }

            composable<ScreenRoute.Deteccion> { backStackEntry ->
                val route = backStackEntry.toRoute<ScreenRoute.Deteccion>()

                val selectedTankId = backStackEntry.savedStateHandle
                    .getLiveData<Int>("selected_tank_id")
                    .observeAsState()

                val selectedTankType = backStackEntry.savedStateHandle
                    .getLiveData<String>("selected_tank_type")
                    .observeAsState()

                val selectedTankName = backStackEntry.savedStateHandle
                    .getLiveData<String>("selected_tank_name")
                    .observeAsState()

                DetectorRoute(
                    onSelectTank = {
                        appState.navController.navigate(ScreenRoute.SelectTank(route.idCaja))
                    },
                    selectedTankId = selectedTankId.value ?: 0,
                    selectedTankType = selectedTankType.value ?: "",
                    selectedTankName = selectedTankName.value ?: ""
                )
            }

            composable<ScreenRoute.DatosVehiculos> {
                VehiculosRoute(onCalibrate = { idCaja ->
                    appState.navController.navigate(ScreenRoute.Deteccion(idCaja))
                })
            }

            composable<ScreenRoute.SelectTank> {
                SelectTankRoute(
                    onBack = {
                        appState.navController.popBackStack()
                    },
                    onTankSelected = { tank ->
                        val previousBackStackEntry = appState.navController.previousBackStackEntry
                        previousBackStackEntry?.savedStateHandle?.set("selected_tank_id", tank.id)
                        previousBackStackEntry?.savedStateHandle?.set(
                            "selected_tank_type",
                            tank.type.value
                        )
                        previousBackStackEntry?.savedStateHandle?.set(
                            "selected_tank_name",
                            tank.name
                        )
                        appState.navController.popBackStack()
                    }
                )
            }
        }
    }
}

private fun hasAllPermissions(context: Context): Boolean {
    val requiredPermission = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        requiredPermission.add(Manifest.permission.BLUETOOTH_CONNECT)
        requiredPermission.add(Manifest.permission.BLUETOOTH_SCAN)
    }
    return requiredPermission.all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }
}