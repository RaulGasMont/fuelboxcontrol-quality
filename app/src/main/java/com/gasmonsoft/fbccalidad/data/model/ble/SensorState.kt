package com.gasmonsoft.fbccalidad.data.model.ble

data class SensorState(
    val sensor1: SensorData = SensorData()
)

data class SensorData(
    val rawData: String = "",
    val error: Boolean = false,
    val date: String = "",
    val temperatura: String = "",
    val calidad: String = ""
)