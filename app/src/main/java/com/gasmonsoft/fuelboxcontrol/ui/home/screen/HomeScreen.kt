package com.gasmonsoft.fuelboxcontrol.ui.home.screen

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
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.gasmonsoft.fuelboxcontrol.R
import com.gasmonsoft.fuelboxcontrol.data.model.ble.ConnectionState
import com.gasmonsoft.fuelboxcontrol.data.model.boxlogin.QualityBox
import com.gasmonsoft.fuelboxcontrol.ui.commons.ErrorDialog
import com.gasmonsoft.fuelboxcontrol.ui.commons.LoadingDialog
import com.gasmonsoft.fuelboxcontrol.ui.commons.SectionTitle
import com.gasmonsoft.fuelboxcontrol.ui.home.viewmodel.HomeViewModel
import com.gasmonsoft.fuelboxcontrol.utils.NetworkConfig
import com.gasmonsoft.fuelboxcontrol.utils.ProcessingEvent

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
    onNavToLogin: () -> Unit,
    onNavToSensorView: () -> Unit,
) {
    val uiState by viewModel.state.collectAsState()
    val logoutEvent by viewModel.logoutEvent.collectAsState()
    val connectionStatus by viewModel.connectionStatus.collectAsState(
        initial = ConnectionState.Disconnected
    )

    val hasConfiguration = NetworkConfig.nombreconfiguracion.isNotEmpty()
    val lifecycleOwner = LocalLifecycleOwner.current


    // Redirigir a Login si no hay sesión activa
    LaunchedEffect(uiState.sessionExpired) {
        if (uiState.sessionExpired && logoutEvent !is ProcessingEvent.Loading) {
            onNavToLogin()
        }
    }

    when (logoutEvent) {
        is ProcessingEvent.Error -> {
            ErrorDialog(
                message = "No se pudo cerrar la sesión. Intente de nuevo.",
                onDismiss = viewModel::dismissLogoutError,
            )
        }

        is ProcessingEvent.Success -> {
            onNavToLogin()
        }

        is ProcessingEvent.Loading -> {
            LoadingDialog()
        }

        else -> {}
    }

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

    LaunchedEffect(connectionStatus) {
        val hasConfiguration = NetworkConfig.nombreconfiguracion.isNotEmpty()
        if (hasConfiguration && connectionStatus == ConnectionState.Connected) {
            onNavToSensorView()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        HomeScreenContent(
            devices = uiState.boxes,
            onConnect = { box ->
                viewModel.selectDevice(box)
            },
            onLogout = {
                viewModel.logout()
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
    devices: List<QualityBox>,
    onConnect: (box: QualityBox) -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(R.drawable.fbc_icon),
                        contentDescription = null,
                        modifier = Modifier.size(60.dp)
                    )

                    IconButton(onClick = onLogout) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Default.Logout,
                            contentDescription = null,
                        )
                    }
                }

                SectionTitle(
                    title = "Cajas de Calidad Asignadas",
                    subtitle = "Seleccione una de las Cajas de Calidad para conectarse."
                )

                AssistChip(
                    onClick = {},
                    label = {
                        Text("${devices.size} dispositivo(s)")
                    }
                )
            }
        }

        if (devices.isEmpty()) {
            item {
                EmptyDevicesCard()
            }
        } else {
            item {
                Text(
                    text = "Los dispositivos asignados son:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            items(
                items = devices,
                key = { it.id }) { device ->
                DeviceCard(
                    device = device,
                    enabled = true,
                    onClick = { onConnect(device) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeviceCard(
    device: QualityBox,
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
                    text = device.name,
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
private fun EmptyDevicesCard(modifier: Modifier = Modifier) {
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
                text = "No se encontraron dispositivos",
                style = MaterialTheme.typography.titleSmall
            )

            Text(
                text = "Ahora mismo no cuenta con cajas asignadas.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}