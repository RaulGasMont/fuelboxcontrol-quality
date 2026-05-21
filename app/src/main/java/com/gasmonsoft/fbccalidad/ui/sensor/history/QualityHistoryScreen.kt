package com.gasmonsoft.fbccalidad.ui.sensor.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Thermostat
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.gasmonsoft.fbccalidad.domain.model.QualitySensorRecord
import com.gasmonsoft.fbccalidad.ui.commons.ErrorDialog
import com.gasmonsoft.fbccalidad.ui.commons.LoadingDialog
import com.gasmonsoft.fbccalidad.ui.commons.ScreenHeaderCard
import com.gasmonsoft.fbccalidad.ui.theme.FuelBoxControlTheme
import com.gasmonsoft.fbccalidad.utils.LoadState
import com.gasmonsoft.fbccalidad.utils.formatApiDateToUser

@Composable
fun QualityHistoryRoute(
    idCajaCalidad: Int,
    onBack: () -> Unit,
    viewModel: QualityHistoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(idCajaCalidad) {
        viewModel.loadHistory(idCajaCalidad)
    }

    QualityHistoryScreen(
        uiState = uiState,
        onBack = onBack,
        onRetry = { viewModel.loadHistory(idCajaCalidad) }
    )
}

@Composable
fun QualityHistoryScreen(
    uiState: QualityHistoryUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit
) {
    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
            MaterialTheme.colorScheme.background,
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f)
        )
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundBrush)
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                ScreenHeaderCard(
                    title = "Historial de Calidad",
                    subtitle = "Últimos registros recibidos del sensor",
                    icon = Icons.Outlined.History,
                    onBack = onBack,
                    action = {
                        IconButton(onClick = onRetry) {
                            Icon(
                                imageVector = Icons.Outlined.Refresh,
                                contentDescription = "Recargar",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                )

                if (uiState.records.isEmpty() && uiState.loadState is LoadState.Success) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No hay registros disponibles",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.records) { record ->
                            QualityRecordItem(record = record)
                        }
                    }
                }
            }

            when (uiState.loadState) {
                is LoadState.Loading -> {
                    LoadingDialog(message = "Cargando registros...")
                }

                is LoadState.Error -> {
                    ErrorDialog(
                        message = "Error al cargar el historial. Intente de nuevo.",
                        onDismiss = onBack
                    )
                }

                else -> Unit
            }
        }
    }
}

@Composable
fun QualityRecordItem(record: QualitySensorRecord) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.CalendarMonth,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = formatApiDateToUser(record.date),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                MetricItem(
                    icon = Icons.Outlined.WaterDrop,
                    label = "Calidad",
                    value = "${record.quality}",
                    modifier = Modifier.weight(1f)
                )

                MetricItem(
                    icon = Icons.Outlined.Thermostat,
                    label = "Temp.",
                    value = "${record.temperature}°C",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun MetricItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun QualityHistoryScreenPreview() {
    FuelBoxControlTheme {
        QualityHistoryScreen(
            uiState = QualityHistoryUiState(
                records = listOf(
                    QualitySensorRecord(
                        date = "2026-05-21T14:00:38.78",
                        quality = 85.5,
                        temperature = 25.0
                    ),
                    QualitySensorRecord(
                        date = "2026-05-21T15:00:12.45",
                        quality = 86.2,
                        temperature = 25.5
                    ),
                    QualitySensorRecord(
                        date = "2026-05-21T16:00:05.10",
                        quality = 84.8,
                        temperature = 26.0
                    )
                ),
                loadState = LoadState.Success
            ),
            onBack = {},
            onRetry = {}
        )
    }
}
