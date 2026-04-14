package com.gasmonsoft.fuelboxcontrol.data.repository.api

import com.gasmonsoft.fuelboxcontrol.data.model.calibracion.Calibracion
import com.gasmonsoft.fuelboxcontrol.data.model.calibracion.toDto
import com.gasmonsoft.fuelboxcontrol.data.model.login.Login
import com.gasmonsoft.fuelboxcontrol.data.model.login.LoginResponse
import com.gasmonsoft.fuelboxcontrol.data.model.login.toDto
import com.gasmonsoft.fuelboxcontrol.data.model.sensor.SensorCalidadUnitario
import com.gasmonsoft.fuelboxcontrol.data.model.sensor.UploadSensorResponse
import com.gasmonsoft.fuelboxcontrol.data.model.vehicle.ConfVehicle
import com.gasmonsoft.fuelboxcontrol.data.model.vehicle.ConfVehiclesResponse
import com.gasmonsoft.fuelboxcontrol.data.repository.user.UserRepository
import com.gasmonsoft.fuelboxcontrol.data.service.ble.BleDataSource
import com.gasmonsoft.fuelboxcontrol.data.service.ble.RemoteFscDataSource
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class FuelSoftwareControlRepository @Inject constructor(
    private val remoteFscDataSource: RemoteFscDataSource,
    private val userRepository: UserRepository,
    bleDataSource: BleDataSource
) {
    val sensorPackages = bleDataSource.sensorState

    val logDataframe = bleDataSource.logDataframe

    suspend fun login(login: Login): Result<List<LoginResponse>> {
        return remoteFscDataSource.login(login.toDto())
    }

    suspend fun getVehicleData(body: ConfVehicle): Result<List<ConfVehiclesResponse>> {
        return remoteFscDataSource.getVehicleData(
            token = body.token,
            idVehiculo = body.idVehiculo
        )
    }

    suspend fun sendSensorCalidadData(body: SensorCalidadUnitario): Result<UploadSensorResponse> {
        val token = userRepository.getUser().first()?.token
            ?: return Result.failure(Exception("No se pudo obtener la informacion del usuario."))
        return remoteFscDataSource.addSensorDatosUnitariaList(
            token = token,
            body = body
        )
    }

//    suspend fun getDatFile(body: Calibracion): Result<ByteArray> {
//        val result = login(Login("9612406463", "1234", "test"))
//        return if (result.isSuccess) {
//            val userData = result.getOrNull()
//                ?: return Result.failure(Exception("No se pudo obtener el token"))
//            val user = userData.firstOrNull()
//                ?: return Result.failure(Exception("No se pudo obtener el token"))
//            remoteFscDataSource.getDatFile(
//                token = user.token,
//                body = body.toDto()
//            )
//        } else {
//            Result.failure(Exception("No se pudo obtener el token"))
//        }
//    }

//    suspend fun sendAlertGenerales(
//        body: SensorAlertasUnitario
//    ): Result<UploadSensorResponse> {
//        val token = userRepository.getUser().first()?.token
//            ?: return Result.failure(Exception("No se pudo obtener la informacion del usuario."))
//        return remoteFscDataSource.addSensorAlertasData(
//            token = token,
//            body = SensorAlertasUnitarioRequest(listOf(body))
//        )
//    }
}