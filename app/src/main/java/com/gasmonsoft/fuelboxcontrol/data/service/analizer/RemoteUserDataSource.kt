package com.gasmonsoft.fuelboxcontrol.data.service.analizer

import com.gasmonsoft.fuelboxcontrol.data.client.FuelSoftwareService
import com.gasmonsoft.fuelboxcontrol.data.model.login.LoginDto
import javax.inject.Inject

class RemoteUserDataSource @Inject constructor(private val apiService: FuelSoftwareService) {

    suspend fun sendFuelAnalyze(user: LoginDto) {

    }
}