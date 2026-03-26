package com.gasmonsoft.fuelboxcontrol.model.calibracion

import com.google.gson.annotations.SerializedName

fun Calibration.toDto() = CalibrationDto(
    formula = formula
)

data class Calibration(
    val formula: String,
    val message: String,
)

data class Tendencia(
    val pendiente: Double,
    val intercepto: Double,
    val puntoInicial: Int,
    val puntoFinal: Int,
    val initialValue: Double,
    val sampleValue: Pair<Double, Double>,
)

data class CalibrationDto(
    @SerializedName("") val formula: String
)