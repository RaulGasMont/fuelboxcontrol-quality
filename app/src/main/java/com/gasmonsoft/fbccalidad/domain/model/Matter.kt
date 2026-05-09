package com.gasmonsoft.fbccalidad.domain.model

import androidx.compose.ui.graphics.Color

data class Matter(
    val id: Int,
    val ranges: List<QualityRange>
)

data class QualityRange(
    val label: String,
    val min: Double?,
    val max: Double?,
    val color: String,
    val status: String
) {
    companion object {
        val DESCONOCIDO = QualityRange(
            label = "desconocido",
            min = null,
            max =null,
            color = "0xFFD1D5DB",
            status = "desconocido"
        )
    }
}

fun String.toColor(): Color {
    return try {
        Color(java.lang.Long.decode(this))
    } catch (e: Exception) {
        Color(0xFFD1D5DB) // Gris por defecto
    }
}
