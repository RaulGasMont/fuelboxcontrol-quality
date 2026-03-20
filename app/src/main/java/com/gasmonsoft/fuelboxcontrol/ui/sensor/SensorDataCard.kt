package com.gasmonsoft.fuelboxcontrol.ui.sensor

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.rounded.DataArray
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gasmonsoft.fuelboxcontrol.data.ble.AccelerometerData
import com.gasmonsoft.fuelboxcontrol.data.ble.SensorData
import com.gasmonsoft.fuelboxcontrol.ui.theme.FuelBoxControlTheme

val WarningAmber = Color(0xFFFFC107)

@Composable
fun SensorDataCard(
    numSensor: String,
    sensorData: SensorData?,
    modifier: Modifier = Modifier
) {
    if (sensorData == null) return

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (sensorData.error) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.10f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Sensor $numSensor",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Lecturas más recientes del sensor",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                UpdatedBadge(date = sensorData.date)
            }

            Spacer(modifier = Modifier.height(16.dp))

            HorizontalDivider(
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (sensorData.error) {
                    SensorErrorData(
                        title = "Datos erróneos",
                        data = sensorData.rawData,
                        modifier = Modifier.weight(1f),
                        icon = Icons.Rounded.DataArray
                    )
                } else {
                    SensorDataRow(
                        title = "Volumen",
                        data = sensorData.volumen,
                        modifier = Modifier.weight(1f),
                        icon = Icons.Rounded.WaterDrop
                    )

                    SensorDataRow(
                        title = "Calidad",
                        data = sensorData.calidad,
                        modifier = Modifier.weight(1f),
                        icon = Icons.Rounded.Verified
                    )

                    SensorDataRow(
                        title = "Temp.",
                        data = sensorData.temperatura,
                        modifier = Modifier.weight(1f),
                        icon = Icons.Rounded.Thermostat
                    )
                }
            }
        }
    }
}

@Composable
fun SingleSensorDataCard(
    accelerometerData: AccelerometerData?,
    modifier: Modifier = Modifier
) {
    if (accelerometerData == null) return

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.10f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Acelerómetro",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Lectura actual del acelerómetro",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                UpdatedBadge(date = accelerometerData.date)
            }

            Spacer(modifier = Modifier.height(16.dp))

            HorizontalDivider(
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
            )

            Spacer(modifier = Modifier.height(18.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Speed,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Text(
                        text = accelerometerData.value.ifBlank { "---" },
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun SensorErrorData(
    title: String,
    data: String,
    modifier: Modifier = Modifier,
    icon: ImageVector
) {

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.onErrorContainer
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onError,
                    modifier = Modifier.size(20.dp)
                )
            }

            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onError,
                textAlign = TextAlign.Center
            )

            Text(
                text = data.ifBlank { "Sin datos." },
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onError,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun SensorDataRow(
    title: String,
    data: String,
    modifier: Modifier = Modifier,
    icon: ImageVector
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Text(
                text = data.ifBlank { "---" },
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun UpdatedBadge(
    date: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.error
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = "Actualizado",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onError
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = date.ifBlank { "---" },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onError
            )
        }
    }
}

@Composable
fun SensorDataCaption(
    isAccelerometer: Boolean,
    isAllSensorData: Boolean,
    modifier: Modifier = Modifier
) {
    val message =
        if (isAccelerometer && !isAllSensorData) "Aun no hay datos de sensores. Solo Acelerómetro. Revise la conexión de los sensores."
        else if (!isAccelerometer && !isAllSensorData) "Aun no hay datos de la caja. Si después de un momento no hay datos revise la conexión física."
        else null

    if (message == null) return

    AlertMessage(
        message = message,
        modifier = modifier
    )
}

@Composable
fun AlertMessage(message: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.errorContainer
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = null,
                    tint = WarningAmber,
                    modifier = Modifier.size(32.dp)
                )
            }
            Text(
                text = message,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SensorDataCaptionPreview() {
    FuelBoxControlTheme {
        SensorDataCaption(
            isAccelerometer = true,
            isAllSensorData = false
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SensorDataCardPreview() {
    FuelBoxControlTheme {
        SensorDataCard(
            numSensor = "1",
            sensorData = SensorData(
                date = "2026/02/03 12:22:11",
                temperatura = "34",
                volumen = "172",
                calidad = "53"
            )
        )
    }
}


@Preview(showBackground = true)
@Composable
fun SensorErrorDataCardPreview() {
    FuelBoxControlTheme {
        SensorDataCard(
            numSensor = "1",
            sensorData = SensorData(
                error = true,
                rawData = "1234567890",
                date = "2026/02/03 12:22:11",
                temperatura = "34",
                volumen = "172",
                calidad = "53"
            )
        )
    }
}