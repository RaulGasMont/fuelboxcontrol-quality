package com.gasmonsoft.fuelboxcontrol.ui.sensor.utils

enum class SensorVisualStatus {
    Ok, Error, Empty
}

fun String.toDisplayValue(): String {
    return if (isBlank() || this == "-555") "Sin dato" else this
}