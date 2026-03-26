package com.gasmonsoft.fuelboxcontrol.ui.vehiculo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Login
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Sensors
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gasmonsoft.fuelboxcontrol.R
import com.gasmonsoft.fuelboxcontrol.data.model.wifi.WifiConnectionState
import com.gasmonsoft.fuelboxcontrol.domain.sensor.SensorPackage
import com.gasmonsoft.fuelboxcontrol.model.vehicle.VehicleInfo
import com.gasmonsoft.fuelboxcontrol.ui.commons.ErrorDialog
import com.gasmonsoft.fuelboxcontrol.ui.commons.InfoRow
import com.gasmonsoft.fuelboxcontrol.ui.commons.LoadingDialog
import com.gasmonsoft.fuelboxcontrol.ui.commons.ScreenHeaderCard
import com.gasmonsoft.fuelboxcontrol.ui.commons.SectionCard
import com.gasmonsoft.fuelboxcontrol.ui.commons.SectionTitle
import com.gasmonsoft.fuelboxcontrol.ui.sensor.components.AlertMessage
import com.gasmonsoft.fuelboxcontrol.ui.theme.FuelBoxControlTheme
import com.gasmonsoft.fuelboxcontrol.ui.vehiculo.viewmodel.NetworkEvent
import com.gasmonsoft.fuelboxcontrol.ui.vehiculo.viewmodel.SensorSendingEvent
import com.gasmonsoft.fuelboxcontrol.ui.vehiculo.viewmodel.VehiculosViewModel

@Composable
fun VehiculosRoute(
    modifier: Modifier = Modifier,
    viewModel: VehiculosViewModel = hiltViewModel()
) {
    val uiState = viewModel.uiState.collectAsState()
    val sensorData = viewModel.sensorData.collectAsState(SensorPackage("", ""))
    val logData = viewModel.logSensorData.collectAsState("")
    val sendingStatus = viewModel.dataSendStatus.collectAsState(SensorSendingEvent.Idle)
    val wifiStatus = viewModel.wifiState.collectAsState()
    var usuario by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        VehiculosScreen(
            wifiStatus = wifiStatus.value,
            loggedUser = uiState.value.userData?.username ?: "",
            sensorData = sensorData.value,
            logData = logData.value,
            onLogout = { viewModel.logout() },
            dataSendingStatus = sendingStatus.value,
            isLogged = uiState.value.isLoggedIn,
            username = usuario,
            password = password,
            vehicles = uiState.value.userData?.vehiculos ?: emptyList(),
            modifier = modifier,
            currentVehicle = uiState.value.vehicleInfo,
            onLoginUsername = { usuario = it },
            onLoginPassword = { password = it },
            onLogin = {
                viewModel.doLogin(
                    username = usuario,
                    password = password
                )
                usuario = ""
                password = ""
            },
            onSelectVehicle = {
                viewModel.getVehicleData(it)
            }
        )

        when (uiState.value.loginEvent) {
            NetworkEvent.Loading -> LoadingDialog()
            is NetworkEvent.Error -> {
                ErrorDialog(
                    message = (uiState.value.loginEvent as NetworkEvent.Error).message,
                    onDismiss = { viewModel.dismissLoginDialog() }
                )
            }

            else -> {}
        }

        when (uiState.value.vehicleEvent) {
            NetworkEvent.Loading -> LoadingDialog()
            is NetworkEvent.Error -> {
                ErrorDialog(
                    message = (uiState.value.vehicleEvent as NetworkEvent.Error).message,
                    onDismiss = { viewModel.dismissLoginDialog() }
                )
            }

            else -> {}
        }
    }
}

