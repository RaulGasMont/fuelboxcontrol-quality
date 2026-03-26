package com.gasmonsoft.fuelboxcontrol.data.model.calibracion

import com.google.gson.annotations.SerializedName

fun Calibracion.toDto(): CalibrationDto {
    return CalibrationDto(
        tendencias = tendencias.map { tendencia ->
            TendenciasDto(
                limite = tendencia.initialValue,
                pendiente = tendencia.pendiente,
                ordenada = tendencia.intercepto
            )
        },
        capacidad = capacidad,
        capacitancia = capacitancia,
        intervalos = intervalos,
        coefPolinomial = coefPolinomial
    )
}

data class AnalisisRegresion(
    val coefPolinomial: String,
    val tendencias: List<Tendencia>,
)

data class Tendencia(
    val pendiente: Double,
    val intercepto: Double,
    val puntoInicial: Int,
    val puntoFinal: Int,
    val initialValue: Double,
    val sampleValue: Pair<Double, Double>,
)

data class Calibracion(
    val tendencias: List<Tendencia>,
    val capacidad: Double,
    val capacitancia: Int,
    val intervalos: Int,
    val coefPolinomial: String,
)

data class TendenciasDto(
    @SerializedName("limite") val limite: Double,
    @SerializedName("pendiente") val pendiente: Double,
    @SerializedName("ordenada") val ordenada: Double
)

data class CalibrationDto(
    @SerializedName("tendencias") val tendencias: List<TendenciasDto>,
    @SerializedName("capacidad") val capacidad: Double,
    @SerializedName("capacitancia") val capacitancia: Int,
    @SerializedName("intervalos") val intervalos: Int,
    @SerializedName("coef_polinomial") val coefPolinomial: String,
)