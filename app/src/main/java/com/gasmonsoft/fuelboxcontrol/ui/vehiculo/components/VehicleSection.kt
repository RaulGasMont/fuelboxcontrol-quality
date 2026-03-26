package com.gasmonsoft.fuelboxcontrol.ui.vehiculo.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gasmonsoft.fuelboxcontrol.data.model.vehicle.VehicleInfo
import com.gasmonsoft.fuelboxcontrol.ui.commons.SectionCard
import com.gasmonsoft.fuelboxcontrol.ui.commons.SectionTitle
import com.gasmonsoft.fuelboxcontrol.ui.theme.FuelBoxControlTheme

@Composable
fun VehicleSection(
    vehicles: List<VehicleInfo>,
    currentVehicle: VehicleInfo?,
    isLogged: Boolean,
    onSelectVehicle: (Int) -> Unit
) {
    val isEnabled = isLogged && vehicles.isNotEmpty()

    val subtitle = when {
        !isLogged -> "Inicia sesión para consultar las unidades disponibles."
        vehicles.isEmpty() -> "No hay unidades disponibles para este usuario."
        else -> "Elige la unidad para cargar su información."
    }

    SectionCard {
        SectionTitle(
            title = "Unidad a consultar",
            subtitle = subtitle
        )

        Spacer(modifier = Modifier.height(18.dp))

        DropdownVehicleMenu(
            vehicles = vehicles,
            currentVehicle = currentVehicle,
            enabled = isEnabled,
            emptyMessage = when {
                !isLogged -> "Primero inicia sesión"
                vehicles.isEmpty() -> "No hay vehículos disponibles"
                else -> "Seleccione un vehículo"
            },
            onSelectVehicle = onSelectVehicle
        )

        if (currentVehicle != null && isEnabled) {
            Spacer(modifier = Modifier.height(14.dp))

            Surface(
                shape = RoundedCornerShape(999.dp),
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Text(
                    text = "Unidad actual: ${currentVehicle.description}",
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownVehicleMenu(
    vehicles: List<VehicleInfo>,
    currentVehicle: VehicleInfo?,
    enabled: Boolean,
    emptyMessage: String,
    modifier: Modifier = Modifier,
    onSelectVehicle: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val displayText = when {
        currentVehicle != null -> currentVehicle.description
        !enabled -> emptyMessage
        else -> "Seleccione un vehículo"
    }

    ExposedDropdownMenuBox(
        expanded = expanded && enabled,
        onExpandedChange = {
            if (enabled) expanded = !expanded
        },
        modifier = modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = displayText,
            onValueChange = {},
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
            readOnly = true,
            enabled = enabled,
            singleLine = true,
            shape = RoundedCornerShape(18.dp),
            label = { Text("Vehículo") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.DirectionsCar,
                    contentDescription = null
                )
            },
            trailingIcon = {
                if (enabled) {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                }
            }
        )

        ExposedDropdownMenu(
            expanded = expanded && enabled,
            onDismissRequest = { expanded = false },
            shape = RoundedCornerShape(18.dp),
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            vehicles.forEach { vehicle ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = vehicle.description,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.DirectionsCar,
                            contentDescription = null
                        )
                    },
                    onClick = {
                        expanded = false
                        onSelectVehicle(vehicle.id.toInt())
                    }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun VehicleSectionPreview() {
    val vehicles = listOf(
        VehicleInfo(id = "1", description = "Vehículo 1", mac = "00:11:22:33:44:55"),
        VehicleInfo(id = "2", description = "Vehículo 2", mac = "66:77:88:99:AA:BB"),
        VehicleInfo(id = "3", description = "Vehículo 3", mac = "CC:DD:EE:FF:00:11")
    )
    FuelBoxControlTheme {
        VehicleSection(
            vehicles = vehicles,
            currentVehicle = vehicles[0],
            isLogged = true,
            onSelectVehicle = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun VehicleSectionNotLoggedPreview() {
    FuelBoxControlTheme {
        VehicleSection(
            vehicles = emptyList(),
            currentVehicle = null,
            isLogged = false,
            onSelectVehicle = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun VehicleSectionNoVehiclesPreview() {
    FuelBoxControlTheme {
        VehicleSection(
            vehicles = emptyList(),
            currentVehicle = null,
            isLogged = true,
            onSelectVehicle = {}
        )
    }
}
