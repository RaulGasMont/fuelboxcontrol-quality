package com.gasmonsoft.fbccalidad.ui.sensor.ui.calibrate

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.GasMeter
import androidx.compose.material.icons.filled.SettingsSuggest
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.gasmonsoft.fbccalidad.ui.sensor.viewmodel.calibrate.CalibrationUiState
import com.gasmonsoft.fbccalidad.ui.sensor.viewmodel.calibrate.CalibrationViewModel
import com.gasmonsoft.fbccalidad.ui.theme.FuelBoxControlTheme
import com.gasmonsoft.fbccalidad.utils.LoadState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalibrationDialog(
    modifier: Modifier = Modifier,
    onDismissRequest: () -> Unit,
    viewModel: CalibrationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    BasicAlertDialog(onDismissRequest = onDismissRequest) {
        Card(
            modifier = modifier
                .wrapContentHeight()
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            AnimatedContent(
                targetState = uiState,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "CalibrationStepAnimation"
            ) { state ->
                CalibrationContent(
                    state = state,
                    onDismissRequest = onDismissRequest,
                    onCalibrate = { viewModel.calibrate() },
                    onGetAirValue = { viewModel.getAirValue() },
                    onGetQualityValue = { viewModel.getQualityValue() },
                    onProCalibrate = { viewModel.proCalibrate() }
                )
            }
        }
    }
}

@Composable
fun CalibrationContent(
    state: CalibrationUiState,
    onDismissRequest: () -> Unit,
    onCalibrate: () -> Unit,
    onGetAirValue: () -> Unit,
    onGetQualityValue: () -> Unit,
    onProCalibrate: () -> Unit
) {
    val isLoading = state.initialCalibration is LoadState.Loading ||
            state.gettingAirValue is LoadState.Loading ||
            state.gettingQualityFuelValue is LoadState.Loading ||
            state.refineCalibration is LoadState.Loading

    Column(
        modifier = Modifier
            .padding(24.dp)
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when {
            state.refineCalibration is LoadState.Success -> {
                SuccessStep(onDismissRequest)
            }

            state.gettingQualityFuelValue is LoadState.Success -> {
                RefineStep(isLoading, state.refineCalibration is LoadState.Error, onProCalibrate)
            }

            state.gettingAirValue is LoadState.Success -> {
                QualityStep(
                    isLoading,
                    state.gettingQualityFuelValue is LoadState.Error,
                    onGetQualityValue
                )
            }

            state.initialCalibration is LoadState.Success -> {
                AirStep(isLoading, state.gettingAirValue is LoadState.Error, onGetAirValue)
            }

            else -> {
                InitialStep(
                    isLoading,
                    state.initialCalibration is LoadState.Error,
                    onCalibrate,
                    onDismissRequest
                )
            }
        }
    }
}

@Composable
private fun StepLayout(
    icon: ImageVector,
    title: String,
    description: String,
    isLoading: Boolean,
    isError: Boolean,
    primaryButtonText: String,
    onPrimaryClick: () -> Unit,
    secondaryButton: (@Composable () -> Unit)? = null
) {
    Icon(
        imageVector = if (isError) Icons.Default.ErrorOutline else icon,
        contentDescription = null,
        tint = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(48.dp)
    )

    Text(
        text = if (isError) "Ocurrió un error" else title,
        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
        textAlign = TextAlign.Center,
        color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    )

    Text(
        text = if (isError) "No se pudieron obtener los datos. Por favor, inténtelo de nuevo." else description,
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    HorizontalDivider(
        modifier = Modifier.padding(vertical = 8.dp),
        color = MaterialTheme.colorScheme.outlineVariant
    )

    if (isLoading) {
        Box(modifier = Modifier.height(80.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Spacer(Modifier.height(8.dp))
                Text("Procesando...", style = MaterialTheme.typography.labelSmall)
            }
        }
    } else {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onPrimaryClick,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(if (isError) "Reintentar" else primaryButtonText)
            }
            secondaryButton?.invoke()
        }
    }
}

@Composable
private fun InitialStep(
    isLoading: Boolean,
    isError: Boolean,
    onCalibrate: () -> Unit,
    onCancel: () -> Unit
) {
    StepLayout(
        icon = Icons.Default.SettingsSuggest,
        title = "Calibración Inicial",
        description = "Establece el punto de referencia base del sensor. Asegúrese de que el sensor esté limpio.",
        isLoading = isLoading,
        isError = isError,
        primaryButtonText = "Iniciar Calibración",
        onPrimaryClick = onCalibrate,
        secondaryButton = {
            TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
private fun AirStep(isLoading: Boolean, isError: Boolean, onGetAirValue: () -> Unit) {
    StepLayout(
        icon = Icons.Default.Air,
        title = "Paso 1: Valor de Aire",
        description = "Referencia en aire. Mantenga el sensor fuera de cualquier líquido.",
        isLoading = isLoading,
        isError = isError,
        primaryButtonText = "Medir Aire",
        onPrimaryClick = onGetAirValue
    )
}

@Composable
private fun QualityStep(isLoading: Boolean, isError: Boolean, onGetQualityValue: () -> Unit) {
    StepLayout(
        icon = Icons.Default.GasMeter,
        title = "Paso 2: Valor de Calidad",
        description = "Sumerja el sensor en el combustible de prueba para obtener la referencia.",
        isLoading = isLoading,
        isError = isError,
        primaryButtonText = "Medir Calidad",
        onPrimaryClick = onGetQualityValue
    )
}

@Composable
private fun RefineStep(isLoading: Boolean, isError: Boolean, onProCalibrate: () -> Unit) {
    StepLayout(
        icon = Icons.Default.AutoFixHigh,
        title = "Refinación Final",
        description = "Datos recolectados. Presione para aplicar la calibración de alta precisión.",
        isLoading = isLoading,
        isError = isError,
        primaryButtonText = "Finalizar y Aplicar",
        onPrimaryClick = onProCalibrate
    )
}

@Composable
private fun SuccessStep(onClose: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(64.dp)
        )
        Text(
            text = "¡Calibración Completada!",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "El sensor ha sido configurado con éxito. Ya puede cerrar este asistente.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        Button(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
            Text("Cerrar")
        }
    }
}

@Preview(showBackground = true, name = "Air Step")
@Composable
private fun AirStepPreview() {
    FuelBoxControlTheme {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AirStep(
                isLoading = false,
                isError = false,
                onGetAirValue = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CalibrationContentInitialPreview() {
    FuelBoxControlTheme {
        CalibrationContent(
            state = CalibrationUiState(),
            onDismissRequest = {},
            onCalibrate = {},
            onGetAirValue = {},
            onGetQualityValue = {},
            onProCalibrate = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun CalibrationContentLoadingPreview() {
    FuelBoxControlTheme {
        CalibrationContent(
            state = CalibrationUiState(initialCalibration = LoadState.Loading),
            onDismissRequest = {},
            onCalibrate = {},
            onGetAirValue = {},
            onGetQualityValue = {},
            onProCalibrate = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun CalibrationContentSuccessPreview() {
    FuelBoxControlTheme {
        CalibrationContent(
            state = CalibrationUiState(
                initialCalibration = LoadState.Success,
                gettingAirValue = LoadState.Success,
                gettingQualityFuelValue = LoadState.Success,
                refineCalibration = LoadState.Success
            ),
            onDismissRequest = {},
            onCalibrate = {},
            onGetAirValue = {},
            onGetQualityValue = {},
            onProCalibrate = {}
        )
    }
}
