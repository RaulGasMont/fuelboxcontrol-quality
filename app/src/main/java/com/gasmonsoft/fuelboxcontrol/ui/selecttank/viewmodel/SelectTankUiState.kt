package com.gasmonsoft.fuelboxcontrol.ui.selecttank.viewmodel

import com.gasmonsoft.fuelboxcontrol.data.model.selectvehicle.Other
import com.gasmonsoft.fuelboxcontrol.data.model.selectvehicle.Vehicle

data class SelectTankUiState(
    val search: String = "",
    val screen: SelectVehicleScreen = SelectVehicleScreen.Vehicles(),
    val vehicles: List<Vehicle> = emptyList(),
    val otros: List<Other> = emptyList()
)

interface SelectTankScreen {
    val name: String
    val index: Int
}

sealed class SelectVehicleScreen(override val name: String, override val index: Int) :
    SelectTankScreen {
    data class Vehicles(override val name: String = "Vehículos", override val index: Int = 0) :
        SelectVehicleScreen(name, index)

    data class Otros(override val name: String = "Otros", override val index: Int = 1) :
        SelectVehicleScreen(name, index)
}
