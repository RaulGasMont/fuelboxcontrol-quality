package com.gasmonsoft.fuelboxcontrol.ui.detector.viewmodel

import com.gasmonsoft.fuelboxcontrol.ui.detector.ui.FuelType

data class DetectorUiState(
    val station: String = "",
    val fuelType: FuelType = FuelType.UNKNOWN,
    val level: Float = 0.0f
)

data class FuelTank(
    val name: String,
    val id: Int,
)