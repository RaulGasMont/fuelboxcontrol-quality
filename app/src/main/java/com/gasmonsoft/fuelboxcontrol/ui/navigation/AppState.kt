package com.gasmonsoft.fuelboxcontrol.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController


@Stable
class AppState(
    val navController: NavHostController
) {
    @Composable
    fun currentDestination(): NavDestination? {
        val backStackEntry by navController.currentBackStackEntryAsState()
        return backStackEntry?.destination
    }

    @Composable
    fun shouldShowBottomBar(): Boolean {
        val currentDestination = currentDestination()
        return destinations.any { topLevel ->
            when (topLevel.route) {
                is ScreenRoute.Sensores -> currentDestination?.hasRoute<ScreenRoute.Sensores>() == true
                is ScreenRoute.DatosVehiculos -> currentDestination?.hasRoute<ScreenRoute.DatosVehiculos>() == true
            }
        }
    }

    fun navigateToTopLevel(route: Any) {
        navController.navigate(route) {
            launchSingleTop = true
            restoreState = true
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
        }
    }
}

@Composable
fun rememberAppState(
    navController: NavHostController = rememberNavController()
) = remember(navController) {
    AppState(navController)
}