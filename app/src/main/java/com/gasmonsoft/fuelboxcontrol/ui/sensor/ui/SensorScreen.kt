package com.gasmonsoft.fuelboxcontrol.ui.sensor.ui

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.Battery6Bar
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gasmonsoft.fuelboxcontrol.data.model.ble.ConnectionState
import com.gasmonsoft.fuelboxcontrol.data.model.ble.SensorData
import com.gasmonsoft.fuelboxcontrol.data.model.ble.SensorState
import com.gasmonsoft.fuelboxcontrol.ui.sensor.components.CommandSection
import com.gasmonsoft.fuelboxcontrol.ui.sensor.components.ConnectionStatusRow
import com.gasmonsoft.fuelboxcontrol.ui.sensor.components.InfoLine
import com.gasmonsoft.fuelboxcontrol.ui.sensor.components.PermissionRequiredContent
import com.gasmonsoft.fuelboxcontrol.ui.sensor.components.SectionHeader
import com.gasmonsoft.fuelboxcontrol.ui.sensor.components.SensorDataCaption
import com.gasmonsoft.fuelboxcontrol.ui.sensor.components.SensorDataCard
import com.gasmonsoft.fuelboxcontrol.ui.sensor.components.SensorSectionCard
import com.gasmonsoft.fuelboxcontrol.ui.sensor.viewmodel.SensorUiEvent
import com.gasmonsoft.fuelboxcontrol.ui.sensor.viewmodel.SensorUiState
import com.gasmonsoft.fuelboxcontrol.ui.sensor.viewmodel.SensorViewModel
import com.gasmonsoft.fuelboxcontrol.ui.theme.FuelBoxControlTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.rememberMultiplePermissionsState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun SensorRoute(
    onBack: () -> Unit,
    viewModelSensor: SensorViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModelSensor.uiState.collectAsStateWithLifecycle()
    val sensorInfoState by viewModelSensor.sensorInfoState.collectAsStateWithLifecycle()

    val permissions = remember {
        buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(Manifest.permission.BLUETOOTH_SCAN)
                add(Manifest.permission.BLUETOOTH_CONNECT)
            } else {
                add(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    val permissionState = rememberMultiplePermissionsState(permissions = permissions)

    LaunchedEffect(Unit) {
        viewModelSensor.events.collect { event ->
            when (event) {
                is SensorUiEvent.ShowToast -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_LONG).show()
                }

                is SensorUiEvent.ShowError -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        if (!permissionState.allPermissionsGranted) {
            permissionState.launchMultiplePermissionRequest()
        }
    }

    LaunchedEffect(permissionState.allPermissionsGranted) {
        if (permissionState.allPermissionsGranted) {
            viewModelSensor.initializeConnection()
        }
    }

    SensorScreenContent(
        uiState = uiState,
        sensorInfoState = sensorInfoState,
        permissionState = permissionState,
        onDisconnect = {
            viewModelSensor.disconnect()
        },
        onBack = {
            viewModelSensor.disconnect()
            onBack()
        },
        onReconnect = viewModelSensor::reconnect,
        onInitializeConnection = viewModelSensor::initializeConnection,
        onWriteEinc = viewModelSensor::onWriteEinc,
        onWriteRTC = viewModelSensor::onWriteRTC
    )
}

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SensorScreenContent(
    uiState: SensorUiState,
    sensorInfoState: SensorState,
    permissionState: MultiplePermissionsState,
    onDisconnect: () -> Unit,
    onBack: () -> Unit,
    onReconnect: () -> Unit,
    onInitializeConnection: () -> Unit,
    onWriteEinc: (String) -> Unit,
    onWriteRTC: (String) -> Unit
) {
    BackHandler {
        onBack()
    }

    val isConnected = uiState.connectionState == ConnectionState.Connected
    val isInitializing = uiState.connectionState == ConnectionState.CurrentlyInitializing

    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
            MaterialTheme.colorScheme.background,
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.30f)
        )
    )

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
            .statusBarsPadding()
            .navigationBarsPadding(),
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundBrush)
        ) {
            if (!permissionState.allPermissionsGranted) {
                PermissionRequiredContent(
                    modifier = Modifier.fillMaxSize(),
                    onRequestPermissions = { permissionState.launchMultiplePermissionRequest() }
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    item {
                        SensorTopBar(
                            onBack = onBack,
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 560.dp)
                        )
                    }

                    item {
                        SensorSectionCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 560.dp)
                        ) {
                            SectionHeader(
                                title = "Conexión del dispositivo",
                                subtitle = "Revise el estado actual de la caja y gestione la conexión."
                            )

                            Spacer(modifier = Modifier.height(18.dp))

                            ConnectionStatusRow(
                                state = uiState.connectionState,
                                initializingMessage = uiState.initializingMessage,
                                errorMessage = uiState.errorMessage,
                                onReconnect = onReconnect,
                                onInitialize = onInitializeConnection,
                                onDisconnect = onDisconnect
                            )
                        }
                    }

                    item {
                        SensorSectionCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 560.dp)
                        ) {
                            SectionHeader(
                                title = "Resumen del dispositivo",
                                subtitle = "Información general y mensajes recientes del sistema."
                            )

                            Spacer(modifier = Modifier.height(18.dp))

                            InfoLine(
                                title = "Estado de batería / comando",
                                value = uiState.bateria.ifBlank { "Sin información disponible" },
                                icon = Icons.Rounded.Battery6Bar
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            InfoLine(
                                title = "Mensaje del sistema",
                                value = uiState.sensorMessage.ifBlank { "Sin mensajes disponibles" },
                                icon = Icons.Rounded.Info
                            )
                        }
                    }

                    item {
                        CommandSection(
                            title = "Calibrar acelerómetro",
                            subtitle = "Seleccione el eje de referencia para calibrar el acelerómetro."
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CommandActionButton(
                                    text = "Eje X",
                                    enabled = isConnected && !isInitializing,
                                    modifier = Modifier.weight(1f),
                                    onClick = { onWriteEinc("x") }
                                )

                                CommandActionButton(
                                    text = "Eje Y",
                                    enabled = isConnected && !isInitializing,
                                    modifier = Modifier.weight(1f),
                                    onClick = { onWriteEinc("y") }
                                )

                                CommandActionButton(
                                    text = "Eje Z",
                                    enabled = isConnected && !isInitializing,
                                    modifier = Modifier.weight(1f),
                                    onClick = { onWriteEinc("z") }
                                )
                            }
                        }
                    }

                    item {
                        CommandSection(
                            title = "Actualizar fecha y hora",
                            subtitle = "Sincronice la fecha y la hora del dispositivo."
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CommandActionButton(
                                    text = "Actualizar fecha",
                                    enabled = isConnected && !isInitializing,
                                    modifier = Modifier.weight(1f),
                                    onClick = { onWriteRTC("0") }
                                )

                                CommandActionButton(
                                    text = "Actualizar hora",
                                    enabled = isConnected && !isInitializing,
                                    modifier = Modifier.weight(1f),
                                    onClick = { onWriteRTC("1") }
                                )
                            }
                        }
                    }

                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 560.dp)
                        ) {
                            SectionHeader(
                                title = "Lecturas de sensores",
                                subtitle = "Consulte las lecturas recibidas de cada sensor."
                            )
                        }
                    }

                    item {
                        val sensor1 = sensorInfoState.sensor1.rawData
                        val hasSensorData = sensor1 != "-555" && sensor1.isNotBlank()

                        SensorDataCaption(
                            isAllSensorData = hasSensorData,
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 560.dp)
                        )
                    }

                    item {
                        SensorDataCard(
                            numSensor = "1",
                            sensorData = sensorInfoState.sensor1,
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 560.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CommandActionButton(
    text: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    FilledTonalButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(50.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(
            text = text,
            fontWeight = FontWeight.SemiBold
        )
    }
}


@Composable
private fun SensorTopBar(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledTonalIconButton(
                onClick = onBack,
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Regresar"
                )
            }

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Monitoreo de caja",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Lecturas, conexión y comandos del dispositivo.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun SensorScreenContentPreview() {
    FuelBoxControlTheme {
        SensorScreenContent(
            uiState = SensorUiState(
                connectionState = ConnectionState.Connected,
                bateria = "85%",
                sensorMessage = "Todo normal"
            ),
            sensorInfoState = SensorState(
                sensor1 = SensorData(rawData = "25.4", error = true)
            ),
            permissionState = object : MultiplePermissionsState {
                override val allPermissionsGranted: Boolean = true
                override val permissions: List<PermissionState> = emptyList()
                override val revokedPermissions: List<PermissionState> = emptyList()
                override val shouldShowRationale: Boolean = false
                override fun launchMultiplePermissionRequest() {}
            },
            onDisconnect = {},
            onBack = {},
            onReconnect = {},
            onInitializeConnection = {},
            onWriteEinc = {},
            onWriteRTC = {}
        )
    }
}