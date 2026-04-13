package com.gasmonsoft.fuelboxcontrol.ui.selecttank.viewmodel

import com.gasmonsoft.fuelboxcontrol.data.model.selectvehicle.Tank
import com.gasmonsoft.fuelboxcontrol.utils.ProcessingEvent

data class SelectTankUiState(
    val search: String = "",
    val selectedContainer: Tank? = null,
    val screen: SelectVehicleScreen = SelectVehicleScreen.Vehicles(),
    val vehicles: List<Tank> = emptyList(),
    val otros: List<Tank> = emptyList(),
    val seeTankEvent: ProcessingEvent = ProcessingEvent.Idle,
    val saveTankEvent: ProcessingEvent = ProcessingEvent.Idle
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
