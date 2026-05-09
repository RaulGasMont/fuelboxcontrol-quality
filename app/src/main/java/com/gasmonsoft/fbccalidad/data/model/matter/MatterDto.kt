package com.gasmonsoft.fbccalidad.data.model.matter

import com.google.gson.annotations.SerializedName

data class MatterResponseDto(
    @SerializedName("id_sustancia")
    val idSustancia: Int,
    @SerializedName("fld_json")
    val fldJson: String
)

data class MatterJsonDto(
    @SerializedName("quality_ranges")
    val qualityRanges: List<QualityRangeDto>
)

data class QualityRangeDto(
    @SerializedName("nombre_sustancia")
    val nombreSustancia: String,
    @SerializedName("valor_min")
    val valorMin: Double?,
    @SerializedName("valor_max")
    val valorMax: Double?,
    @SerializedName("color_hex")
    val colorHex: String,
    @SerializedName("texto_estado")
    val textoEstado: String
)
