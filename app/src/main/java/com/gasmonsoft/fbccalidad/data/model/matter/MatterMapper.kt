package com.gasmonsoft.fbccalidad.data.model.matter

import com.gasmonsoft.fbccalidad.domain.model.Matter
import com.gasmonsoft.fbccalidad.domain.model.QualityRange
import com.google.gson.Gson

fun MatterResponseDto.toDomain(gson: Gson): Matter {
    val jsonDto = try {
        gson.fromJson(fldJson, MatterJsonDto::class.java)
    } catch (e: Exception) {
        MatterJsonDto(emptyList())
    }

    return Matter(
        id = idSustancia,
        ranges = jsonDto.qualityRanges.map { it.toDomain() }
    )
}

fun QualityRangeDto.toDomain(): QualityRange =
    QualityRange(
        label = nombreSustancia,
        min = valorMin,
        max = valorMax,
        color = colorHex,
        status = textoEstado
    )
