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
            label = "Desconocido",
            min = null,
            max = null,
            color = "0xFF9CA3AF",
            status = "Desconocido"
        )
        val AIRE = QualityRange(
            label = "Aire",
            min = 0.0,
            max = 1.9,
            color = "0xFFBEE3F8",
            status = "Vacio"
        )
        val DIESEL = QualityRange(
            label = "Diesel",
            min = 2.0,
            max = 2.8,
            color = "0xFFFBBF24",
            status = "Normal"
        )
        val ACEITE = QualityRange(
            label = "Aceite",
            min = null,
            max = null,
            color = "0xFF4B5563",
            status = "Aceite"
        )
        val ALCOHOL = QualityRange(
            label = "Alcohol",
            min = 6.0,
            max = 18.0,
            color = "0xFFF9A8D4",
            status = "Alerta"
        )
        val AGUA = QualityRange(
            label = "Agua",
            min = 20.0,
            max = null,
            color = "0xFF60A5FA",
            status = "Peligro"
        )
        val ADULTERADO = QualityRange(
            label = "Adulterado",
            min = null,
            max = 0.0,
            color = "0xFFEF4444",
            status = "Critico"
        )

        val entries = listOf(
            DESCONOCIDO, AIRE, DIESEL, ACEITE, ALCOHOL, AGUA, ADULTERADO
        )
    }
}

fun String.toColor(): Color {
    return try {
        val hex = if (startsWith("#")) {
            replace("#", "0xFF")
        } else if (startsWith("0x")) {
            replace("0x", "0xFF")
        } else {
            "0xFF$this"
        }
        Color(java.lang.Long.decode(hex))
    } catch (e: Exception) {
        Color(0xFFD1D5DB)
    }
}
