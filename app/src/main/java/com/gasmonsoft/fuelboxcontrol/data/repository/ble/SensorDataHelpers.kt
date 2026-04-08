package com.gasmonsoft.fuelboxcontrol.data.repository.ble

import com.gasmonsoft.fuelboxcontrol.data.model.ble.AccelerometerData
import com.gasmonsoft.fuelboxcontrol.data.model.ble.SensorData

fun getAcelerometroData(sensorData: List<String>): AccelerometerData {
    val date = "${sensorData[0]} ${sensorData[1]}"
    return AccelerometerData(
        date = date,
        value = sensorData.drop(2).last()
    )
}

fun setErrorAccelerometer(rawData: String): AccelerometerData {
    return AccelerometerData(
        rawData = rawData,
        error = true
    )
}

fun getSensorData(sensorData: List<String>): SensorData {
    val date = "${sensorData[0]} ${sensorData[1]}"
    val data = sensorData.drop(2)
    val isError = data.any {
        val temp = data[2].toIntOrNull()
        val calidad = data[0].toIntOrNull()
        val volumen = data[1].toIntOrNull()
        if (temp == null || calidad == null || volumen == null) return@any true
        data.size != 3 && volumen > 0 && calidad > 0 && temp > 0
    }
    return SensorData(
        date = date,
        temperatura = if (data.size >= 3) data[2] else "",
        volumen = if (data.isNotEmpty()) data[0] else "",
        calidad = if (data.size >= 2) data[1] else "",
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