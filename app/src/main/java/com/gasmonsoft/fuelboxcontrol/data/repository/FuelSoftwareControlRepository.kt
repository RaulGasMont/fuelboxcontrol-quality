package com.gasmonsoft.fuelboxcontrol.data.repository

import com.gasmonsoft.fuelboxcontrol.data.client.FuelSoftwareService
import com.gasmonsoft.fuelboxcontrol.model.SensorResponse
import com.gasmonsoft.fuelboxcontrol.model.login.Login
import com.gasmonsoft.fuelboxcontrol.model.login.LoginResponse
import com.gasmonsoft.fuelboxcontrol.model.login.toDto
import retrofit2.Response
import javax.inject.Inject

class FuelSoftwareControlRepository @Inject constructor(
    private val fuelSoftwareService: FuelSoftwareService,
) {

    suspend fun login(login: Login): Response<List<LoginResponse>> {
        return fuelSoftwareService.login(login.toDto())
    }

    suspend fun doSensor(response: List<SensorResponse>) {
        // Implementation logic here
    }
}