package com.gasmonsoft.fuelboxcontrol.ui.selecttank.screen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.LocalGasStation
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.gasmonsoft.fuelboxcontrol.data.model.selectvehicle.Other
import com.gasmonsoft.fuelboxcontrol.data.model.selectvehicle.Tank
import com.gasmonsoft.fuelboxcontrol.data.model.selectvehicle.Vehicle
import com.gasmonsoft.fuelboxcontrol.ui.commons.ErrorDialog
import com.gasmonsoft.fuelboxcontrol.ui.commons.LoadingDialog
import com.gasmonsoft.fuelboxcontrol.ui.selecttank.viewmodel.SelectTankUiState
import com.gasmonsoft.fuelboxcontrol.ui.selecttank.viewmodel.SelectTankViewModel
import com.gasmonsoft.fuelboxcontrol.ui.selecttank.viewmodel.SelectVehicleScreen
import com.gasmonsoft.fuelboxcontrol.ui.theme.FuelBoxControlTheme
import com.gasmonsoft.fuelboxcontrol.utils.ProcessingEvent

@Composable
fun SelectTankRoute(
    onBack: () -> Unit,
    onTankSelected: (Tank) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SelectTankViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.getTankList()
    }

    when (uiState.seeTankEvent) {
        is ProcessingEvent.Loading -> LoadingDialog()
        is ProcessingEvent.Error -> {
            ErrorDialog(
                message = "No se pudo obtener la lista de tanques",
                onDismiss = viewModel::dismissedError
            )
        }

        else -> Unit
    }

    SelectTankScreen(
        uiState = uiState,
        onBack = onBack,
        onTankSelected = onTankSelected,
        onChangeScreen = viewModel::changeScreen,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectTankScreen(
    uiState: SelectTankUiState,
    onBack: () -> Unit,
    onTankSelected: (Tank) -> Unit,
    onChangeScreen: (screen: SelectVehicleScreen) -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
            MaterialTheme.colorScheme.surface,
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
    ) {
        Column(modifier = modifier.fillMaxSize()) {
            // ── Toolbar ──
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 12.dp)
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar")
                }
                Text(
                    text = "Seleccionar Tanque",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            // ── Tabs ──
            val tabs = listOf(SelectVehicleScreen.Vehicles(), SelectVehicleScreen.Otros())

            PrimaryTabRow(selectedTabIndex = uiState.screen.index) {
                tabs.forEach { screen ->
                    Tab(
                        selected = uiState.screen.index == screen.index,
                        onClick = { onChangeScreen(screen) },
                        text = {
                            Text(
                                text = screen.name,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    )
                }
            }

            // ── Contenido con transición animada ──
            AnimatedContent(
                targetState = uiState.screen,
                transitionSpec = {
                    val direction = if (targetState.index > initialState.index)
                        AnimatedContentTransitionScope.SlideDirection.Start
                    else
                        AnimatedContentTransitionScope.SlideDirection.End

                    slideIntoContainer(direction, tween(300)) togetherWith
                            slideOutOfContainer(direction, tween(300))
                },
                label = "tab_transition"
            ) { screen ->
                val tanks = when (screen) {
                    is SelectVehicleScreen.Vehicles -> uiState.vehicles
                    is SelectVehicleScreen.Otros -> uiState.otros
                }
                val emptyMessage = when (screen) {
                    is SelectVehicleScreen.Vehicles -> "No hay vehículos disponibles"
                    is SelectVehicleScreen.Otros -> "No hay tanques disponibles"
                }

                TankList(
                    tanks = tanks,
                    emptyMessage = emptyMessage,
                    onTankSelected = onTankSelected
                )
            }
        }
    }
}

@Composable
private fun TankList(
    tanks: List<Tank>,
    emptyMessage: String,
    onTankSelected: (Tank) -> Unit,
    modifier: Modifier = Modifier
) {
    if (tanks.isEmpty()) {
        EmptyState(message = emptyMessage)
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(tanks, key = { it.id }) { tank ->
            TankCard(
                tank = tank,
                onClick = { onTankSelected(tank) },
                modifier = Modifier.animateItem()
            )
        }

        item { Spacer(modifier = Modifier.height(8.dp)) }
    }
}

@Composable
private fun EmptyState(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Outlined.LocalGasStation,   // o el ícono que prefieras
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SelectTankScreenPreview() {
    FuelBoxControlTheme {
        SelectTankScreen(
            uiState = SelectTankUiState(
                vehicles = listOf(
                    Vehicle(id = 1, name = "Camión de Carga 01"),
                    Vehicle(id = 2, name = "Camión de Carga 02")
                ),
                otros = listOf(
                    Other(id = 3, name = "Tanque Estacionario 01"),
                    Other(id = 4, name = "Tanque Estacionario 02")
                )
            ),
            onBack = {},
            onTankSelected = {},
            onChangeScreen = {}
        )
    }
}