package com.gasmonsoft.fuelboxcontrol.ui.home.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.gasmonsoft.fuelboxcontrol.R
import com.gasmonsoft.fuelboxcontrol.data.model.ble.ConnectionState
import com.gasmonsoft.fuelboxcontrol.data.model.boxlogin.QualityBox
import com.gasmonsoft.fuelboxcontrol.ui.commons.ErrorDialog
import com.gasmonsoft.fuelboxcontrol.ui.commons.LoadingDialog
import com.gasmonsoft.fuelboxcontrol.ui.home.viewmodel.HomeState
import com.gasmonsoft.fuelboxcontrol.ui.home.viewmodel.HomeViewModel
import com.gasmonsoft.fuelboxcontrol.ui.theme.FuelBoxControlTheme
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

    HomeScreen(
        uiState = uiState,
        logoutEvent = logoutEvent,
        connectionStatus = connectionStatus,
        hasConfiguration = NetworkConfig.nombreconfiguracion.isNotEmpty(),
        onScanDevices = viewModel::scanDevices,
        onConnect = viewModel::selectDevice,
        onLogout = viewModel::logout,
        onDismissLogoutError = viewModel::dismissLogoutError,
        onNavToLogin = onNavToLogin,
        onNavToSensorView = onNavToSensorView,
        modifier = modifier
    )
}

@Composable
private fun HomeScreen(
    uiState: HomeState,
    logoutEvent: ProcessingEvent,
    connectionStatus: ConnectionState,
    hasConfiguration: Boolean,
    onScanDevices: () -> Unit,
    onConnect: (QualityBox) -> Unit,
    onLogout: () -> Unit,
    onDismissLogoutError: () -> Unit,
    onNavToLogin: () -> Unit,
    onNavToSensorView: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(uiState.sessionExpired, logoutEvent) {
        if (uiState.sessionExpired && logoutEvent !is ProcessingEvent.Loading) {
            onNavToLogin()
        }
    }

    LaunchedEffect(logoutEvent) {
        if (logoutEvent is ProcessingEvent.Success) {
            onNavToLogin()
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                onScanDevices()
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(hasConfiguration, connectionStatus) {
        if (hasConfiguration && connectionStatus == ConnectionState.Connected) {
            onNavToSensorView()
        }
    }

    when (logoutEvent) {
        is ProcessingEvent.Error -> {
            ErrorDialog(
                message = "No se pudo cerrar la sesión. Intente de nuevo.",
                onDismiss = onDismissLogoutError
            )
        }

        is ProcessingEvent.Loading -> {
            LoadingDialog()
        }

        else -> Unit
    }

    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
            MaterialTheme.colorScheme.background,
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)
        )
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundBrush)
    ) {
        HomeScreenContent(
            devices = uiState.boxes,
            hasConfiguration = hasConfiguration,
            onConnect = onConnect,
            onLogout = onLogout
        )

        if (uiState.connectingBoxName != null && connectionStatus == ConnectionState.CurrentlyInitializing) {
            LoadingDialog(
                title = "Iniciando conexión",
                message = "Por favor espere mientras se configura la conexión..."
            )
        }
    }
}

