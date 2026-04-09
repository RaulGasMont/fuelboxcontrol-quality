package com.gasmonsoft.fuelboxcontrol.ui.selecttank.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFloatingActionButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gasmonsoft.fuelboxcontrol.data.model.selectvehicle.Tank
import com.gasmonsoft.fuelboxcontrol.ui.selecttank.viewmodel.SelectTankUiState
import com.gasmonsoft.fuelboxcontrol.ui.selecttank.viewmodel.SelectVehicleScreen

@Composable
fun SelectTankRoute(onBack: () -> Unit, modifier: Modifier = Modifier) {
    SelectTankScreen(uiState = SelectTankUiState(), onBack = onBack, modifier = modifier)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectTankScreen(
    uiState: SelectTankUiState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(title = { }, navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Regresar")
                }
            })
        }
    ) { paddingValues ->
        Surface(modifier = Modifier.padding(paddingValues)) {
            PrimaryTabRow(
                selectedTabIndex = uiState.screen.index,
            ) {
                Tab(
                    selected = uiState.screen.index == SelectVehicleScreen.Vehicles().index,
                    onClick = {

                    },
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
                    onClick = {

                    },
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
}

@Composable
fun SelectTankVehicleScreen(vehicles: List<Tank>) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(vehicles.size, key = { index -> vehicles[index].id }) { index ->
            Text(text = vehicles[index].name)
        }
    }
}

@Composable
fun SelectTankOtherScreen(otros: List<Tank>, modifier: Modifier = Modifier) {
    Box {
        LazyColumn(modifier = modifier.fillMaxSize()) {
            items(otros.size, key = { index -> otros[index].id }) { index ->
                Text(text = otros[index].name)
            }
        }

        LargeFloatingActionButton(
            onClick = { },
            shape = CircleShape,
            modifier = Modifier
                .padding(16.dp)
                .align(Alignment.BottomEnd)
        ) {
            Icon(Icons.Filled.Add, "Large floating action button")
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun SelectTankScreenPreview() {
    SelectTankScreen(uiState = SelectTankUiState(), onBack = {})
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun SelectTankOtherScreenPreview() {
    SelectTankOtherScreen(otros = listOf())
}