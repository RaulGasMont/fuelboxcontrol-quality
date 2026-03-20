package com.gasmonsoft.fuelboxcontrol.ui.sensor.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.BluetoothSearching
import androidx.compose.material.icons.rounded.Battery6Bar
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LinkOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gasmonsoft.fuelboxcontrol.data.ble.AccelerometerData
import com.gasmonsoft.fuelboxcontrol.data.ble.ConnectionState
import com.gasmonsoft.fuelboxcontrol.data.ble.SensorData
import com.gasmonsoft.fuelboxcontrol.data.ble.SensorState
import com.gasmonsoft.fuelboxcontrol.ui.sensor.components.SensorDataCaption
import com.gasmonsoft.fuelboxcontrol.ui.sensor.components.SensorDataCard
import com.gasmonsoft.fuelboxcontrol.ui.sensor.components.SingleSensorDataCard
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
    val bleConnectionState by viewModelSensor.connectionState.collectAsState()
    val sensorInfoState by viewModelSensor.sensorInfoState.collectAsState()

    val sensorMessage by viewModelSensor.Message.observeAsState("")
    val bateria by viewModelSensor.bateria.observeAsState("")

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

    SensorScreenContent(
        onBack = {
            viewModelSensor.disconnect()
            onBack()
        },
        bleConnectionState = bleConnectionState,
        sensorInfoState = sensorInfoState,
        permissionState = permissionState,
        sensorMessage = sensorMessage,
        bateria = bateria,
        onDisconnect = viewModelSensor::disconnect,
        onReconnect = viewModelSensor::reconnect,
        onInitializeConnection = viewModelSensor::initializeConnection,
        onWriteEinc = viewModelSensor::onwriteEinc,
        onWriteRTC = viewModelSensor::onwriteRTC
    )
}

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SensorScreenContent(
    onBack: () -> Unit,
    bleConnectionState: ConnectionState,
    sensorInfoState: SensorState,
    permissionState: MultiplePermissionsState,
    sensorMessage: String,
    bateria: String,
    onDisconnect: () -> Unit,
    onReconnect: () -> Unit,
    onInitializeConnection: () -> Unit,
    onWriteEinc: (String) -> Unit,
    onWriteRTC: (String) -> Unit
) {
    BackHandler {
        onDisconnect()
        onBack()
    }

    LaunchedEffect(Unit) {
        if (!permissionState.allPermissionsGranted) {
            permissionState.launchMultiplePermissionRequest()
        }
    }

    LaunchedEffect(permissionState.allPermissionsGranted, bleConnectionState) {
        if (permissionState.allPermissionsGranted &&
            bleConnectionState == ConnectionState.Uninitialized
        ) {
            onInitializeConnection()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Caja de comunicaciones",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            onDisconnect()
                            onBack()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Regresar"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                    scrolledContainerColor = Color.Unspecified,
                    navigationIconContentColor = Color.Unspecified,
                    titleContentColor = Color.Unspecified,
                    actionIconContentColor = Color.Unspecified
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
                            title = "Estado de conexión",
                            subtitle = "Control de la sesión Bluetooth actual"
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        ConnectionStatusRow(
                            state = bleConnectionState,
                            onReconnect = onReconnect,
                            onDisconnect = {
                                onDisconnect()
                                onBack()
                            }
                        )
                    }
                }

                item {
                    SensorSectionCard {
                        SectionHeader(
                            title = "Características",
                            subtitle = "Resumen actual de batería y mensaje del sensor"
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        if (bateria.isNotBlank()) {
                            InfoLine(
                                title = "Comandos",
                                value = bateria,
                                icon = Icons.Rounded.Battery6Bar
                            )
                        }

                        if (sensorMessage.isNotBlank()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            InfoLine(
                                title = "Mensaje",
                                value = sensorMessage,
                                icon = Icons.Rounded.Info
                            )
                        }

                        if (bateria.isBlank() && sensorMessage.isBlank()) {
                            EmptyInfoState("Aún no hay información disponible.")
                        }
                    }
                }

                item {
                    CommandSection(
                        title = "Comando EINC",
                        subtitle = "Calibra el acelerómetro con base en la posición actual del vehículo.",
                        options = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    onClick = { onWriteEinc("x") },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.textButtonColors(
                                        MaterialTheme.colorScheme.secondaryFixed
                                    )
                                ) {
                                    Text(
                                        text = "X",
                                        color = MaterialTheme.colorScheme.onSecondaryFixed
                                    )
                                }
                                TextButton(
                                    onClick = { onWriteEinc("y") },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.textButtonColors(
                                        MaterialTheme.colorScheme.secondaryFixed
                                    )
                                ) {
                                    Text(
                                        text = "Y",
                                        color = MaterialTheme.colorScheme.onSecondaryFixed
                                    )
                                }
                                TextButton(
                                    onClick = { onWriteEinc("z") },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.textButtonColors(
                                        MaterialTheme.colorScheme.secondaryFixed
                                    )
                                ) {
                                    Text(
                                        text = "Z",
                                        color = MaterialTheme.colorScheme.onSecondaryFixed
                                    )
                                }
                            }
                        }
                    )
                }

                item {
                    CommandSection(
                        title = "Comando RTC",
                        subtitle = "Actualiza fecha y hora en la caja de comunicaciones.",
                        options = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                TextButton(
                                    onClick = { onWriteRTC("0") },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.textButtonColors(
                                        MaterialTheme.colorScheme.secondaryFixed
                                    )
                                ) {
                                    Text(
                                        text = "Fecha",
                                        color = MaterialTheme.colorScheme.onSecondaryFixed
                                    )
                                }
                                TextButton(
                                    onClick = { onWriteRTC("1") },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.textButtonColors(
                                        MaterialTheme.colorScheme.secondaryFixed
                                    )
                                ) {
                                    Text(
                                        text = "Hora",
                                        color = MaterialTheme.colorScheme.onSecondaryFixed
                                    )
                                }
                            }
                        }
                    )
                }

                item {
                    SectionHeader(
                        title = "Sensores",
                        subtitle = "Lecturas y acelerómetro"
                    )
                }

                item {
                    val sensor1 = sensorInfoState.sensor1.rawData
                    val sensor2 = sensorInfoState.sensor2.rawData
                    val sensor3 = sensorInfoState.sensor3.rawData
                    val sensor4 = sensorInfoState.sensor4.rawData

                    val allSensorDataAvailability = (sensor1 != "-555" && sensor1.isNotBlank()) &&
                            (sensor2 != "-555" && sensor2.isNotBlank()) &&
                            (sensor3 != "-555" && sensor3.isNotBlank()) &&
                            (sensor4 != "-555" && sensor4.isNotBlank())

                    SensorDataCaption(
                        isAccelerometer = sensorInfoState.acelerometro.value.isNotBlank(),
                        isAllSensorData = allSensorDataAvailability,
                    )
                }

                item {
                    SensorDataCard(
                        numSensor = "1",
                        sensorData = sensorInfoState.sensor1
                    )
                }

                item {
                    SensorDataCard(
                        numSensor = "2",
                        sensorData = sensorInfoState.sensor2
                    )
                }

                item {
                    SensorDataCard(
                        numSensor = "3",
                        sensorData = sensorInfoState.sensor3
                    )
                }

                item {
                    SensorDataCard(
                        numSensor = "4",
                        sensorData = sensorInfoState.sensor4
                    )
                }

                item {
                    SingleSensorDataCard(
                        accelerometerData = sensorInfoState.acelerometro
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun PermissionRequiredContent(
    onRequestPermissions: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        SensorSectionCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.BluetoothSearching,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Permisos requeridos",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "La app necesita permisos Bluetooth para buscar, conectar y comunicarse con la caja.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onRequestPermissions,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Conceder permisos")
            }
        }
    }
}

