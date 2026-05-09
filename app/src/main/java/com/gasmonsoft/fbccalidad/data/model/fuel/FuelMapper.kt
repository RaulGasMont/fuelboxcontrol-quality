package com.gasmonsoft.fbccalidad.data.model.fuel

import com.gasmonsoft.fbccalidad.ui.detector.screen.FuelType
import com.google.gson.annotations.SerializedName

data class MatterType(
    val id: Int,
    val type: FuelType,
    val minValue: Double,
    val maxValueL: Double
)

data class MatterTypeDto(
    @SerializedName("id") val id: Int,
    @SerializedName("type") val name: String,
    @SerializedName("minValue") val minValue: Double,
    @SerializedName("maxValue") val maxValue: Double
)

fun MatterTypeDto.toMatterType(): MatterType {
    val type = FuelType.entries.find { it.label == name }
    return MatterType(
        id = id,
        type = type ?: FuelType.DESCONOCIDO,
        minValue = minValue,
        maxValueL = maxValue
    )
}