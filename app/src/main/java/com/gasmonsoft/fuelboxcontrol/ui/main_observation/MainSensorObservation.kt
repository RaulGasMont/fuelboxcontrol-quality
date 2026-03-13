package com.gasmonsoft.fuelboxcontrol.ui.main_observation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.rememberNavController
import com.gasmonsoft.fuelboxcontrol.ui.navigation.AppNavHost
import com.gasmonsoft.fuelboxcontrol.ui.navigation.AppState
import com.gasmonsoft.fuelboxcontrol.ui.navigation.ScreenRoute
import com.gasmonsoft.fuelboxcontrol.ui.navigation.destinations
import com.gasmonsoft.fuelboxcontrol.ui.navigation.rememberAppState

@Composable
fun MainSensorObservation(
    modifier: Modifier = Modifier,
    onBack: () -> Unit
) {
    val navController = rememberNavController()
    val appState = rememberAppState(navController)

    Scaffold(
        bottomBar = {
            if (appState.shouldShowBottomBar()) {
                AppBottomBar(appState = appState)
            }
        }
    ) { innerPadding ->
        AppNavHost(
            navController = navController,
            modifier = Modifier.padding(innerPadding),
            onGeneralBack = onBack
        )
    }
}

@Composable
fun AppBottomBar(appState: AppState) {
    val currentDestination = appState.currentDestination()

    NavigationBar {
        destinations.forEach { destination ->
            val selected = when (destination.route) {
                is ScreenRoute.Sensores -> currentDestination?.hasRoute<ScreenRoute.Sensores>() == true
                is ScreenRoute.DatosVehiculos -> currentDestination?.hasRoute<ScreenRoute.DatosVehiculos>() == true
            }

            NavigationBarItem(
                selected = selected,
                onClick = { appState.navigateToTopLevel(destination.route) },
                icon = {
                    Icon(
                        painter = painterResource(destination.icon),
                        contentDescription = destination.title
                    )
                },
                label = { Text(destination.title) }
            )
        }
    }
}