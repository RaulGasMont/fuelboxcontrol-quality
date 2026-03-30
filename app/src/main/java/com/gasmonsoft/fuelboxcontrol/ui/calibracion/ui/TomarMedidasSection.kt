package com.gasmonsoft.fuelboxcontrol.ui.calibracion.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gasmonsoft.fuelboxcontrol.R
import com.gasmonsoft.fuelboxcontrol.ui.calibracion.viewmodel.CalibrationSensor
import com.gasmonsoft.fuelboxcontrol.ui.calibracion.viewmodel.CalibrationUiState
import com.gasmonsoft.fuelboxcontrol.ui.commons.SectionCard
import com.gasmonsoft.fuelboxcontrol.ui.commons.SectionTitle
import com.gasmonsoft.fuelboxcontrol.ui.theme.FuelBoxControlTheme
import com.gasmonsoft.fuelboxcontrol.utils.sanitizeDecimalInput

@Composable
fun TomarMedidasSection(
    medidaValor: String,
    medidaLitros: String,
    valorValido: Boolean,
    litrosValido: Boolean,
    canTakeMeasure: Boolean,
    userEditedValorManually: Boolean,
    uiState: CalibrationUiState,
    onTakeMeasure: (litros: String, valor: String) -> Unit,
    onMedidaValorChange: (String) -> Unit,
    onMedidaLitrosChange: (String) -> Unit,
    onUserIsTryingToEditValorChange: (Boolean) -> Unit,
    onUserEditedValorManuallyChange: (Boolean) -> Unit,
    onLoadData: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    SectionCard {
        SectionTitle(
            title = "Tomar medida",
            subtitle = "Captura una medición de combustible para el análisis"
        )

        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.medium_padding)))

        Column(
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.medium_padding))
        ) {
            DecimalInputField(
                enabled = uiState.selectedSensor != null,
                value = medidaValor,
                onValueChange = {
                    onMedidaValorChange(it)
                    onUserEditedValorManuallyChange(false)
                },
                label = "Valor del sensor",
                supportingText = if (!userEditedValorManually) {
                    "Se autocompleta con la lectura actual del sensor"
                } else {
                    "Editado manualmente"
                },
                isError = medidaValor.isNotBlank() && !valorValido,
                errorText = "Valor inválido",
                imeAction = ImeAction.Next,
                onImeAction = { focusManager.moveFocus(FocusDirection.Down) },
                modifier = Modifier.onFocusChanged { focusState ->
                    onUserIsTryingToEditValorChange(focusState.isFocused)
                }
            )

            DecimalInputField(
                enabled = uiState.selectedSensor != null,
                value = medidaLitros,
                onValueChange = onMedidaLitrosChange,
                label = "Litros",
                suffix = "L",
                supportingText = "Ingresa un number mayor a 0",
                isError = medidaLitros.isNotBlank() && !litrosValido,
                errorText = "Litros inválidos",
                imeAction = ImeAction.Done,
                onImeAction = {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                }
            )
        }

        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.medium_padding)))

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    enabled = uiState.selectedSensor != null,
                    onClick = {
                        onMedidaValorChange(sanitizeDecimalInput(uiState.currentSensorValue))
                        onUserEditedValorManuallyChange(false)
                        keyboardController?.hide()
                        focusManager.clearFocus()
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Text("Usar lectura actual")
                }

                Button(
                    enabled = canTakeMeasure,
                    onClick = {
                        keyboardController?.hide()
                        focusManager.clearFocus()
                        onTakeMeasure(medidaLitros, medidaValor)

                        onMedidaLitrosChange("")
                        onMedidaValorChange(sanitizeDecimalInput(uiState.currentSensorValue))
                        onUserIsTryingToEditValorChange(false)
                        onUserEditedValorManuallyChange(false)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp),
                    shape = RoundedCornerShape(18.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(text = "Tomar medida")
                }
            }

            Button(
                onClick = onLoadData,
                shape = RoundedCornerShape(18.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Green.copy(blue = 0.3f, green = 0.7f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                enabled = uiState.selectedSensor != null
            ) {
                Text("Usar Excel")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TomarMedidasSectionPreview() {
    FuelBoxControlTheme {
        TomarMedidasSection(
            medidaValor = "1024.5",
            medidaLitros = "50.0",
            valorValido = true,
            litrosValido = true,
            canTakeMeasure = true,
            userEditedValorManually = false,
            uiState = CalibrationUiState(
                selectedSensor = CalibrationSensor.SENSOR_1,
                currentSensorValue = "1024.5"
            ),
            onTakeMeasure = { _, _ -> },
            onMedidaValorChange = {},
            onMedidaLitrosChange = {},
            onUserIsTryingToEditValorChange = {},
            onUserEditedValorManuallyChange = {},
            onLoadData = {}
        )
    }
}
