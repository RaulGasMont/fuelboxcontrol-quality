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
        data.size >= 3 && data[2] == "-555" && data[1] == "-555" && data[0] == "-555"
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