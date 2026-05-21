package com.gasmonsoft.fbccalidad.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

const val FORMAT_ISO_WITH_MS = "yyyy-MM-dd'T'HH:mm:ss.SSS"
const val FORMAT_USER_DATE_TIME = "dd/MM/yyyy HH:mm"

fun formatApiDateToUser(apiDate: String?): String {
    if (apiDate.isNullOrBlank()) return ""
    return try {
        // El formato de la API puede venir con o sin 'Z' al final, y con diferente precisión de ms
        // Intentamos limpiar o usar un formato flexible
        val cleanDate = apiDate.substringBefore('Z').substringBefore('+')
        val inputSdf = SimpleDateFormat(FORMAT_ISO_WITH_MS, Locale.getDefault())
        val date = inputSdf.parse(cleanDate)
        val outputSdf = SimpleDateFormat(FORMAT_USER_DATE_TIME, Locale.getDefault())
        date?.let { outputSdf.format(it) } ?: apiDate
    } catch (e: Exception) {
        apiDate
    }
}

fun sanitizeDecimalInput(input: String): String {
    val normalized = input.replace(',', '.')
    val filtered = buildString {
        var dotFound = false
        for (char in normalized) {
            when {
                char.isDigit() -> append(char)
                char == '.' && !dotFound -> {
                    append(char)
                    dotFound = true
                }
            }
        }
    }
    return filtered
}

fun String.toPositiveDoubleOrNull(): Double? {
    val value = this.toDoubleOrNull() ?: return null
    return if (value > 0) value else null
}


fun getCurrentDate(
    date: Date = Date(),
    pattern: String = FORMAT_ISO_WITH_MS
): String {
    val sdf = SimpleDateFormat(pattern, Locale.getDefault())
    return sdf.format(date)
}

interface LoadState {
    object Idle : LoadState
    object Loading : LoadState
    object Success: LoadState
    object Error: LoadState
}