package com.gasmonsoft.fuelboxcontrol.ui.vehiculo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gasmonsoft.fuelboxcontrol.data.model.wifi.WifiConnectionState
import com.gasmonsoft.fuelboxcontrol.domain.sensor.SensorPackage
import com.gasmonsoft.fuelboxcontrol.data.model.vehicle.VehicleInfo
import com.gasmonsoft.fuelboxcontrol.ui.commons.ErrorDialog
import com.gasmonsoft.fuelboxcontrol.ui.commons.LoadingDialog
import com.gasmonsoft.fuelboxcontrol.ui.commons.ScreenHeaderCard
import com.gasmonsoft.fuelboxcontrol.ui.theme.FuelBoxControlTheme
import com.gasmonsoft.fuelboxcontrol.ui.vehiculo.components.NetworkStatusCard
import com.gasmonsoft.fuelboxcontrol.ui.vehiculo.components.SendingSummarySection
import com.gasmonsoft.fuelboxcontrol.ui.vehiculo.components.SessionSection
import com.gasmonsoft.fuelboxcontrol.ui.vehiculo.components.TechnicalLogSection
import com.gasmonsoft.fuelboxcontrol.ui.vehiculo.components.VehicleSection
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

    var usuario by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(uiState.value.isLoggedIn) {
        if (uiState.value.isLoggedIn) {
            usuario = ""
            password = ""
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        VehiculosScreen(
            wifiStatus = wifiStatus.value,
            loggedUser = uiState.value.userData?.username.orEmpty(),
            logData = logData.value,
            sensorData = sensorData.value,
            dataSendingStatus = sendingStatus.value,
            isLogged = uiState.value.isLoggedIn,
            username = usuario,
            password = password,
            vehicles = uiState.value.userData?.vehiculos ?: emptyList(),
            currentVehicle = uiState.value.vehicleInfo,
            onLoginUsername = { usuario = it },
            onLoginPassword = { password = it },
            onLogin = {
                viewModel.doLogin(
                    username = usuario.trim(),
                    password = password
                )
            },
            onLogout = { viewModel.logout() },
            onSelectVehicle = { viewModel.getVehicleData(it) },
            modifier = modifier
        )

        when (uiState.value.loginEvent) {
            NetworkEvent.Loading -> LoadingDialog()
            is NetworkEvent.Error -> {
                ErrorDialog(
                    message = (uiState.value.loginEvent as NetworkEvent.Error).message,
                    onDismiss = { viewModel.dismissLoginDialog() }
                )
            }

            else -> Unit
        }

        when (uiState.value.vehicleEvent) {
            NetworkEvent.Loading -> LoadingDialog()
            is NetworkEvent.Error -> {
                ErrorDialog(
                    message = (uiState.value.vehicleEvent as NetworkEvent.Error).message,
                    onDismiss = { viewModel.dismissLoginDialog() }
                )
            }

            else -> Unit
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
    onLoginUsername: (String) -> Unit,
    onLoginPassword: (String) -> Unit,
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
                title = "Conexión y envío",
                subtitle = "Inicia sesión, selecciona una unidad y revisa el último envío.",
                icon = Icons.Rounded.Dns
            )

            NetworkStatusCard(wifiStatus = wifiStatus)

            SessionSection(
                isLogged = isLogged,
                loggedUser = loggedUser,
                username = username,
                password = password,
                onLoginUsername = onLoginUsername,
                onLoginPassword = onLoginPassword,
                onLogin = onLogin,
                onLogout = onLogout
            )

            VehicleSection(
                vehicles = vehicles,
                currentVehicle = currentVehicle,
                isLogged = isLogged,
                onSelectVehicle = onSelectVehicle
            )

            SendingSummarySection(
                sensorData = sensorData,
                dataSendingStatus = dataSendingStatus
            )

            TechnicalLogSection(
                logData = logData
            )

            Spacer(modifier = Modifier.height(6.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun VehiculosScreenPreview() {
    val vehicles = listOf(
        VehicleInfo(id = "1", description = "Vehículo 1", mac = "00:11:22:33:44:55"),
        VehicleInfo(id = "2", description = "Vehículo 2", mac = "66:77:88:99:AA:BB")
    )
    FuelBoxControlTheme {
        VehiculosScreen(
            wifiStatus = WifiConnectionState(
                connected = true,
                wifi = true,
                signalLevel = 4,
                signalMessage = "Excelente"
            ),
            loggedUser = "Usuario Prueba",
            logData = "Log data sample...",
            sensorData = SensorPackage(date = "2023-10-27 10:00:00", data = "123.45"),
            dataSendingStatus = SensorSendingEvent.Success(message = "Enviado correctamente"),
            isLogged = true,
            username = "",
            password = "",
            vehicles = vehicles,
            currentVehicle = vehicles[0],
            onLoginUsername = {},
            onLoginPassword = {},
            onLogin = {},
            onLogout = {},
            onSelectVehicle = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun VehiculosScreenNotLoggedPreview() {
    FuelBoxControlTheme {
        VehiculosScreen(
            wifiStatus = WifiConnectionState(
                connected = true,
                wifi = true,
                signalLevel = 4,
                signalMessage = "Excelente"
            ),
            loggedUser = "",
            logData = "",
            sensorData = SensorPackage(date = "", data = ""),
            dataSendingStatus = SensorSendingEvent.Idle,
            isLogged = false,
            username = "admin",
            password = "****",
            vehicles = emptyList(),
            currentVehicle = null,
            onLoginUsername = {},
            onLoginPassword = {},
            onLogin = {},
            onLogout = {},
            onSelectVehicle = {}
        )
    }
}
