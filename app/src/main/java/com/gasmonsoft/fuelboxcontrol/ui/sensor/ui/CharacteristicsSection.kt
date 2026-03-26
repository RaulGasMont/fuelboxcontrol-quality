package com.gasmonsoft.fuelboxcontrol.ui.sensor.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Battery6Bar
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun CharacteristicsSection(
    bateria: String,
    sensorMessage: String
) {
    SensorSectionCard {
        SectionHeader(
            title = "Información",
            subtitle = "Resumen actual de batería y mensaje del sensor"
        )

        Spacer(modifier = Modifier.height(18.dp))

        InfoLine(
            title = "Comandos",
            value = bateria.ifBlank { "Sin información disponible" },
            icon = Icons.Rounded.Keyboard
        )

        Spacer(modifier = Modifier.height(12.dp))

        InfoLine(
            title = "Mensaje",
            value = sensorMessage.ifBlank { "Sin mensaje disponible" },
            icon = Icons.Rounded.Info
        )
    }
}