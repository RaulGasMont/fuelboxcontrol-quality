package com.gasmonsoft.fuelboxcontrol.data.repository

import com.gasmonsoft.fuelboxcontrol.data.service.SensorService
import com.gasmonsoft.fuelboxcontrol.model.SensorResponse
import javax.inject.Inject

class SensorRepository @Inject constructor(
    private val sensorService: SensorService,
) {

    suspend fun doSensor(response: List<SensorResponse>) {
        try {
            return sensorService.doSensor()
        } catch (e: Exception) {
            throw e
        }
    }
}