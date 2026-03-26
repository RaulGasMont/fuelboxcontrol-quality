package com.gasmonsoft.fuelboxcontrol.ui.vehiculo.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
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
    vehicles: List<VehicleInfo>,
    currentVehicle: VehicleInfo?,
    onSelectVehicle: (Int) -> Unit
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
    }

}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun VehicleSectionPreview() {
    FuelBoxControlTheme {
        VehicleSection(
            vehicles = emptyList(),
            currentVehicle = VehicleInfo(
                id = "",
                description = "",
                mac = ""
            ),
            onSelectVehicle = {}
        )
    }
}