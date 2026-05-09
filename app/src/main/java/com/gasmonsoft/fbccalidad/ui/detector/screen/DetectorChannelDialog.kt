package com.gasmonsoft.fbccalidad.ui.detector.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gasmonsoft.fbccalidad.ui.detector.viewmodel.DetectorChannelType
import com.gasmonsoft.fbccalidad.ui.theme.FuelBoxControlTheme

@Composable
fun DetectorChannelDialog(
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit,
    onChannelSelected: (channel: DetectorChannelType) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        modifier = modifier,
        text = {
            DetectorChannelContent(
                onChannelSelected = onChannelSelected
            )
        }
    )
}

@Composable
fun DetectorChannelContent(
    modifier: Modifier = Modifier,
    onChannelSelected: (channel: DetectorChannelType) -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.error.copy(alpha = 0.18f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 14.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Text(
                text = "Medio de analisis",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold
                )
            )
            Text(
                text = "Seleccione el medio de analisis para realizar la deteccion"
            )

            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                onClick = { onChannelSelected(DetectorChannelType.BLE) },
                shape = RoundedCornerShape(18.dp)
            ) {
                Text(
                    text = "Bluetooth",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                onClick = { onChannelSelected(DetectorChannelType.USB) },
                shape = RoundedCornerShape(18.dp)
            ) {
                Text(
                    text = "USB",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DetectorChannelContentPreview() {
    FuelBoxControlTheme {
        DetectorChannelContent(
            onChannelSelected = {}
        )
    }
}