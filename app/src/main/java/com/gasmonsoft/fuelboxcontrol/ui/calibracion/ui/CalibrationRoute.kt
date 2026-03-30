package com.gasmonsoft.fuelboxcontrol.ui.calibracion.ui

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gasmonsoft.fuelboxcontrol.R
import com.gasmonsoft.fuelboxcontrol.ui.calibracion.viewmodel.CalibrationSensor
import com.gasmonsoft.fuelboxcontrol.ui.calibracion.viewmodel.CalibrationUiState
import com.gasmonsoft.fuelboxcontrol.ui.calibracion.viewmodel.CalibrationViewModel
import com.gasmonsoft.fuelboxcontrol.ui.calibracion.viewmodel.GeneralEvent
import com.gasmonsoft.fuelboxcontrol.ui.calibracion.viewmodel.SenderCalibrationEvent
import com.gasmonsoft.fuelboxcontrol.ui.commons.ErrorDialog
import com.gasmonsoft.fuelboxcontrol.ui.commons.InfoDialog
import com.gasmonsoft.fuelboxcontrol.ui.commons.LoadingDialog
import com.gasmonsoft.fuelboxcontrol.ui.commons.ScreenHeaderCard
import com.gasmonsoft.fuelboxcontrol.ui.commons.SectionCard
import com.gasmonsoft.fuelboxcontrol.ui.commons.SectionTitle
import com.gasmonsoft.fuelboxcontrol.ui.commons.SuccessDialog
import com.gasmonsoft.fuelboxcontrol.ui.sensor.components.ConnectionStatusRow
import com.gasmonsoft.fuelboxcontrol.ui.sensor.components.SectionHeader
import com.gasmonsoft.fuelboxcontrol.ui.sensor.components.SensorSectionCard
import com.gasmonsoft.fuelboxcontrol.ui.theme.FuelBoxControlTheme
import com.gasmonsoft.fuelboxcontrol.utils.sanitizeDecimalInput
import com.gasmonsoft.fuelboxcontrol.utils.toPositiveDoubleOrNull
import kotlinx.coroutines.launch

@Composable
fun CalibracionRoute(
    idCaja: Int,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CalibrationViewModel = hiltViewModel()
) {
    val uiState = viewModel.calibrationUiState.collectAsStateWithLifecycle()
    var showWarningDialog by remember { mutableStateOf(false) }
    var pendingSensor by remember { mutableStateOf<CalibrationSensor?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    BackHandler {
        onBack()
    }

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri: Uri? ->
        if (uri == null) {
            Toast.makeText(context, "Descarga cancelada", Toast.LENGTH_SHORT).show()
            return@rememberLauncherForActivityResult
        }

        scope.launch {
            try {
                viewModel.guardarByteArrayEnUri(
                    uri = uri,
                    data = uiState.value.datFile ?: byteArrayOf()
                )

                Toast.makeText(context, "Archivo descargado correctamente", Toast.LENGTH_SHORT)
                    .show()
            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    "Error al descargar: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    val excelPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importExcelMeasurements(context, uri)
        }
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
                    uiState.value.selectedSensor != sensor &&
                    uiState.value.datFile != null
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
            onClearTable = { viewModel.clearTable() },
            onReconnect = {},
            onInitializeConnection = {},
            onDisconnect = {},
            onDownloadDat = {
                val nombreArchivo =
                    "archivoDat_${uiState.value.selectedSensor?.id}_${System.currentTimeMillis()}.txt"
                createDocumentLauncher.launch(nombreArchivo)
            },
            onLoadData = {
                excelPicker.launch(
                    arrayOf(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                        "application/vnd.ms-excel"
                    )
                )
            },
            byteArray = uiState.value.datFile
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

        when (uiState.value.excelReadingEvent) {
            is GeneralEvent.Error -> {
                ErrorDialog(
                    message = (uiState.value.excelReadingEvent as GeneralEvent.Error).message,
                    onDismiss = { viewModel.restarExcelReadingState() },
                )
            }

            GeneralEvent.Loading -> {
                LoadingDialog(
                    title = (uiState.value.excelReadingEvent as GeneralEvent.Loading).title,
                    message = "Espera un momento mientras procesamos la información",
                )
            }

            GeneralEvent.Success -> {
                LaunchedEffect(Unit) {
                    Toast.makeText(context, "Datos importados correctamente", Toast.LENGTH_SHORT)
                        .show()
                    viewModel.restarExcelReadingState()
                }
            }

            else -> Unit
        }

    }
}

@Composable
fun CalibrationScreen(
    byteArray: ByteArray?,
    uiState: CalibrationUiState,
    onTakeMeasure: (litros: String, valor: String) -> Unit,
    onSelectSensor: (CalibrationSensor) -> Unit,
    onDeleteMeasurement: () -> Unit,
    onClearTable: () -> Unit,
    onStarAnalise: () -> Unit,
    onDownloadDat: () -> Unit,
    onSaveConfig: (capacidad: String, capacitancia: String) -> Unit,
    onBack: () -> Unit,
    onReconnect: () -> Unit,
    onInitializeConnection: () -> Unit,
    onDisconnect: () -> Unit,
    onLoadData: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
            MaterialTheme.colorScheme.surface,
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
        )
    )

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
    val canStartAnalysis = uiState.measurements.size >= 3 &&
            uiState.selectedSensor != null &&
            capacidadValida && capacitanciaValida


    if (uiState.calibrationEvent == SenderCalibrationEvent.Success) {
        configCapacidad = ""
        configCapacitancia = ""
        medidaLitros = ""
        medidaValor = ""
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

            SectionCard {
                SectionTitle(
                    title = "Sensores",
                    subtitle = "Selecciona el sensor a calibrar"
                )

                Spacer(modifier = Modifier.height(dimensionResource(R.dimen.medium_padding)))

                DropdownSelectSensor(
                    sensors = uiState.sensors,
                    sensor = uiState.selectedSensor,
                    onSelectSensor = {
                        configCapacidad = ""
                        configCapacitancia = ""
                        medidaLitros = ""
                        medidaValor = ""
                        onSelectSensor(it)
                    }
                )
            }

            ConfigDataSection(
                configCapacitancia = configCapacitancia,
                configCapacidad = configCapacidad,
                canSaveConfig = canSaveConfig,
                capacidadValida = capacidadValida,
                capacitanciaValida = capacitanciaValida,
                uiState = uiState,
                onSaveConfig = onSaveConfig,
                onCapacitanciaChange = { configCapacitancia = it },
                onCapacidadChange = { configCapacidad = it }
            )

            TomarMedidasSection(
                medidaValor = medidaValor,
                medidaLitros = medidaLitros,
                valorValido = valorValido,
                litrosValido = litrosValido,
                canTakeMeasure = canTakeMeasure,
                userEditedValorManually = userEditedValorManually,
                uiState = uiState,
                onTakeMeasure = onTakeMeasure,
                onMedidaValorChange = { medidaValor = it },
                onMedidaLitrosChange = { medidaLitros = it },
                onUserIsTryingToEditValorChange = { userIsTryingToEditValor = it },
                onUserEditedValorManuallyChange = { userEditedValorManually = it },
                onLoadData = onLoadData
            )

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
                    onClick = onDownloadDat,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(18.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    enabled = byteArray != null
                ) {
                    Text(text = "Descargar DAT")
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
            onClearTable = {},
            onReconnect = {},
            onInitializeConnection = {},
            onDisconnect = {},
            onDownloadDat = {},
            onLoadData = {},
            byteArray = null
        )
    }
}
