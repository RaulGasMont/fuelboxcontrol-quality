package com.gasmonsoft.fuelboxcontrol.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gasmonsoft.fuelboxcontrol.data.model.ble.ConnectionState
import com.gasmonsoft.fuelboxcontrol.data.repository.ble.Device
import com.gasmonsoft.fuelboxcontrol.ui.theme.FuelBoxControlTheme
import com.gasmonsoft.fuelboxcontrol.utils.NetworkConfig

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
    onNavToSensorView: () -> Unit
) {
    // Collect the state from the ViewModel
    val devices by viewModel.availableDevices.collectAsState()
    val connectionStatus by viewModel.connectionStatus.collectAsState(ConnectionState.Disconnected)

    LaunchedEffect(Unit) {
        viewModel.scanDevices()
    }

    LaunchedEffect(connectionStatus) {
        if (connectionStatus == ConnectionState.Connected && NetworkConfig.nombreconfiguracion.isNotEmpty()) {
            onNavToSensorView()
        }
    }

    // Pass the state to a stateless content Composable to allow Previews
    HomeScreenContent(
        modifier = modifier,
        devices = devices
    ) { mac ->
        viewModel.selectDevice(mac)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreenContent(
    modifier: Modifier = Modifier,
    devices: Set<Device>,
    onConnect: (mac: String) -> Unit,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Dispositivos disponibles",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        }
        items(devices.toList()) { device ->
            Card(
                onClick = { onConnect(device.mac) },
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = device.mac,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.Black
                    )
                }
            }

        }
    }
}

@Composable
@Preview(showBackground = true, showSystemUi = true)
fun HomeScreenPreview() {
    FuelBoxControlTheme {
        // Use the stateless content Composable in the Preview with mock data
        HomeScreenContent(
            modifier = Modifier
                .padding(16.dp),
            devices = setOf(
                Device("00:11:22:33:44:55", "Preview Device 1", -60),
                Device("AA:BB:CC:DD:EE:FF", "Preview Device 2", -70)
            )
        ) {}
    }
}
