package com.gasmonsoft.fuelboxcontrol.ui.sensorconfig

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import com.gasmonsoft.fuelboxcontrol.ui.navigation.AppState
import com.gasmonsoft.fuelboxcontrol.ui.navigation.ScreenRoute
import com.gasmonsoft.fuelboxcontrol.ui.navigation.ScreenRoute.Calibracion
import com.gasmonsoft.fuelboxcontrol.ui.navigation.ScreenRoute.DatosVehiculos
import com.gasmonsoft.fuelboxcontrol.ui.navigation.ScreenRoute.Sensores
import com.gasmonsoft.fuelboxcontrol.ui.navigation.destinations

@Composable
fun SensorConfigBottomBar(appState: AppState) {
    val currentDestination = appState.currentDestination()

    SensorConfigBottomBarContent(
        isRouteSelected = { route ->
            when (route) {
                Sensores -> {
                    currentDestination?.hierarchy?.any {
                        it.hasRoute<Sensores>()
                    } == true
                }

                Calibracion -> {
                    currentDestination?.hierarchy?.any {
                        it.hasRoute<Calibracion>()
                    } == true
                }

                DatosVehiculos -> {
                    currentDestination?.hierarchy?.any {
                        it.hasRoute<DatosVehiculos>()
                    } == true
                }
            }

        },
        onNavigate = appState::navigateToTopLevel
    )
}

@Composable
fun SensorConfigBottomBarContent(
    isRouteSelected: (ScreenRoute) -> Boolean,
    onNavigate: (ScreenRoute) -> Unit
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
                alwaysShowLabel = true
            )
        }
    }
}