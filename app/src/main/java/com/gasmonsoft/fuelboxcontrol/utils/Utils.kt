package com.gasmonsoft.fuelboxcontrol.utils

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

interface ProcessingEvent {
    object Idle : ProcessingEvent
    object Loading : ProcessingEvent
    object Success: ProcessingEvent
    object Error: ProcessingEvent
}