@Composable
fun VehiculosScreen(
    wifiStatus: WifiConnectionState,
    loggedUser: String,
    logData: String,
    sensorData: SensorPackage,
    dataSendingStatus: SensorSendingEvent,
    isLogged: Boolean,
    username: String,
    password: String,
    vehicles: List<VehicleInfo>,
    onLoginUsername: (username: String) -> Unit,
    onLoginPassword: (password: String) -> Unit,
    onLogin: () -> Unit,
    onLogout: () -> Unit,
    onSelectVehicle: (Int) -> Unit,
    modifier: Modifier = Modifier,
    currentVehicle: VehicleInfo?,
) {
    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
            MaterialTheme.colorScheme.surface,
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
        )
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundBrush)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ScreenHeaderCard(
                title = stringResource(R.string.conexion_servidor),
                subtitle = "Acceso, selección de unidad y monitoreo del envío",
                icon = Icons.Rounded.Dns
            )

            Surface(
                modifier = Modifier
                    .fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = if (wifiStatus.signalLevel == 1 || !wifiStatus.wifi)
                    MaterialTheme.colorScheme.errorContainer
                else MaterialTheme.colorScheme.tertiaryContainer
            ) {
                Text(
                    text = when {
                        !wifiStatus.connected -> "Sin conexión"
                        !wifiStatus.wifi -> "Conectado, pero no por Wi-Fi"
                        else -> "Intensidad Wi-Fi: ${wifiStatus.signalMessage}"
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = if (wifiStatus.signalLevel == 1 || !wifiStatus.wifi)
                        MaterialTheme.colorScheme.onErrorContainer
                    else MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.padding(12.dp)
                )
            }

            SectionCard {
                SectionTitle(
                    title = stringResource(R.string.credenciales),
                    subtitle = "Ingresa tu usuario y contraseña para continuar"
                )

                Spacer(modifier = Modifier.height(20.dp))

                OutlinedTextField(
                    value = username,
                    onValueChange = onLoginUsername,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Person,
                            contentDescription = null
                        )
                    },
                    label = { Text(stringResource(R.string.usuario)) },
                    textStyle = MaterialTheme.typography.bodyLarge,
                    colors = outlinedFieldColors()
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = onLoginPassword,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Lock,
                            contentDescription = null
                        )
                    },
                    label = { Text(stringResource(R.string.contrasenia)) },
                    textStyle = MaterialTheme.typography.bodyLarge,
                    colors = outlinedFieldColors()
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onLogin,
                    enabled = username.isNotBlank() && password.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(18.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Login,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.iniciar_sesion),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
                if (isLogged) {
                    SessionChip(text = "Sesión iniciada correctamente")
                    Spacer(modifier = Modifier.height(16.dp))
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer
                    ) {
                        Text(
                            "Usuario Actual: $loggedUser",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onLogout,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(18.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Login,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Cerrar Sesión",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }

            VehicleSection(
                vehicles = vehicles,
                currentVehicle = currentVehicle,
                onSelectVehicle = { onSelectVehicle(it) },
            )

            SectionCard {
                SectionTitle(
                    title = stringResource(R.string.estatus_envio),
                    subtitle = "Resumen del último envío de sensores"
                )
                Spacer(modifier = Modifier.height(18.dp))

                InfoRow(
                    title = "Última trama sin procesar",
                    value = logData.trim(),
                    icon = Icons.AutoMirrored.Rounded.ReceiptLong
                )

                Spacer(modifier = Modifier.height(18.dp))

                StatusPill(status = dataSendingStatus.title)

                when (dataSendingStatus) {
                    is SensorSendingEvent.Error -> {
                        Spacer(modifier = Modifier.height(18.dp))
                        AlertMessage(
                            message = dataSendingStatus.message,
                        )
                    }

                    else -> {}
                }

                Spacer(modifier = Modifier.height(18.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        InfoRow(
                            title = "Evento",
                            value = stringResource(R.string.sensores),
                            icon = Icons.Rounded.Sensors
                        )

                        HorizontalDivider(
                            Modifier,
                            DividerDefaults.Thickness,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
                        )

                        InfoRow(
                            title = "Estado",
                            value = dataSendingStatus.title,
                            icon = Icons.Rounded.CloudDone
                        )

                        HorizontalDivider(
                            Modifier,
                            DividerDefaults.Thickness,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
                        )

                        InfoRow(
                            title = "Última trama correcta",
                            value = "${sensorData.date} ${sensorData.data}".trim(),
                            icon = Icons.AutoMirrored.Rounded.ReceiptLong
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownVehicleMenu(
    vehicles: List<VehicleInfo>,
    currentVehicle: VehicleInfo?,
    modifier: Modifier = Modifier,
    onSelectVehicle: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = currentVehicle?.description ?: "Seleccione un vehículo",
            onValueChange = {},
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
            readOnly = true,
            singleLine = true,
            shape = RoundedCornerShape(18.dp),
            label = { Text(stringResource(R.string.vehiculo)) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.DirectionsCar,
                    contentDescription = null
                )
            },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            colors = outlinedFieldColors()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = RoundedCornerShape(18.dp),
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            vehicles.forEach { vehicle ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = vehicle.description,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.DirectionsCar,
                            contentDescription = null
                        )
                    },
                    onClick = {
                        expanded = false
                        onSelectVehicle(vehicle.id.toInt())
                    }
                )
            }
        }
    }
}

@Composable
fun SessionChip(text: String) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
    }
}

