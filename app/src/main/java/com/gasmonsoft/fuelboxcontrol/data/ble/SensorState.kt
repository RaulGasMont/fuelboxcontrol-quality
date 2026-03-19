package com.gasmonsoft.fuelboxcontrol.data.ble

data class SensorState(
    val sensor1: SensorData? = null,
    val sensor2: SensorData? = null,
    val sensor3: SensorData? = null,
    val sensor4: SensorData? = null,
    val acelerometro: AccelerometerData? = null,
    val alertas: String = ""
)

data class SensorData(
    val rawData: String = "",
    val error: Boolean = false,
    val date: String = "",
    val temperatura: String = "",
    val volumen: String = "",
    val calidad: String = ""
)

data class AccelerometerData(
    val rawData: String = "",
    val error: Boolean = false,
    val date: String = "",
    val value: String = "",
)
