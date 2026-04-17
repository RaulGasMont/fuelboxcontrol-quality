package com.gasmonsoft.fbccalidad.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

const val FORMAT_ISO_WITH_MS = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"

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

interface ProcessingEvent {
    object Idle : ProcessingEvent
    object Loading : ProcessingEvent
    object Success: ProcessingEvent
    object Error: ProcessingEvent
}