@Composable
fun StatusPill(status: String) {
    val normalized = status.lowercase()
    val containerColor = when {
        "error" in normalized || "fall" in normalized || "fail" in normalized ->
            MaterialTheme.colorScheme.errorContainer

        "ok" in normalized || "enviado" in normalized || "success" in normalized ->
            MaterialTheme.colorScheme.tertiaryContainer

        else ->
            MaterialTheme.colorScheme.secondaryContainer
    }

    val contentColor = when {
        "error" in normalized || "fall" in normalized || "fail" in normalized ->
            MaterialTheme.colorScheme.onErrorContainer

        "ok" in normalized || "enviado" in normalized || "success" in normalized ->
            MaterialTheme.colorScheme.onTertiaryContainer

        else ->
            MaterialTheme.colorScheme.onSecondaryContainer
    }

    Surface(
        shape = RoundedCornerShape(999.dp),
        color = containerColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Info,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = status,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = contentColor
            )
        }
    }
}

@Composable
private fun outlinedFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = MaterialTheme.colorScheme.surface,
    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
    disabledContainerColor = MaterialTheme.colorScheme.surface,
    focusedTextColor = MaterialTheme.colorScheme.onSurface,
    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
    focusedLabelColor = MaterialTheme.colorScheme.primary,
    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    focusedIndicatorColor = MaterialTheme.colorScheme.primary,
    unfocusedIndicatorColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
    cursorColor = MaterialTheme.colorScheme.primary
)

@Preview(showBackground = true)
@Composable
fun DropdownMenuVehiclePreview() {
    FuelBoxControlTheme {
        DropdownVehicleMenu(
            vehicles = listOf(
                VehicleInfo(
                    id = "1",
                    description = "Unidad 1",
                    mac = "12:34:56:78:90"
                ),
                VehicleInfo(
                    id = "2",
                    description = "Unidad 2",
                    mac = "12:34:56:78:90"
                )
            ),
            currentVehicle = VehicleInfo(
                id = "1",
                description = "Unidad 1",
                mac = ""
            ),
            onSelectVehicle = {}
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun VehiculosRoutePreview() {
    FuelBoxControlTheme {
        VehiculosScreen(
            loggedUser = "",
            isLogged = true,
            username = "",
            password = "",
            onLoginUsername = {},
            onLoginPassword = {},
            onLogin = {},
            vehicles = emptyList(),
            onSelectVehicle = {},
            currentVehicle = VehicleInfo(
                id = "",
                description = "",
                mac = ""
            ),
            sensorData = SensorPackage("2025/04/24", "-555,-555,-555"),
            dataSendingStatus = SensorSendingEvent.Error("No se pudo enviar por que la trama no corresponde."),
            onLogout = {},
            wifiStatus = WifiConnectionState(),
            logData = "2025/04/24,-555,-555,-555",
        )
    }
}