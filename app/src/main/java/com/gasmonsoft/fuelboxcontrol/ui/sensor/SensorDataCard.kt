package com.gasmonsoft.fuelboxcontrol.ui.sensor

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gasmonsoft.fuelboxcontrol.data.ble.AccelerometerData
import com.gasmonsoft.fuelboxcontrol.data.ble.SensorData
import com.gasmonsoft.fuelboxcontrol.ui.theme.FuelBoxControlTheme

@Composable
fun SensorDataCard(numSensor: String, sensorData: SensorData?, modifier: Modifier = Modifier) {
    if (sensorData == null) return
    val localModifier = Modifier
        .fillMaxWidth()
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(width = 4.dp, color = MaterialTheme.colorScheme.primary)
    ) {
        Column(
            modifier = localModifier.padding(16.dp)
        ) {
            Row(
                modifier = localModifier,
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Sensor $numSensor",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.Black,
                )
                Column() {
                    Text(
                        "Actualizado",
                        color = Color.Black,
                        modifier = Modifier.align(Alignment.End)
                    )
                    Text(sensorData.date, color = Color.Black)
                }
            }

            HorizontalDivider(thickness = 4.dp, color = MaterialTheme.colorScheme.primary)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = localModifier.fillMaxWidth()
            ) {
                SensorDataRow(
                    title = "Volumen",
                    data = sensorData.volumen,
                    modifier = Modifier.weight(1f)
                )
                SensorDataRow(
                    title = "Calidad",
                    data = sensorData.calidad,
                    modifier = Modifier.weight(1f)
                )
                SensorDataRow(
                    title = "Temp.",
                    data = sensorData.temperatura,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun SingleSensorDataCard(accelerometerData: AccelerometerData?, modifier: Modifier = Modifier) {
    if (accelerometerData == null) return
    val localModifier = Modifier
        .fillMaxWidth()
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(width = 4.dp, color = MaterialTheme.colorScheme.primary)
    ) {
        Column(
            modifier = localModifier.padding(16.dp)
        ) {
            Row(
                modifier = localModifier,
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Acelerometro",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.Black,
                )
                Column() {
                    Text(
                        "Actualizado",
                        color = Color.Black,
                        modifier = Modifier.align(Alignment.End)
                    )
                    Text(accelerometerData.date, color = Color.Black)
                }
            }

            HorizontalDivider(thickness = 4.dp, color = MaterialTheme.colorScheme.primary)
            Column(
                modifier = modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = accelerometerData.value, color = Color.Black, style = MaterialTheme.typography.headlineMedium)
            }
        }

    }
}

@Composable
fun SensorDataRow(title: String, data: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "$title: ", color = Color.Black, style = MaterialTheme.typography.titleMedium)
        Text(text = data, color = Color.Black)
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