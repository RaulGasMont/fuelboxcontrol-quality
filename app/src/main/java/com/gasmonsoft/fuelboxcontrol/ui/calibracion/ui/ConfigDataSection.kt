package com.gasmonsoft.fuelboxcontrol.ui.calibracion.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.OilBarrel
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gasmonsoft.fuelboxcontrol.R
import com.gasmonsoft.fuelboxcontrol.ui.calibracion.viewmodel.CalibrationSensor
import com.gasmonsoft.fuelboxcontrol.ui.calibracion.viewmodel.CalibrationUiState
import com.gasmonsoft.fuelboxcontrol.ui.commons.InfoRow
import com.gasmonsoft.fuelboxcontrol.ui.commons.SectionCard
import com.gasmonsoft.fuelboxcontrol.ui.commons.SectionTitle
import com.gasmonsoft.fuelboxcontrol.ui.theme.FuelBoxControlTheme

@Composable
fun ConfigDataSection(
    configCapacitancia: String,
    configCapacidad: String,
    canSaveConfig: Boolean,
    capacidadValida: Boolean,
    capacitanciaValida: Boolean,
    uiState: CalibrationUiState,
    onSaveConfig: (capacidad: String, capacitancia: String) -> Unit,
    onCapacitanciaChange: (String) -> Unit,
    onCapacidadChange: (String) -> Unit
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    SectionCard {
        SectionTitle(
            title = "Datos de configuración",
            subtitle = "Ingresa la capacidad del tanque y la capacitancia inicial"
        )

        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.medium_padding)))
        Text(
            text = "Configuraciones actuales",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = MaterialTheme.typography.titleMedium.fontWeight
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.medium_padding)))
        Row {
            InfoRow(
                title = "Capacidad",
                value = "${uiState.capacidad} L",
                icon = Icons.Filled.OilBarrel,
                modifier = Modifier.weight(1f)
            )

            InfoRow(
                title = "Capacitancia",
                value = "${uiState.capacitancia} F",
                icon = Icons.Default.ElectricBolt,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.medium_padding)))

        Column(
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.medium_padding))
        ) {
            DecimalInputField(
                enabled = uiState.selectedSensor != null,
                value = configCapacitancia,
                onValueChange = onCapacitanciaChange,
                label = "Capacitancia",
                supportingText = "Ingresa un número mayor a 0",
                isError = configCapacitancia.isNotBlank() && !capacitanciaValida,
                errorText = "Capacitancia inválida",
                imeAction = ImeAction.Next,
                onImeAction = { focusManager.moveFocus(FocusDirection.Down) }
            )

            DecimalInputField(
                enabled = uiState.selectedSensor != null,
                value = configCapacidad,
                onValueChange = onCapacidadChange,
                label = "Capacidad del tanque",
                suffix = "L",
                supportingText = "Ingresa un número mayor a 0",
                isError = configCapacidad.isNotBlank() && !capacidadValida,
                errorText = "Capacidad inválida",
                imeAction = ImeAction.Done,
                onImeAction = {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                }
            )
        }

        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.medium_padding)))

        Button(
            enabled = canSaveConfig,
            onClick = {
                keyboardController?.hide()
                focusManager.clearFocus()
                onSaveConfig(configCapacidad, configCapacitancia)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(18.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text(text = "Guardar")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ConfigDataViewPreview() {
    FuelBoxControlTheme {
        ConfigDataSection(
            configCapacitancia = "123.45",
            configCapacidad = "500.0",
            canSaveConfig = true,
            capacidadValida = true,
            capacitanciaValida = true,
            uiState = CalibrationUiState(
                selectedSensor = CalibrationSensor.SENSOR_1,
                capacidad = 450.0,
                capacitancia = 110.0
            ),
            onSaveConfig = { _, _ -> },
            onCapacitanciaChange = {},
            onCapacidadChange = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ConfigDataViewDisabledPreview() {
    FuelBoxControlTheme {
        ConfigDataSection(
            configCapacitancia = "",
            configCapacidad = "",
            canSaveConfig = false,
            capacidadValida = false,
            capacitanciaValida = false,
            uiState = CalibrationUiState(
                selectedSensor = null,
                capacidad = 0.0,
                capacitancia = 0.0
            ),
            onSaveConfig = { _, _ -> },
            onCapacitanciaChange = {},
            onCapacidadChange = {}
        )
    }
}