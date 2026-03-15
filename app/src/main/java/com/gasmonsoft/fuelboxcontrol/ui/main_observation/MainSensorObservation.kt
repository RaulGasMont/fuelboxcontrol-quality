package com.gasmonsoft.fuelboxcontrol.ui.main_observation

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.rememberNavController
import com.gasmonsoft.fuelboxcontrol.ui.navigation.AppNavHost
import com.gasmonsoft.fuelboxcontrol.ui.navigation.AppState
import com.gasmonsoft.fuelboxcontrol.ui.navigation.ScreenRoute
import com.gasmonsoft.fuelboxcontrol.ui.navigation.destinations
import com.gasmonsoft.fuelboxcontrol.ui.navigation.rememberAppState
import com.gasmonsoft.fuelboxcontrol.ui.theme.FuelBoxControlTheme

@Composable
fun MainSensorObservation(
    modifier: Modifier = Modifier,
    onBack: () -> Unit
) {
    val navController = rememberNavController()
    val appState = rememberAppState(navController)

    MainSensorObservationContent(
        shouldShowBottomBar = appState.shouldShowBottomBar(),
        bottomBar = { AppBottomBar(appState = appState) },
        modifier = modifier
    ) { innerPadding ->
        AppNavHost(
            navController = navController,
            modifier = Modifier.padding(innerPadding),
            onGeneralBack = onBack
        )
    }
}

@Composable
private fun MainSensorObservationContent(
    shouldShowBottomBar: Boolean,
    bottomBar: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        modifier = modifier,
        bottomBar = {
            if (shouldShowBottomBar) {
                bottomBar()
            }
        }
    ) { innerPadding ->
        content(innerPadding)
    }
}

@Composable
fun AppBottomBar(appState: AppState) {
    val currentDestination = appState.currentDestination()
    AppBottomBarContent(
        isRouteSelected = { route ->
            when (route) {
                is ScreenRoute.Sensores -> currentDestination?.hasRoute<ScreenRoute.Sensores>() == true
                is ScreenRoute.DatosVehiculos -> currentDestination?.hasRoute<ScreenRoute.DatosVehiculos>() == true
                else -> false
            }
        },
        onNavigate = { appState.navigateToTopLevel(it) }
    )
}

@Composable
private fun AppBottomBarContent(
    isRouteSelected: (Any) -> Boolean,
    onNavigate: (Any) -> Unit
) {
    NavigationBar(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 14.dp,
                shape = RoundedCornerShape(26.dp),
                clip = false
            )
            .clip(RoundedCornerShape(26.dp))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.10f),
                shape = RoundedCornerShape(26.dp)
            ),
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
        tonalElevation = 6.dp
    ) {
        destinations.forEach { destination ->
            NavigationBarItem(
                selected = isRouteSelected(destination.route),
                onClick = { onNavigate(destination.route) },
                icon = {
                    Icon(
                        painter = painterResource(destination.icon),
                        contentDescription = destination.title,
                        modifier = Modifier.size(22.dp)
                    )
                },
                label = { Text(destination.title) },
                alwaysShowLabel = true,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MainSensorObservationPreview() {
    FuelBoxControlTheme {
        MainSensorObservationContent(
            shouldShowBottomBar = true,
            bottomBar = {
                AppBottomBarContent(
                    isRouteSelected = { it is ScreenRoute.Sensores },
                    onNavigate = {}
                )
            }
        ) { innerPadding ->
            Text(
                text = "Main Content Area",
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AppBottomBarPreview() {
    FuelBoxControlTheme {
        AppBottomBarContent(
            isRouteSelected = { it is ScreenRoute.Sensores },
            onNavigate = {}
        )
    }
}
