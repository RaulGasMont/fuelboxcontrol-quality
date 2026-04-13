package com.gasmonsoft.fuelboxcontrol.ui.selecttank.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.gasmonsoft.fuelboxcontrol.data.model.selectvehicle.Other
import com.gasmonsoft.fuelboxcontrol.data.model.selectvehicle.Tank
import com.gasmonsoft.fuelboxcontrol.data.model.selectvehicle.Vehicle
import com.gasmonsoft.fuelboxcontrol.ui.selecttank.viewmodel.SelectTankUiState
import com.gasmonsoft.fuelboxcontrol.ui.selecttank.viewmodel.SelectTankViewModel
import com.gasmonsoft.fuelboxcontrol.ui.selecttank.viewmodel.SelectVehicleScreen

@Composable
fun SelectTankRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SelectTankViewModel = hiltViewModel(),
) {

    val uiState = viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.getTankList()
    }

    SelectTankScreen(
        uiState = uiState.value,
        onBack = onBack,
        onChangeScreen = { viewModel.changeScreen(it) },
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectTankScreen(
    uiState: SelectTankUiState,
    onBack: () -> Unit,
    onChangeScreen: (screen: SelectVehicleScreen) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDialog by rememberSaveable { mutableStateOf(false) }

    if (showDialog) {
        SelectTankDialog()
    }

    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 16.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Regresar")
            }
            Text(
                text = "Seleccionar Tanque",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        }
        PrimaryTabRow(
            selectedTabIndex = uiState.screen.index,
        ) {
            Tab(
                selected = uiState.screen.index == SelectVehicleScreen.Vehicles().index,
                onClick = { onChangeScreen(SelectVehicleScreen.Vehicles()) },
                text = {
                    Text(
                        text = SelectVehicleScreen.Vehicles().name,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            )

            Tab(
                selected = uiState.screen.index == SelectVehicleScreen.Otros().index,
                onClick = { onChangeScreen(SelectVehicleScreen.Otros()) },
                text = {
                    Text(
                        text = SelectVehicleScreen.Otros().name,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            )
        }

        when (uiState.screen) {
            is SelectVehicleScreen.Otros -> {
                SelectTankOtherScreen(otros = uiState.otros)
            }

            is SelectVehicleScreen.Vehicles -> {
                SelectTankVehicleScreen(vehicles = uiState.vehicles)
            }
        }
    }
}

@Composable
fun SelectTankVehicleScreen(vehicles: List<Tank>) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(vehicles.size, key = { index -> vehicles[index].id }) { index ->
            TankCard(tank = vehicles[index], onClick = {})
        }
    }
}

@Composable
fun SelectTankOtherScreen(otros: List<Tank>, modifier: Modifier = Modifier) {
    Box(modifier = Modifier.padding(16.dp)) {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(otros.size, key = { index -> otros[index].id }) { index ->
                TankCard(tank = otros[index], onClick = {})
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun SelectTankScreenPreview() {
    val onlyVehicles: List<Vehicle> = listOf(
        Vehicle(id = 1, name = "Tanque Tracto 01"),
        Vehicle(id = 2, name = "Tanque Camión 02"),
        Vehicle(id = 3, name = "Tanque Unidad 03"),
        Vehicle(id = 4, name = "Tanque Reparto 04")
    )

    val onlyOthers: List<Other> = listOf(
        Other(id = 5, name = "Tanque Estacionario A"),
        Other(id = 6, name = "Tanque Auxiliar B"),
        Other(id = 7, name = "Tanque Reserva C"),
        Other(id = 8, name = "Tanque Industrial D")
    )
    SelectTankScreen(
        uiState = SelectTankUiState(
            screen = SelectVehicleScreen.Otros(),
            vehicles = onlyVehicles,
            otros = onlyOthers,
        ), onBack = {},
        onChangeScreen = {}
    )
}