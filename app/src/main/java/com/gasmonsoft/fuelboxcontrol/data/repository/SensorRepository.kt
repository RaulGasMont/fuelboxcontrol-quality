package com.gasmonsoft.fuelboxcontrol.data.repository

import com.gasmonsoft.fuelboxcontrol.data.client.FuelSoftwareService
import com.gasmonsoft.fuelboxcontrol.model.SensorResponse
import javax.inject.Inject

class SensorRepository @Inject constructor(
    private val fuelSoftwareService: FuelSoftwareService,
) {

    suspend fun doSensor(response: List<SensorResponse>) {
        // Implementation logic here
    }
}