package com.gasmonsoft.fuelboxcontrol.data.service.user

import com.gasmonsoft.fuelboxcontrol.data.client.FuelSoftwareService
import com.gasmonsoft.fuelboxcontrol.data.model.login.LoginDto
import com.gasmonsoft.fuelboxcontrol.data.networkRequestHelper
import javax.inject.Inject

class UserRemoteDataSource @Inject constructor(private val apiService: FuelSoftwareService) {
    suspend fun login(user: LoginDto) =
        networkRequestHelper {
            apiService.boxLogin(user)
        }.mapCatching { users ->
            users.firstOrNull { it.id == 200 }
                ?: throw Exception("Usuario no encontrado.")
        }
}