package com.gasmonsoft.fuelboxcontrol.ui.sensor.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.LinkOff
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gasmonsoft.fuelboxcontrol.data.model.ble.ConnectionState
import com.gasmonsoft.fuelboxcontrol.ui.theme.FuelBoxControlTheme

@Composable
fun ConnectionStatusRow(
    state: ConnectionState,
    initializingMessage: String?,
    errorMessage: String?,
    onReconnect: () -> Unit,
    onInitialize: () -> Unit,
    onDisconnect: () -> Unit
) {
    val (label, containerColor, contentColor) = when (state) {
        ConnectionState.Connected -> Triple(
            "Conectado",
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer
        )

        ConnectionState.CurrentlyInitializing -> Triple(
            "Inicializando",
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer
        )

        ConnectionState.Disconnected -> Triple(
            "Desconectado",
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer
        )

        ConnectionState.Uninitialized -> Triple(
            "Sin inicializar",
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer
        )

    }

    val actionLabel = when (state) {
        ConnectionState.Connected -> "Cerrar conexión"
        ConnectionState.Disconnected -> "Volver a conectar"
        ConnectionState.Uninitialized -> "Iniciar conexión"
        ConnectionState.CurrentlyInitializing -> "Conectando..."
    }

    val actionEnabled = state != ConnectionState.CurrentlyInitializing

    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(999.dp),
            color = containerColor
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Bluetooth,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = contentColor
                )
            }
        }

        if (!initializingMessage.isNullOrBlank()) {
            Text(
                text = initializingMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (!errorMessage.isNullOrBlank()) {
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = {
                    when (state) {
                        ConnectionState.Connected -> onDisconnect()
                        ConnectionState.Disconnected -> onReconnect()
                        ConnectionState.Uninitialized -> onInitialize()
                        ConnectionState.CurrentlyInitializing -> Unit
                    }
                },
                enabled = actionEnabled,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp)
            ) {
                val icon = when (state) {
                    ConnectionState.Connected -> Icons.Rounded.LinkOff
                    ConnectionState.Disconnected -> Icons.Rounded.Refresh
                    ConnectionState.Uninitialized -> Icons.Rounded.PowerSettingsNew
                    ConnectionState.CurrentlyInitializing -> Icons.Rounded.Sync
                }

                Icon(
                    imageVector = icon,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(actionLabel)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ConnectionStatusRowPreview() {
    FuelBoxControlTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            ConnectionStatusRow(
                state = ConnectionState.Connected,
                initializingMessage = null,
                errorMessage = null,
                onReconnect = {},
                onInitialize = {},
                onDisconnect = {}
            )
            ConnectionStatusRow(
                state = ConnectionState.Disconnected,
                initializingMessage = null,
                errorMessage = "Error de conexión",
                onReconnect = {},
                onInitialize = {},
                onDisconnect = {}
            )
            ConnectionStatusRow(
                state = ConnectionState.CurrentlyInitializing,
                initializingMessage = "Conectando al sensor...",
                errorMessage = null,
                onReconnect = {},
                onInitialize = {},
                onDisconnect = {}
            )
            ConnectionStatusRow(
                state = ConnectionState.Uninitialized,
                initializingMessage = null,
                errorMessage = null,
                onReconnect = {},
                onInitialize = {},
                onDisconnect = {}
            )
        }
    }
}