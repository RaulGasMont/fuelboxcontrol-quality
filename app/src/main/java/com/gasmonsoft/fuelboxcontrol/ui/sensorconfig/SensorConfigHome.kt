package com.gasmonsoft.fuelboxcontrol.ui.sensorconfig

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.gasmonsoft.fuelboxcontrol.ui.navigation.AppNavHost
import com.gasmonsoft.fuelboxcontrol.ui.navigation.ScreenRoute
import com.gasmonsoft.fuelboxcontrol.ui.navigation.rememberAppState
import com.gasmonsoft.fuelboxcontrol.ui.theme.FuelBoxControlTheme

@Composable
fun SensorConfigHome(
    modifier: Modifier = Modifier,
    onBack: () -> Unit
) {
    val appState = rememberAppState()

    SensorConfigContent(
        shouldShowBottomBar = appState.shouldShowBottomBar(),
        bottomBar = { SensorConfigBottomBar(appState = appState) },
        modifier = modifier
    ) { innerPadding ->
        AppNavHost(
            navController = appState.navController,
            modifier = Modifier.padding(innerPadding),
            onGeneralBack = onBack
        )
    }
}

@Composable
private fun SensorConfigContent(
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

@Preview(showBackground = true)
@Composable
private fun SensorConfigPreview() {
    FuelBoxControlTheme {
        SensorConfigContent(
            shouldShowBottomBar = true,
            bottomBar = {
                SensorConfigBottomBarContent(
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
private fun SensorConfigBottomBarPreview() {
    FuelBoxControlTheme {
        SensorConfigBottomBarContent(
            isRouteSelected = { it is ScreenRoute.Sensores },
            onNavigate = {}
        )
    }
}
