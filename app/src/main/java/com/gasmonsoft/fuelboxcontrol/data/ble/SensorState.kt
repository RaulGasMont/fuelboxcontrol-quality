package com.gasmonsoft.fuelboxcontrol.data.ble

data class SensorState(
    val sensor1: SensorData? = null,
    val sensor2: SensorData? = null,
    val sensor3: SensorData? = null,
    val sensor4: SensorData? = null,
    val acelerometro: String = "",
    val alertas: String = ""
)

data class SensorData(
    val date: String,
    val temperatura: String,
    val volumen: String,
    val calidad: String
)
