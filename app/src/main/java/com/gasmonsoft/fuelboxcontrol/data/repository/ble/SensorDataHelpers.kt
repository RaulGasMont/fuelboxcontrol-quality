package com.gasmonsoft.fuelboxcontrol.data.repository.ble

import com.gasmonsoft.fuelboxcontrol.data.model.ble.SensorData

fun getSensorData(sensorData: List<String>): SensorData {
    val date = "${sensorData[0]} ${sensorData[1]}"
    val data = sensorData.drop(2)
    val isError = data.any {
        val temp = data[1].toIntOrNull()
        val calidad = data[0].toIntOrNull()
        if (temp == null || calidad == null) return@any true
        data.size != 2 || (calidad > 0 && temp > 0)
    }
    return SensorData(
        date = date,
        temperatura = if (data.size >= 2) data[1] else "",
        calidad = if (data.isNotEmpty()) data[0] else "",
        error = isError,
        rawData = data.joinToString(",")
    )
}

fun setErrorSensor(rawData: String): SensorData {
    return SensorData(
        rawData = rawData,
        error = rawData.isNotEmpty()
    )
}