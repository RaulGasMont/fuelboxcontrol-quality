package com.gasmonsoft.fuelboxcontrol.ui.detector.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GasMeter
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gasmonsoft.fuelboxcontrol.ui.commons.ErrorDialog
import com.gasmonsoft.fuelboxcontrol.ui.commons.LoadingDialog
import com.gasmonsoft.fuelboxcontrol.ui.commons.SectionCard
import com.gasmonsoft.fuelboxcontrol.ui.detector.viewmodel.DetectorUiState
import com.gasmonsoft.fuelboxcontrol.ui.detector.viewmodel.DetectorViewModel
import com.gasmonsoft.fuelboxcontrol.ui.sensor.components.InfoLine
import com.gasmonsoft.fuelboxcontrol.ui.theme.FuelBoxControlTheme
import com.gasmonsoft.fuelboxcontrol.utils.ProcessingEvent

@Composable
fun DetectorRoute(
    modifier: Modifier = Modifier,
    onSelectTank: () -> Unit,
    viewModel: DetectorViewModel = hiltViewModel()
) {
    val uiState = viewModel.uiState.collectAsState()

    DetectorScreen(
        uiState = uiState.value,
        modifier = modifier,
        onSelectTank = onSelectTank,
        onAnalyze = { viewModel.analyzeData() },
        onDismissDetection = { viewModel.updateDetectionEvent(ProcessingEvent.Idle) }
    )
}

@Composable
fun DetectorScreen(
    uiState: DetectorUiState,
    modifier: Modifier = Modifier,
    onSelectTank: () -> Unit,
    onAnalyze: () -> Unit,
    onDismissDetection: () -> Unit
) {
    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
            MaterialTheme.colorScheme.surface,
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
        )
    )

    when (uiState.detectionEvent) {
        is ProcessingEvent.Loading -> {
            LoadingDialog(
                title = "Analizando",
                message = "Por favor espere mientras se analizan los datos..."
            )
        }

        is ProcessingEvent.Success -> {
            onDismissDetection()
        }

        is ProcessingEvent.Error -> {
            ErrorDialog(
                message = "Ocurrió un error al analizar los datos. Intente de nuevo.",
                onDismiss = onDismissDetection
            )
        }

        else -> {}
    }

    Surface(
        modifier = Modifier
            .background(backgroundBrush)
    ) {
        Box(contentAlignment = Alignment.BottomEnd) {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                SectionCard {
                    Button(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp),
                        onClick = onSelectTank,
                        shape = MaterialTheme.shapes.large
                    ) {
                        Text(text = "Seleccionar tanque")
                    }

                    if (uiState.tankId != -1) {
                        HorizontalDivider(thickness = 4.dp, modifier = Modifier.padding(8.dp))

                        InfoLine(
                            title = "Unidad Seleccionada",
                            value = uiState.tankName,
                            icon = Icons.Default.GasMeter
                        )
                    }
                }

                SectionCard(modifier = Modifier.height(340.dp)) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        AnimatedFuelTank(
                            fuelType = uiState.fuelType,
                            level = uiState.level,
                            modifier = Modifier.size(240.dp, 400.dp)
                        )
                    }
                }
            }

            Button(
                onClick = onAnalyze,
                enabled = uiState.tankId != -1,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(52.dp),
                elevation = ButtonDefaults.buttonElevation(4.dp),
                shape = MaterialTheme.shapes.large,
                colors = ButtonDefaults.buttonColors()
            ) {
                Text("Analizar")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DetectorScreenPreview() {
    FuelBoxControlTheme {
        DetectorScreen(
            uiState = DetectorUiState(
                tankId = 1,
                tankName = "Tanque Diesel 01",
                fuelType = FuelType.DIESEL,
                level = 0.75f
            ),
            onSelectTank = {},
            onAnalyze = {},
            onDismissDetection = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun DetectorScreenEmptyPreview() {
    FuelBoxControlTheme {
        DetectorScreen(
            uiState = DetectorUiState(),
            onSelectTank = {},
            onAnalyze = {},
            onDismissDetection = {}
        )
    }
}