package com.gasmonsoft.fuelboxcontrol.ui.home

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.gasmonsoft.fuelboxcontrol.ui.theme.FuelBoxControlTheme
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.text.font.FontWeight
import com.gasmonsoft.fuelboxcontrol.data.ble.Device

@Composable
fun HomeScreen(modifier: Modifier = Modifier, viewModel: HomeViewModel = hiltViewModel()) {
    // Collect the state from the ViewModel
    val devices by viewModel.availableDevices.collectAsState()

    // Pass the state to a stateless content Composable to allow Previews
    HomeScreenContent(
        modifier = modifier,
        devices = devices
    )
}

@Composable
fun HomeScreenContent(
    modifier: Modifier = Modifier,
    devices: Set<Device>
) {
    LazyColumn(
        modifier = modifier
            .safeContentPadding()
            .fillMaxSize()
    ) {
        item {
            Text(
                text = "Dispositivos disponibles",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        }
        items(devices.toList()) { device ->
            Text(text = device.name)
        }
    }
}

@Composable
@Preview(showBackground = true, showSystemUi = true)
fun HomeScreenPreview() {
    FuelBoxControlTheme {
        // Use the stateless content Composable in the Preview with mock data
        HomeScreenContent(
            devices = setOf(
                Device("00:11:22:33:44:55", "Preview Device 1", -60),
                Device("AA:BB:CC:DD:EE:FF", "Preview Device 2", -70)
            )
        )
    }
}
