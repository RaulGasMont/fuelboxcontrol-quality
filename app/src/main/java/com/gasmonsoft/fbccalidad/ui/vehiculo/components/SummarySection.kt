package com.gasmonsoft.fbccalidad.ui.vehiculo.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Sensors
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.gasmonsoft.fbccalidad.domain.sensor.SensorPackage
import com.gasmonsoft.fbccalidad.ui.commons.InfoRow
import com.gasmonsoft.fbccalidad.ui.commons.SectionCard
import com.gasmonsoft.fbccalidad.ui.commons.SectionTitle
import com.gasmonsoft.fbccalidad.ui.sensor.components.AlertMessage
import com.gasmonsoft.fbccalidad.ui.vehiculo.viewmodel.SensorSendingEvent

@Composable
fun SendingSummarySection(
    sensorData: SensorPackage,
    dataSendingStatus: SensorSendingEvent,
    modifier: Modifier = Modifier
) {
    SectionCard(modifier = modifier) {
        SectionTitle(
            title = "Último envío de sensores",
            subtitle = "Revisa si el último envío se realizó correctamente."
        )

        Spacer(modifier = Modifier.height(18.dp))

        StatusPill(status = dataSendingStatus.title)

        if (dataSendingStatus is SensorSendingEvent.Error) {
            Spacer(modifier = Modifier.height(16.dp))
            AlertMessage(
                message = dataSendingStatus.message
            )
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
                    value = "Sensores",
                    icon = Icons.Rounded.Sensors
                )

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
                )

                InfoRow(
                    title = "Estado actual",
                    value = dataSendingStatus.title,
                    icon = Icons.Rounded.CloudDone
                )

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
                )

                InfoRow(
                    title = "Última trama correcta",
                    value = "${sensorData.date} ${sensorData.data}".trim()
                        .ifBlank { "Sin datos todavía" },
                    icon = Icons.AutoMirrored.Rounded.ReceiptLong
                )
            }
        }
    }
}

@Composable
fun TechnicalLogSection(
    logData: String,
    modifier: Modifier = Modifier
) {
    SectionCard(modifier = modifier) {
        SectionTitle(
            title = "Detalle técnico",
            subtitle = "Última trama recibida sin procesar."
        )

        Spacer(modifier = Modifier.height(18.dp))

        if (logData.isBlank()) {
            EmptyInfoState("Todavía no hay una trama técnica disponible.")
        } else {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                SelectionContainer {
                    Text(
                        text = logData.trim(),
                        modifier = Modifier.padding(14.dp),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = FontFamily.Monospace
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyInfoState(
    message: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}