@Composable
fun ConnectionStatusRow(
    state: ConnectionState,
    onReconnect: () -> Unit,
    onDisconnect: () -> Unit
) {
    val (label, containerColor, contentColor) = when (state) {
        ConnectionState.Connected -> Triple(
            "Conectado",
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer
        )

        ConnectionState.Disconnected -> Triple(
            "Desconectado",
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer
        )

        ConnectionState.Uninitialized -> Triple(
            "Inicializando",
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer
        )

        else -> Triple(
            "Desconocido",
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer
        )
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(999.dp),
            color = containerColor
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Bluetooth,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = contentColor
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
//            OutlinedButton(
//                onClick = onReconnect,
//                modifier = Modifier.weight(1f),
//                shape = RoundedCornerShape(16.dp)
//            ) {
//                Icon(
//                    imageVector = Icons.Rounded.Refresh,
//                    contentDescription = null
//                )
//                Spacer(modifier = Modifier.width(8.dp))
//                Text("Reconectar")
//            }

            Button(
                onClick = onDisconnect,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.LinkOff,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Desconectar")
            }
        }
    }
}

@Composable
fun CommandSection(
    title: String,
    subtitle: String,
    options: @Composable () -> Unit,
) {
    SensorSectionCard {
        SectionHeader(
            title = title,
            subtitle = subtitle
        )

        Spacer(modifier = Modifier.height(18.dp))

        options()
    }
}

@Composable
fun SensorSectionCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.10f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            content()
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    subtitle: String
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun InfoLine(
    title: String,
    value: String,
    icon: ImageVector
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun EmptyInfoState(text: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Preview(showBackground = true)
@Composable
fun SensorScreenPreview() {
    FuelBoxControlTheme {
        SensorScreenContent(
            onBack = {},
            bleConnectionState = ConnectionState.Connected,
            sensorInfoState = SensorState(
                sensor1 = SensorData("", false, "2024-01-01", "25°C", "100L", "Good"),
                sensor2 = SensorData("", false, "2024-01-01", "26°C", "150L", "Normal"),
                sensor3 = SensorData("", false, "2024-01-01", "24°C", "200L", "Good"),
                sensor4 = SensorData("", false, "2024-01-01", "27°C", "50L", "Bad"),
                acelerometro = AccelerometerData("", false, "2024-01-01", "100")
            ),
            permissionState = object : MultiplePermissionsState {
                override val allPermissionsGranted: Boolean = true
                override val permissions: List<PermissionState> =
                    emptyList()
                override val revokedPermissions: List<PermissionState> =
                    emptyList()
                override val shouldShowRationale: Boolean = false
                override fun launchMultiplePermissionRequest() {}
            },
            sensorMessage = "Test Message",
            bateria = "80%",
            onDisconnect = {},
            onReconnect = {},
            onInitializeConnection = {},
            onWriteEinc = {},
            onWriteRTC = {},
        )
    }
}
