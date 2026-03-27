package com.gasmonsoft.fuelboxcontrol.ui.calibracion.ui

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.OilBarrel
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.input.ImeAction
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
import com.gasmonsoft.fuelboxcontrol.ui.commons.InfoDialog
import com.gasmonsoft.fuelboxcontrol.ui.commons.InfoRow
import com.gasmonsoft.fuelboxcontrol.ui.commons.LoadingDialog
import com.gasmonsoft.fuelboxcontrol.ui.commons.ScreenHeaderCard
import com.gasmonsoft.fuelboxcontrol.ui.commons.SectionCard
import com.gasmonsoft.fuelboxcontrol.ui.commons.SectionTitle
import com.gasmonsoft.fuelboxcontrol.ui.commons.SuccessDialog
import com.gasmonsoft.fuelboxcontrol.ui.theme.FuelBoxControlTheme
import com.gasmonsoft.fuelboxcontrol.utils.sanitizeDecimalInput
import com.gasmonsoft.fuelboxcontrol.utils.toPositiveDoubleOrNull

@Composable
fun CalibracionRoute(
    idCaja: Int,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CalibrationViewModel = hiltViewModel()
) {
    val uiState = viewModel.calibrationUiState.collectAsState()
    var showWarningDialog by remember { mutableStateOf(false) }
    var pendingSensor by remember { mutableStateOf<CalibrationSensor?>(null) }

    BackHandler {
        onBack()
    }

    if (showWarningDialog) {
        InfoDialog(
            message = "Se perderán los datos actuales al cambiar de sensor",
            onDismiss = {
                showWarningDialog = false
                pendingSensor = null
            }
        ) {
            pendingSensor?.let {
                viewModel.selectSensor(it)
            }
            showWarningDialog = false
            pendingSensor = null
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CalibrationScreen(
            uiState = uiState.value,
            onTakeMeasure = { litros, valor ->
                viewModel.takeMeasurement(litros, valor)
            },
            onSelectSensor = { sensor ->
                if ((uiState.value.measurements.isNotEmpty() || uiState.value.capacidad != 0.0 ||
                            uiState.value.capacitancia != 0.0) &&
                    uiState.value.selectedSensor != sensor
                ) {
                    pendingSensor = sensor
                    showWarningDialog = true
                } else {
                    viewModel.selectSensor(sensor)
                }
            },
            onDeleteMeasurement = { viewModel.eliminarUltimaMedicion() },
            onStarAnalise = { viewModel.startAnalise(idCaja) },
            onSaveConfig = { capacidad, capacitancia ->
                viewModel.saveCapacidadAndCapacitancia(capacidad, capacitancia)
            },
            onBack = onBack,
            onClearTable = { viewModel.clearTable() }
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

            else -> Unit
        }
    }
}

@Composable
fun CalibrationScreen(
    uiState: CalibrationUiState,
    onTakeMeasure: (litros: String, valor: String) -> Unit,
    onSelectSensor: (CalibrationSensor) -> Unit,
    onDeleteMeasurement: () -> Unit,
    onClearTable: () -> Unit,
    onStarAnalise: () -> Unit,
    onSaveConfig: (capacidad: String, capacitancia: String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
            MaterialTheme.colorScheme.surface,
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
        )
    )

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    var medidaLitros by remember { mutableStateOf("") }
    var medidaValor by remember { mutableStateOf("") }
    var configCapacidad by remember { mutableStateOf("") }
    var configCapacitancia by remember { mutableStateOf("") }

    var userIsTryingToEditValor by remember { mutableStateOf(false) }
    var userEditedValorManually by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.currentSensorValue) {
        if (!userIsTryingToEditValor && !userEditedValorManually) {
            medidaValor = sanitizeDecimalInput(uiState.currentSensorValue)
        }
    }

    val capacidadValida = configCapacidad.toPositiveDoubleOrNull() != null
    val capacitanciaValida = configCapacitancia.toPositiveDoubleOrNull() != null
    val valorValido = medidaValor.toPositiveDoubleOrNull() != null
    val litrosValido = medidaLitros.toPositiveDoubleOrNull() != null

    val canSaveConfig = uiState.selectedSensor != null && capacidadValida && capacitanciaValida
    val canTakeMeasure = uiState.selectedSensor != null && valorValido && litrosValido
    val canStartAnalysis = uiState.measurements.size >= 3

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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Regresar"
                    )
                }
                ScreenHeaderCard(
                    title = "Calibración",
                    subtitle = "",
                    icon = Icons.Filled.Sensors,
                )
            }

            SectionCard {
                SectionTitle(
                    title = "Sensores",
                    subtitle = "Selecciona el sensor a calibrar"
                )

                Spacer(modifier = Modifier.height(dimensionResource(R.dimen.medium_padding)))

                DropdownSelectSensor(
                    sensors = uiState.sensors,
                    sensor = uiState.selectedSensor,
                    onSelectSensor = onSelectSensor
                )
            }

            SectionCard {
                SectionTitle(
                    title = "Datos de configuración",
                    subtitle = "Ingresa la capacidad del tanque y la capacitancia inicial"
                )

                Spacer(modifier = Modifier.height(dimensionResource(R.dimen.medium_padding)))
                Text(
                    text = "Configuraciones actuales",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = MaterialTheme.typography.titleMedium.fontWeight
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(dimensionResource(R.dimen.medium_padding)))
                Row {

                    InfoRow(
                        title = "Capacidad",
                        value = "${uiState.capacidad} L",
                        icon = Icons.Filled.OilBarrel,
                        modifier = Modifier.weight(1f)
                    )

                    InfoRow(
                        title = "Capacitancia",
                        value = "${uiState.capacitancia} F",
                        icon = Icons.Default.ElectricBolt,
                        modifier = Modifier.weight(1f)
                    )
                }


                Spacer(modifier = Modifier.height(dimensionResource(R.dimen.medium_padding)))

                Column(
                    verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.medium_padding))
                ) {
                    DecimalInputField(
                        enabled = uiState.selectedSensor != null,
                        value = configCapacitancia,
                        onValueChange = { configCapacitancia = it },
                        label = "Capacitancia",
                        supportingText = "Ingresa un número mayor a 0",
                        isError = configCapacitancia.isNotBlank() && !capacitanciaValida,
                        errorText = "Capacitancia inválida",
                        imeAction = ImeAction.Next,
                        onImeAction = { focusManager.moveFocus(FocusDirection.Down) }
                    )

                    DecimalInputField(
                        enabled = uiState.selectedSensor != null,
                        value = configCapacidad,
                        onValueChange = { configCapacidad = it },
                        label = "Capacidad del tanque",
                        suffix = "L",
                        supportingText = "Ingresa un número mayor a 0",
                        isError = configCapacidad.isNotBlank() && !capacidadValida,
                        errorText = "Capacidad inválida",
                        imeAction = ImeAction.Done,
                        onImeAction = {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                        }
                    )
                }

                Spacer(modifier = Modifier.height(dimensionResource(R.dimen.medium_padding)))

                Button(
                    enabled = canSaveConfig,
                    onClick = {
                        keyboardController?.hide()
                        focusManager.clearFocus()
                        onSaveConfig(configCapacidad, configCapacitancia)
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
                    subtitle = "Captura una medición de combustible para el análisis"
                )

                Spacer(modifier = Modifier.height(dimensionResource(R.dimen.medium_padding)))

                Column(
                    verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.medium_padding))
                ) {
                    DecimalInputField(
                        enabled = uiState.selectedSensor != null,
                        value = medidaValor,
                        onValueChange = {
                            medidaValor = it
                            userEditedValorManually = true
                        },
                        label = "Valor del sensor",
                        supportingText = if (!userEditedValorManually) {
                            "Se autocompleta con la lectura actual del sensor"
                        } else {
                            "Editado manualmente"
                        },
                        isError = medidaValor.isNotBlank() && !valorValido,
                        errorText = "Valor inválido",
                        imeAction = ImeAction.Next,
                        onImeAction = { focusManager.moveFocus(FocusDirection.Down) },
                        modifier = Modifier.onFocusChanged { focusState ->
                            userIsTryingToEditValor = focusState.isFocused
                        }
                    )

                    DecimalInputField(
                        enabled = uiState.selectedSensor != null,
                        value = medidaLitros,
                        onValueChange = { medidaLitros = it },
                        label = "Litros",
                        suffix = "L",
                        supportingText = "Ingresa un número mayor a 0",
                        isError = medidaLitros.isNotBlank() && !litrosValido,
                        errorText = "Litros inválidos",
                        imeAction = ImeAction.Done,
                        onImeAction = {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                        }
                    )
                }

                Spacer(modifier = Modifier.height(dimensionResource(R.dimen.medium_padding)))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        enabled = uiState.selectedSensor != null,
                        onClick = {
                            medidaValor = sanitizeDecimalInput(uiState.currentSensorValue)
                            userEditedValorManually = false
                            keyboardController?.hide()
                            focusManager.clearFocus()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Text("Usar lectura actual")
                    }

                    Button(
                        enabled = canTakeMeasure,
                        onClick = {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                            onTakeMeasure(medidaLitros, medidaValor)

                            medidaLitros = ""
                            medidaValor = sanitizeDecimalInput(uiState.currentSensorValue)
                            userIsTryingToEditValor = false
                            userEditedValorManually = false
                        },
                        modifier = Modifier
                            .weight(1f)
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
            }

            SectionCard {
                MeasurementsTable(
                    measurements = uiState.measurements,
                    onDeleteMeasurement = onDeleteMeasurement,
                    onClearAllTable = onClearTable
                )
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!canStartAnalysis) {
                    Text(
                        text = "Se requieren al menos 3 mediciones para iniciar el análisis",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
                    enabled = canStartAnalysis
                ) {
                    Text(text = "Iniciar análisis")
                }
            }
        }
    }
}

