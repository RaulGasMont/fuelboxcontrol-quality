package com.gasmonsoft.fuelboxcontrol.data.ble

data class SensorState(
    val sensor1: SensorData = SensorData(),
    val sensor2: SensorData = SensorData(),
    val sensor3: SensorData = SensorData(),
    val sensor4: SensorData = SensorData(),
    val acelerometro: AccelerometerData = AccelerometerData(),
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
