package com.gasmonsoft.fuelboxcontrol.model.calibracion

import com.google.gson.annotations.SerializedName

fun Calibration.toDto() = CalibrationDto(
    formula = formula
)

data class Calibration(
    val formula: String
)

data class CalibrationDto(
    @SerializedName("") val formula: String
)