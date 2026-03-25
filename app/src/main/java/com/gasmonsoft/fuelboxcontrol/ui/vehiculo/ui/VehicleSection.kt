package com.gasmonsoft.fuelboxcontrol.ui.vehiculo.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gasmonsoft.fuelboxcontrol.R
import com.gasmonsoft.fuelboxcontrol.model.vehicle.VehicleInfo
import com.gasmonsoft.fuelboxcontrol.ui.commons.SectionCard
import com.gasmonsoft.fuelboxcontrol.ui.commons.SectionTitle
import com.gasmonsoft.fuelboxcontrol.ui.theme.FuelBoxControlTheme

@Composable
fun VehicleSection(
    idCaja: Int?,
    vehicles: List<VehicleInfo>,
    currentVehicle: VehicleInfo?,
    onSelectVehicle: (Int) -> Unit,
    onCalibrate: (idCaja: Int) -> Unit
) {
    val context = LocalContext.current
    SectionCard {
        SectionTitle(
            title = stringResource(R.string.vehiculo),
            subtitle = "Selecciona la unidad que deseas consultar"
        )

        Spacer(modifier = Modifier.height(18.dp))

        DropdownVehicleMenu(
            vehicles = vehicles,
            currentVehicle = currentVehicle
        ) {
            onSelectVehicle(it)
        }

        Spacer(modifier = Modifier.height(18.dp))

        if (currentVehicle != null) {
            Button(
                onClick = {
                    if (idCaja != null) {
                        onCalibrate(idCaja)
                    } else {
                        Toast.makeText(
                            context,
                            "No se encontro el Id de la caja de comunicaciones.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
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
                Text("Calibrar")
            }
        }
    }

}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun VehicleSectionPreview() {
    FuelBoxControlTheme {
        VehicleSection(
            idCaja = 0,
            vehicles = emptyList(),
            currentVehicle = VehicleInfo(
                id = "",
                description = "",
                mac = ""
            ),
            onSelectVehicle = {},
            onCalibrate = { }
        )
    }
}