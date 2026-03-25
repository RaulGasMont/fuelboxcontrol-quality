package com.gasmonsoft.fuelboxcontrol.ui.calibracion.ui

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
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gasmonsoft.fuelboxcontrol.R
import com.gasmonsoft.fuelboxcontrol.ui.calibracion.viewmodel.CalibrationSensor
import com.gasmonsoft.fuelboxcontrol.ui.calibracion.viewmodel.CalibrationUiState
import com.gasmonsoft.fuelboxcontrol.ui.calibracion.viewmodel.CalibrationViewModel
import com.gasmonsoft.fuelboxcontrol.ui.calibracion.viewmodel.SenderCalibrationEvent
import com.gasmonsoft.fuelboxcontrol.ui.commons.ErrorDialog
import com.gasmonsoft.fuelboxcontrol.ui.commons.LoadingDialog
import com.gasmonsoft.fuelboxcontrol.ui.commons.ScreenHeaderCard
import com.gasmonsoft.fuelboxcontrol.ui.commons.SectionCard
import com.gasmonsoft.fuelboxcontrol.ui.commons.SectionTitle
import com.gasmonsoft.fuelboxcontrol.ui.commons.SuccessDialog
import com.gasmonsoft.fuelboxcontrol.ui.theme.FuelBoxControlTheme

@Composable
fun CalibracionRoute(
    idCaja: Int,
    modifier: Modifier = Modifier,
    viewModel: CalibrationViewModel = hiltViewModel()
) {
    val uiState = viewModel.calibrationUiState.collectAsState()

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CalibrationScreen(
            uiState = uiState.value,
            onTakeMeasure = { litros, valor ->
                viewModel.takeMeasurement(litros, valor)
            },
            onSelectSensor = { viewModel.selectSensor(it) },
            onDeleteMeasurement = { viewModel.eliminarUltimaMedicion() },
            onStarAnalise = { viewModel.startAnalise() },
            onSaveConfig = { capacidad, capacitancia ->
                viewModel.saveCapacidadAndCapacitancia(capacidad, capacitancia)
            }
        )

        when (uiState.value.calibrationEvent) {
            SenderCalibrationEvent.Calibrating -> {
                LoadingDialog(
                    title = (uiState.value.calibrationEvent as SenderCalibrationEvent.Calibrating).title,
                    message = "Espera un momento mientras procesamos la información",
                )
            }

            is SenderCalibrationEvent.Error -> {
                ErrorDialog(
                    message = (uiState.value.calibrationEvent as SenderCalibrationEvent.Error).message,
                    onDismiss = { viewModel.restarCalibrationState() },
                )
            }

            SenderCalibrationEvent.Loading -> {
                LoadingDialog(
                    title = (uiState.value.calibrationEvent as SenderCalibrationEvent.Loading).title,
                    message = "Espera un momento mientras procesamos la información",
                )
            }

            SenderCalibrationEvent.Success -> {
                SuccessDialog(
                    message = "El archivo de configuración se ha procesado y enviado correctamente",
                    onDismiss = { viewModel.restarCalibrationState() }
                )
            }

            SenderCalibrationEvent.Updating -> {
                LoadingDialog(
                    title = (uiState.value.calibrationEvent as SenderCalibrationEvent.Updating).title,
                    message = "Espera un momento mientras procesamos la información",
                )
            }

            else -> {}
        }
    }
}

