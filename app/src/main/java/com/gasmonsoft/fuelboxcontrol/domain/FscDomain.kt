package com.gasmonsoft.fuelboxcontrol.domain

import com.gasmonsoft.fuelboxcontrol.data.repository.FuelSoftwareControlRepository
import com.gasmonsoft.fuelboxcontrol.model.ConfVehicle
import com.gasmonsoft.fuelboxcontrol.model.login.Login
import com.gasmonsoft.fuelboxcontrol.model.login.UserData
import com.gasmonsoft.fuelboxcontrol.model.login.toEntity
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val repository: FuelSoftwareControlRepository
) {
    suspend operator fun invoke(username: String, password: String): Result<UserData?> {
        val result = repository.login(Login(username, password, ""))
        return if (result.isSuccess) Result.success(result.getOrNull()?.firstOrNull()?.toEntity())
        else Result.failure(Exception("Ocurrio un error al traer la informacion del servidor."))
    }
}

class ConfigVehicleUseCase @Inject constructor(private val repository: FuelSoftwareControlRepository) {
    suspend operator fun invoke(token: String, idVehicle: Int) {
        val result = repository.getVehicleData(ConfVehicle(token, idVehicle))
        if (result.isSuccess) Result.success(result.getOrNull()?.firstOrNull())
    }
}

//fun loadConfVehicle(vehicleId: Int) {
//    val token = _uiState.value.userData?.token ?: return
//    viewModelScope.launch {
//        try {
//            var idCaja: String? = null
//
//            val result = confVehicleUseCase("Bearer $token", vehicleId)
//
//        }