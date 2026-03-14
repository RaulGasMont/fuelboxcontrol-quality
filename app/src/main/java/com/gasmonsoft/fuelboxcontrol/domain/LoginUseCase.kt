package com.gasmonsoft.fuelboxcontrol.domain

import com.gasmonsoft.fuelboxcontrol.data.repository.FuelSoftwareControlRepository
import com.gasmonsoft.fuelboxcontrol.model.login.Login
import com.gasmonsoft.fuelboxcontrol.model.login.UserData
import com.gasmonsoft.fuelboxcontrol.model.login.toEntity
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val repository: FuelSoftwareControlRepository
) {
    suspend operator fun invoke(username: String, password: String): Result<UserData?> {
        val result = repository.login(Login(username, password, ""))
        return if (result.isSuccessful) Result.success(result.body()?.firstOrNull()?.toEntity())
        else Result.failure(Exception(result.message()))
    }
}