@Composable
fun CalibrationScreen(
    uiState: CalibrationUiState,
    onTakeMeasure: (litros: String, valor: String) -> Unit,
    onSelectSensor: (CalibrationSensor) -> Unit,
    onDeleteMeasurement: () -> Unit,
    onStarAnalise: () -> Unit,
    onSaveConfig: (capacidad: String, capacitancia: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
            MaterialTheme.colorScheme.surface,
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
        )
    )

    var litros by remember { mutableStateOf("") }
    var valor by remember { mutableStateOf("") }
    var capacidad by remember { mutableStateOf("") }
    var capacitancia by remember { mutableStateOf("") }
    var userIsTryingToEdit by remember { mutableStateOf(false) }
    var userEditedManually by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.currentSensorValue) {
        if (!userIsTryingToEdit && !userEditedManually) {
            valor = uiState.currentSensorValue
        }
    }

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
                title = "Calibracion",
                subtitle = "",
                icon = Icons.Filled.Sensors,
            )

            SectionCard {
                SectionTitle(
                    title = "Sensores",
                    subtitle = "Selecciona el sensor a calibrar"
                )

                Spacer(modifier = Modifier.height(dimensionResource(R.dimen.medium_padding)))

                DropdownSelectSensor(
                    sensors = uiState.sensors,
                    sensor = uiState.selectedSensor,
                    onSelectSensor = { onSelectSensor(it) }
                )
            }

            SectionCard {
                SectionTitle(
                    title = "Datos de configuración",
                    subtitle = "Ingrese la capacidad del tanque y la capacitancia"
                )

                Spacer(modifier = Modifier.height(dimensionResource(R.dimen.medium_padding)))

                Row(horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.medium_padding))) {
                    OutlinedTextField(
                        enabled = uiState.selectedSensor != null,
                        value = capacitancia,
                        onValueChange = { newText ->
                            capacitancia = newText
                        },
                        label = { Text("Capacitancia") },
                        singleLine = true,
                        shape = RoundedCornerShape(dimensionResource(R.dimen.medium_padding)),
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal
                        )
                    )

                    OutlinedTextField(
                        enabled = uiState.selectedSensor != null,
                        value = capacidad,
                        onValueChange = { newText ->
                            litros = newText
                        },
                        label = { Text("Capacidad") },
                        singleLine = true,
                        shape = RoundedCornerShape(dimensionResource(R.dimen.medium_padding)),
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal
                        )
                    )
                }

                Spacer(modifier = Modifier.height(dimensionResource(R.dimen.medium_padding)))

                Button(
                    enabled = uiState.selectedSensor != null,
                    onClick = {
                        onSaveConfig(capacidad, capacitancia)
                        capacidad = ""
                        capacitancia = ""
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(18.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(text = "Guardar")
                }
            }

            SectionCard {
                SectionTitle(
                    title = "Tomar medida",
                    subtitle = "Tomar medición de combustible para realizar el  análisis"
                )

                Spacer(modifier = Modifier.height(dimensionResource(R.dimen.medium_padding)))

                Row(horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.medium_padding))) {
                    OutlinedTextField(
                        enabled = uiState.selectedSensor != null,
                        value = valor,
                        onValueChange = { newText ->
                            valor = newText
                            userEditedManually = true
                        },
                        label = { Text("Valor") },
                        singleLine = true,
                        shape = RoundedCornerShape(dimensionResource(R.dimen.medium_padding)),
                        modifier = Modifier
                            .onFocusChanged { focusState ->
                                userIsTryingToEdit = focusState.isFocused
                            }
                            .weight(1f),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal
                        )
                    )

                    OutlinedTextField(
                        enabled = uiState.selectedSensor != null,
                        value = litros,
                        onValueChange = { newText ->
                            litros = newText

                        },
                        label = { Text("Litros") },
                        singleLine = true,
                        shape = RoundedCornerShape(dimensionResource(R.dimen.medium_padding)),
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal
                        )
                    )
                }

                Spacer(modifier = Modifier.height(dimensionResource(R.dimen.medium_padding)))

                Button(
                    enabled = uiState.selectedSensor != null,
                    onClick = {
                        onTakeMeasure(litros, valor)
                        litros = ""
                        valor = ""
                        userIsTryingToEdit = false
                        userEditedManually = false
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(18.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(text = "Tomar medida")
                }
            }

            SectionCard {
                MeasurementsTable(
                    measurements = uiState.measurements,
                    onDeleteMeasurement = onDeleteMeasurement
                )
            }

            Button(
                onClick = onStarAnalise,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(18.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                enabled = uiState.measurements.isNotEmpty() && uiState.measurements.size >= 3
            ) {
                Text(text = "Iniciar análisis")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownSelectSensor(
    sensors: List<CalibrationSensor>,
    sensor: CalibrationSensor?,
    modifier: Modifier = Modifier,
    onSelectSensor: (CalibrationSensor) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = sensor?.value ?: "Seleccione un sensor",
            onValueChange = {},
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
            readOnly = true,
            singleLine = true,
            shape = RoundedCornerShape(18.dp),
            label = { Text("Sensor") },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = RoundedCornerShape(18.dp),
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            sensors.forEach { sensor ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = sensor.value
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Sensors,
                            contentDescription = null
                        )
                    },
                    onClick = {
                        expanded = false
                        onSelectSensor(sensor)
                    }
                )
            }
        }
    }
}

@Composable
@Preview(showBackground = true, showSystemUi = true)
fun CalibrationRoutePreview() {
    CalibrationScreen(
        uiState = CalibrationUiState(),
        onTakeMeasure = { _, _ -> },
        onSelectSensor = {},
        onDeleteMeasurement = {},
        onStarAnalise = {},
        onSaveConfig = { _, _ -> }
    )
}

@Composable
@Preview(showBackground = true, showSystemUi = true)
fun MeasurementsGridPreview() {
    FuelBoxControlTheme {
        MeasurementsTable(
            measurements = listOf(Pair("1", "2"), Pair("3", "4"), Pair("5", "6")),
            onDeleteMeasurement = {},
            modifier = Modifier.safeContentPadding()
        )
    }
}

@Composable
@Preview(showBackground = true)
fun DropdownMenuSensorPreview() {
    FuelBoxControlTheme {
        DropdownSelectSensor(
            sensors = listOf(
                CalibrationSensor.SENSOR_1,
                CalibrationSensor.SENSOR_2,
                CalibrationSensor.SENSOR_3,
                CalibrationSensor.SENSOR_4
            ),
            sensor = CalibrationSensor.SENSOR_1,
            onSelectSensor = {},
            modifier = Modifier.safeContentPadding()
        )
    }
}