package com.gasmonsoft.fbccalidad.data.service.analizer

import com.gasmonsoft.fbccalidad.data.client.FuelSoftwareService
import com.gasmonsoft.fbccalidad.data.model.login.LoginDto
import javax.inject.Inject

class RemoteUserDataSource @Inject constructor(private val apiService: FuelSoftwareService) {

    suspend fun sendFuelAnalyze(user: LoginDto) {

    }
}