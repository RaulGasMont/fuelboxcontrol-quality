package com.gasmonsoft.fuelboxcontrol.ui.sensor.components

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LinkOff
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    val (label, containerColor, contentColor, indicatorColor) = when (state) {
        ConnectionState.Connected -> Quadruple(
            "Conectado",
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer,
            MaterialTheme.colorScheme.tertiary
        )

        ConnectionState.CurrentlyInitializing -> Quadruple(
            "Inicializando",
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer,
            MaterialTheme.colorScheme.secondary
        )

        ConnectionState.Disconnected -> Quadruple(
            "Desconectado",
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer,
            MaterialTheme.colorScheme.error
        )

        ConnectionState.Uninitialized -> Quadruple(
            "Sin inicializar",
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer,
            MaterialTheme.colorScheme.secondary
        )
    }

    val actionLabel = when (state) {
        ConnectionState.Connected -> "Cerrar conexión"
        ConnectionState.Disconnected -> "Volver a conectar"
        ConnectionState.Uninitialized -> "Iniciar conexión"
        ConnectionState.CurrentlyInitializing -> "Conectando..."
    }

    val actionIcon = when (state) {
        ConnectionState.Connected -> Icons.Rounded.LinkOff
        ConnectionState.Disconnected -> Icons.Rounded.Refresh
        ConnectionState.Uninitialized -> Icons.Rounded.PowerSettingsNew
        ConnectionState.CurrentlyInitializing -> Icons.Rounded.Sync
    }

    val actionEnabled = state != ConnectionState.CurrentlyInitializing

    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(containerColor),
                    contentAlignment = Alignment.Center
                ) {
                    if (state == ConnectionState.CurrentlyInitializing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.2.dp,
                            color = contentColor
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.Bluetooth,
                            contentDescription = null,
                            tint = contentColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Estado de conexión",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(indicatorColor)
                        )

                        Text(
                            text = label,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        if (!initializingMessage.isNullOrBlank()) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )

                    Text(
                        text = initializingMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }

        if (!errorMessage.isNullOrBlank()) {
            AlertMessage(message = errorMessage)
        }

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
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(18.dp)
        ) {
            Icon(
                imageVector = actionIcon,
                contentDescription = null
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = actionLabel,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
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