@Composable
fun HomeScreenContent(
    devices: List<QualityBox>,
    hasConfiguration: Boolean,
    onConnect: (box: QualityBox) -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        containerColor = Color.Transparent
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp)
        ) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    HomeHeaderCard(
                        totalDevices = devices.size,
                        hasConfiguration = hasConfiguration,
                        onLogout = onLogout,
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 520.dp)
                    )
                }
            }

            if (devices.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        EmptyDevicesCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 520.dp)
                        )
                    }
                }
            } else {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 520.dp)
                        ) {
                            Text(
                                text = "Dispositivos disponibles",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onBackground
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "Seleccione una caja de calidad para conectarse.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                items(
                    items = devices,
                    key = { it.id }
                ) { device ->
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        DeviceCard(
                            device = device,
                            enabled = true,
                            onClick = { onConnect(device) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 520.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeHeaderCard(
    totalDevices: Int,
    hasConfiguration: Boolean,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 22.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier
                        .weight(3f)
                        .padding(end = 1.dp)
                ) {
                    Surface(
                        modifier = Modifier.size(64.dp),
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(R.drawable.fbc_icon),
                                contentDescription = null,
                                modifier = Modifier.size(38.dp)
                            )
                        }
                    }

                    Column(
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "FuelBoxControl",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "Cajas de calidad asignadas",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }


                FilledTonalIconButton(
                    modifier = Modifier.weight(1f),
                    onClick = onLogout,
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Logout,
                        contentDescription = "Cerrar sesión"
                    )
                }
            }

            Text(
                text = "Administre y seleccione el dispositivo con el que desea trabajar.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
            ) {
                AssistChip(
                    onClick = {},
                    label = {
                        Text(
                            text = "$totalDevices dispositivo(s)",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Devices,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
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
    ElevatedCard(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f)
        ),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = 6.dp,
            pressedElevation = 2.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(52.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Bluetooth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = device.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
            ) {
                Box(
                    modifier = Modifier.padding(10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyDevicesCard(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                modifier = Modifier.size(72.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.SearchOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Text(
                text = "No se encontraron dispositivos",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = "Actualmente no cuenta con cajas asignadas. Verifique su asignación o intente nuevamente más tarde.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(showBackground = true, name = "With Devices")
@Composable
fun HomeScreenWithDevicesPreview() {
    FuelBoxControlTheme {
        HomeScreen(
            uiState = HomeState(
                boxes = listOf(
                    QualityBox(1, "00:11:22:33:44:55", "Box Alpha"),
                    QualityBox(2, "AA:BB:CC:DD:EE:FF", "Box Beta")
                )
            ),
            logoutEvent = ProcessingEvent.Idle,
            connectionStatus = ConnectionState.Disconnected,
            hasConfiguration = true,
            onScanDevices = {},
            onConnect = {},
            onLogout = {},
            onDismissLogoutError = {},
            onNavToLogin = {},
            onNavToSensorView = {}
        )
    }
}

@Preview(showBackground = true, name = "Empty Devices")
@Composable
fun HomeScreenEmptyPreview() {
    FuelBoxControlTheme {
        HomeScreen(
            uiState = HomeState(boxes = emptyList()),
            logoutEvent = ProcessingEvent.Idle,
            connectionStatus = ConnectionState.Disconnected,
            hasConfiguration = false,
            onScanDevices = {},
            onConnect = {},
            onLogout = {},
            onDismissLogoutError = {},
            onNavToLogin = {},
            onNavToSensorView = {}
        )
    }
}

@Preview(showBackground = true, name = "Loading State")
@Composable
fun HomeScreenLoadingPreview() {
    FuelBoxControlTheme {
        HomeScreen(
            uiState = HomeState(
                boxes = listOf(QualityBox(1, "00:11:22:33:44:55", "Box Alpha"))
            ),
            logoutEvent = ProcessingEvent.Idle,
            connectionStatus = ConnectionState.CurrentlyInitializing,
            hasConfiguration = true,
            onScanDevices = {},
            onConnect = {},
            onLogout = {},
            onDismissLogoutError = {},
            onNavToLogin = {},
            onNavToSensorView = {}
        )
    }
}

@Preview(showBackground = true, name = "Logout Error")
@Composable
fun HomeScreenLogoutErrorPreview() {
    FuelBoxControlTheme {
        HomeScreen(
            uiState = HomeState(
                boxes = listOf(QualityBox(1, "00:11:22:33:44:55", "Box Alpha"))
            ),
            logoutEvent = ProcessingEvent.Error,
            connectionStatus = ConnectionState.Disconnected,
            hasConfiguration = true,
            onScanDevices = {},
            onConnect = {},
            onLogout = {},
            onDismissLogoutError = {},
            onNavToLogin = {},
            onNavToSensorView = {}
        )
    }
}
