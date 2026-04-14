package com.gasmonsoft.fuelboxcontrol.ui.detector.viewmodel

import com.gasmonsoft.fuelboxcontrol.data.model.selectvehicle.TankType
import com.gasmonsoft.fuelboxcontrol.ui.detector.screen.FuelType

data class DetectorUiState(
    val tankType: TankType? = null,
    val tankId: Int = -1,
    val tankName: String = "",
    val fuelType: FuelType = FuelType.DESCONOCIDO,
    val level: Float = 0.0f
)