package com.gasmonsoft.fuelboxcontrol.ui.calibracion.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun MeasurementsTable(
    measurements: List<Pair<String, String>>,
    modifier: Modifier = Modifier,
    onDeleteMeasurement: () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)

    Column(modifier = modifier) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = shape,
            tonalElevation = 3.dp,
            shadowElevation = 2.dp,
            border = BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant
            ),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 100.dp)
                    .padding(vertical = 4.dp)
            ) {
                // Table Header (formerly stickyHeader)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .padding(vertical = 6.dp)
                ) {
                    HeaderCell(
                        value = "Medición",
                        modifier = Modifier.weight(1f)
                    )
                    HeaderCell(
                        value = "Valor",
                        modifier = Modifier.weight(1f)
                    )
                    HeaderCell(
                        value = "Litros",
                        modifier = Modifier.weight(1f)
                    )
                }

                HorizontalDivider(
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                if (measurements.isNotEmpty()) {
                    measurements.forEachIndexed { index, item ->
                        val rowBackground =
                            if (index % 2 == 0) {
                                MaterialTheme.colorScheme.surface
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerLowest
                            }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(rowBackground),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            MeasurementCell(
                                value = (index + 1).toString(),
                                modifier = Modifier.weight(1f)
                            )
                            MeasurementCell(
                                value = item.first,
                                modifier = Modifier.weight(1f)
                            )
                            MeasurementCell(
                                value = item.second,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        if (index < measurements.lastIndex) {
                            HorizontalDivider(
                                thickness = 0.6.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp, horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Sin mediciones",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onDeleteMeasurement,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .height(54.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError
            ),
            enabled = measurements.isNotEmpty()
        ) {
            Text(
                text = "Eliminar medición",
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Composable
private fun HeaderCell(
    value: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = value,
        modifier = modifier
            .padding(horizontal = 12.dp, vertical = 10.dp),
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
fun MeasurementCell(
    value: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = value,
        modifier = modifier
            .padding(horizontal = 12.dp, vertical = 14.dp),
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface
    )
}