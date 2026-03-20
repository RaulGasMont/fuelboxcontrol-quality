package com.gasmonsoft.fuelboxcontrol.domain

import android.content.SharedPreferences
import com.gasmonsoft.fuelboxcontrol.data.repository.ble.FuelSoftwareControlRepository
import com.gasmonsoft.fuelboxcontrol.model.ConfVehicle
import com.gasmonsoft.fuelboxcontrol.model.login.Login
import com.gasmonsoft.fuelboxcontrol.model.login.UserData
import com.gasmonsoft.fuelboxcontrol.model.login.toEntity
import com.gasmonsoft.fuelboxcontrol.model.vehicle.VehicleConfiguration
import com.gasmonsoft.fuelboxcontrol.model.vehicle.VehicleInfo
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val repository: FuelSoftwareControlRepository
) {
    suspend operator fun invoke(username: String, password: String): Result<UserData?> {
        val result = repository.login(Login(username, password, "string"))
        return if (result.isSuccess) {
            val userData = result.getOrNull() ?: emptyList()
            val rawVehicles = userData.firstOrNull()?.vehiculos?.split("|") ?: emptyList()
            val vehicles = rawVehicles.mapNotNull { vehicleValue ->
                val parts = vehicleValue.split(";")
                if (parts.size >= 3) {
                    // Extraemos el ID quitando cualquier prefijo con ':' si existe
                    val idPart = parts[0].split(":")
                    val id = if (idPart.size > 1) idPart.last() else idPart[0]

                    // El MAC suele ser la cuarta parte (índice 3)
                    val macPart = if (parts.size > 3) parts[3].split(":") else null
                    val mac =
                        if (macPart != null && macPart.size > 1) macPart.last() else (macPart?.get(0)
                            ?: "")

                    VehicleInfo(
                        id = id.trim(),
                        description = "${parts[1]} ${parts[2]}".trim(),
                        mac = mac.trim()
                    )
                } else null
            }
            if (vehicles.isEmpty()) return Result.failure(Exception("El usuario no cuenta con vehiculos."))
            Result.success(result.getOrNull()?.firstOrNull()?.toEntity(vehicles))
        } else Result.failure(Exception("Ocurrio un error al traer la informacion del servidor."))
    }
}

class ConfigVehicleUseCase @Inject constructor(
    private val repository: FuelSoftwareControlRepository,
    private val sharedPreferences: SharedPreferences
) {
    suspend operator fun invoke(token: String, idVehicle: Int): Result<VehicleConfiguration?> {
        val result = repository.getVehicleData(ConfVehicle(token, idVehicle))
        return if (result.isSuccess) {
            val vehicleData =
                result.getOrNull()?.first()?.fld_noEconomico?.split("|") ?: emptyList()
            if (vehicleData.isEmpty()) Result.failure(Exception("El vehiculo no cuenta con configuracion."))
            else {
                val idCaja = if (vehicleData.size >= 2) vehicleData[1] else null
                val tipoCaja = if (vehicleData.size == 3) vehicleData[2] else null
                if (tipoCaja != null) {
                    sharedPreferences.edit().putString("tipoCaja", tipoCaja).apply()
                }
                Result.success(
                    VehicleConfiguration(
                        idCaja = idCaja ?: "",
                        tipoCaja = tipoCaja ?: "",
                        idVehiculo = idVehicle
                    )
                )
            }
        } else Result.failure(
            Exception("Ocurrio un error al traer la informacion del servidor.")
        )
    }
}