@Composable
fun DecimalInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    suffix: String? = null,
    supportingText: String? = null,
    isError: Boolean = false,
    errorText: String? = null,
    imeAction: ImeAction = ImeAction.Done,
    onImeAction: () -> Unit = {}
) {
    OutlinedTextField(
        enabled = enabled,
        value = value,
        onValueChange = { newText ->
            onValueChange(sanitizeDecimalInput(newText))
        },
        label = { Text(label) },
        singleLine = true,
        shape = RoundedCornerShape(dimensionResource(R.dimen.medium_padding)),
        modifier = modifier.fillMaxWidth(),
        isError = isError,
        suffix = suffix?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Decimal,
            imeAction = imeAction
        ),
        keyboardActions = KeyboardActions(
            onNext = { onImeAction() },
            onDone = { onImeAction() }
        ),
        supportingText = {
            when {
                isError && !errorText.isNullOrBlank() -> Text(errorText)
                !supportingText.isNullOrBlank() -> Text(supportingText)
            }
        }
    )
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
            sensors.forEach { item ->
                DropdownMenuItem(
                    text = {
                        Text(text = item.value)
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Sensors,
                            contentDescription = null
                        )
                    },
                    onClick = {
                        expanded = false
                        onSelectSensor(item)
                    }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CalibrationScreenPreview() {
    FuelBoxControlTheme {
        CalibrationScreen(
            uiState = CalibrationUiState(
                sensors = listOf(
                    CalibrationSensor.SENSOR_1,
                    CalibrationSensor.SENSOR_2,
                    CalibrationSensor.SENSOR_3,
                    CalibrationSensor.SENSOR_4
                ),
                selectedSensor = CalibrationSensor.SENSOR_1,
                measurements = listOf(
                    "10.0" to "150.0",
                    "20.0" to "300.0",
                    "30.0" to "450.0"
                ),
                currentSensorValue = "500.0",
                capacidad = 100.0,
                capacitancia = 12.5
            ),
            onTakeMeasure = { _, _ -> },
            onSelectSensor = {},
            onDeleteMeasurement = {},
            onStarAnalise = {},
            onSaveConfig = { _, _ -> },
            onBack = {},
            onClearTable = {}
        )
    }
}