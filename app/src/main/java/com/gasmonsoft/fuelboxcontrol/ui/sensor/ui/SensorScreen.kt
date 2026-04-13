package com.gasmonsoft.fuelboxcontrol.ui.sensor.ui

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Battery6Bar
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
            onBack()
        },
        onReconnect = viewModelSensor::reconnect,
        onInitializeConnection = viewModelSensor::initializeConnection,
        onWriteEinc = viewModelSensor::onwriteEinc,
        onWriteRTC = viewModelSensor::onwriteRTC
    )
}

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SensorScreenContent(
    uiState: SensorUiState,
    sensorInfoState: SensorState,
    permissionState: MultiplePermissionsState,
    onDisconnect: () -> Unit,
    onReconnect: () -> Unit,
    onInitializeConnection: () -> Unit,
    onWriteEinc: (String) -> Unit,
    onWriteRTC: (String) -> Unit
) {
    BackHandler {
        onDisconnect()
    }

    val isConnected = uiState.connectionState == ConnectionState.Connected
    val isInitializing = uiState.connectionState == ConnectionState.CurrentlyInitializing

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Monitoreo de caja",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onDisconnect) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Regresar"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                )
            )
        }
    ) { paddingValues ->
        if (!permissionState.allPermissionsGranted) {
            PermissionRequiredContent(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                onRequestPermissions = { permissionState.launchMultiplePermissionRequest() }
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    SensorSectionCard {
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
                    SensorSectionCard {
                        SectionHeader(
                            title = "Resumen del dispositivo",
                            subtitle = "Información general y mensajes recientes del dispositivo."
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
                        subtitle = "Seleccione el eje de referencia para calibrar el acelerómetro.",
                        options = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    onClick = { onWriteEinc("x") },
                                    enabled = isConnected && !isInitializing,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.textButtonColors(
                                        containerColor = MaterialTheme.colorScheme.secondary
                                    )
                                ) {
                                    Text(
                                        text = "Eje X",
                                        color = MaterialTheme.colorScheme.onSecondary
                                    )
                                }

                                TextButton(
                                    onClick = { onWriteEinc("y") },
                                    enabled = isConnected && !isInitializing,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.textButtonColors(
                                        containerColor = MaterialTheme.colorScheme.secondary
                                    )
                                ) {
                                    Text(
                                        text = "Eje Y",
                                        color = MaterialTheme.colorScheme.onSecondary
                                    )
                                }

                                TextButton(
                                    onClick = { onWriteEinc("z") },
                                    enabled = isConnected && !isInitializing,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.textButtonColors(
                                        containerColor = MaterialTheme.colorScheme.secondary
                                    )
                                ) {
                                    Text(
                                        text = "Eje Z",
                                        color = MaterialTheme.colorScheme.onSecondary
                                    )
                                }
                            }
                        }
                    )
                }

                item {
                    CommandSection(
                        title = "Actualizar fecha y hora",
                        subtitle = "Sincronice la fecha y la hora del dispositivo.",
                        options = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                TextButton(
                                    onClick = { onWriteRTC("0") },
                                    enabled = isConnected && !isInitializing,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.textButtonColors(
                                        containerColor = MaterialTheme.colorScheme.secondary
                                    )
                                ) {
                                    Text(
                                        text = "Actualizar fecha",
                                        color = MaterialTheme.colorScheme.onSecondary
                                    )
                                }

                                TextButton(
                                    onClick = { onWriteRTC("1") },
                                    enabled = isConnected && !isInitializing,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.textButtonColors(
                                        containerColor = MaterialTheme.colorScheme.secondary
                                    )
                                ) {
                                    Text(
                                        text = "Actualizar hora",
                                        color = MaterialTheme.colorScheme.onSecondary
                                    )
                                }
                            }
                        }
                    )
                }

                item {
                    SectionHeader(
                        title = "Lecturas de sensores",
                        subtitle = "Consulte las lecturas recibidas de cada sensor."
                    )
                }

                item {
                    val sensor1 = sensorInfoState.sensor1.rawData

                    val allSensorDataAvailability =
                        (sensor1 != "-555" && sensor1.isNotBlank())

                    SensorDataCaption(
                        isAllSensorData = allSensorDataAvailability,
                    )
                }

                item { SensorDataCard(numSensor = "1", sensorData = sensorInfoState.sensor1) }
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
            onReconnect = {},
            onInitializeConnection = {},
            onWriteEinc = {},
            onWriteRTC = {}
        )
    }
}