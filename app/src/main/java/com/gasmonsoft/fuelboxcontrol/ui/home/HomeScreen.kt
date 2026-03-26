package com.gasmonsoft.fuelboxcontrol.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.gasmonsoft.fuelboxcontrol.R
import com.gasmonsoft.fuelboxcontrol.data.model.ble.ConnectionState
import com.gasmonsoft.fuelboxcontrol.data.repository.ble.Device
import com.gasmonsoft.fuelboxcontrol.ui.commons.LoadingDialog
import com.gasmonsoft.fuelboxcontrol.ui.commons.SectionTitle
import com.gasmonsoft.fuelboxcontrol.ui.theme.FuelBoxControlTheme
import com.gasmonsoft.fuelboxcontrol.utils.NetworkConfig

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
    onNavToSensorView: () -> Unit
) {
    val devices by viewModel.availableDevices.collectAsState()
    val connectionStatus by viewModel.connectionStatus.collectAsState(
        initial = ConnectionState.Disconnected
    )

    val hasConfiguration = NetworkConfig.nombreconfiguracion.isNotEmpty()
    val lifecycleOwner = LocalLifecycleOwner.current

    // Trigger scan every time the screen becomes visible (Resume)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.scanDevices()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(connectionStatus, hasConfiguration) {
        if (hasConfiguration && connectionStatus == ConnectionState.Connected) {
            onNavToSensorView()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        HomeScreenContent(
            devices = devices,
            connectionStatus = connectionStatus,
            onConnect = { mac ->
                viewModel.selectDevice(mac)
            },
            onRescan = {
                viewModel.scanDevices()
            }
        )

        if (hasConfiguration && connectionStatus == ConnectionState.CurrentlyInitializing) {
            LoadingDialog(
                title = "Iniciando conexión",
                message = "Por favor espere mientras se configura la conexión..."
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreenContent(
    devices: Set<Device>,
    connectionStatus: ConnectionState,
    onConnect: (mac: String) -> Unit,
    onRescan: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sortedDevices = remember(devices) {
        devices.sortedBy { it.rssi }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Image(
                    painter = painterResource(R.drawable.fbc_icon),
                    contentDescription = null,
                    modifier = Modifier.size(60.dp)
                )
                SectionTitle(
                    title = "Dispositivos encontrados",
                    subtitle = "Seleccione uno de los dispositivos DIESEL para conectarse."
                )

                AssistChip(
                    onClick = {},
                    label = {
                        Text("${sortedDevices.size} dispositivo(s)")
                    }
                )
            }
        }

        item {
            OutlinedButton(
                onClick = onRescan,
                modifier = Modifier.fillMaxWidth(),
                enabled = connectionStatus != ConnectionState.CurrentlyInitializing
            ) {
                Text("Buscar nuevamente")
            }
        }

        if (sortedDevices.isEmpty()) {
            item {
                EmptyDevicesCard(
                    isBusy = connectionStatus == ConnectionState.CurrentlyInitializing
                )
            }
        } else {
            item {
                Text(
                    text = "Los dispositivos más cercanos son:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            items(
                items = sortedDevices,
                key = { it.mac }
            ) { device ->
                DeviceCard(
                    device = device,
                    enabled = true,
                    onClick = { onConnect(device.mac) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeviceCard(
    device: Device,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            ) {
                Icon(
                    imageVector = Icons.Default.Bluetooth,
                    contentDescription = null,
                    modifier = Modifier.padding(12.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = device.mac,
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EmptyDevicesCard(
    isBusy: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.SearchOff,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = if (isBusy) {
                    "Buscando dispositivos..."
                } else {
                    "No se encontraron dispositivos"
                },
                style = MaterialTheme.typography.titleSmall
            )

            Text(
                text = if (isBusy) {
                    "Espere un momento mientras finaliza el escaneo."
                } else {
                    "Asegúrese de que el dispositivo esté encendido y vuelva a intentar."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
            ),
            connectionStatus = ConnectionState.Disconnected,
            onConnect = {},
            onRescan = {}
        )
    }
}
