package com.gasmonsoft.fbccalidad.data.service.user

import com.gasmonsoft.fbccalidad.data.client.FuelSoftwareService
import com.gasmonsoft.fbccalidad.data.model.login.LoginDto
import com.gasmonsoft.fbccalidad.data.networkRequestHelper
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