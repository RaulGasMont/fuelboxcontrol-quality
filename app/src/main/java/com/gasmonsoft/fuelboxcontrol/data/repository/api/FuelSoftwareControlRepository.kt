package com.gasmonsoft.fuelboxcontrol.data.repository.api

import com.gasmonsoft.fuelboxcontrol.data.service.ble.BleDataSource
import com.gasmonsoft.fuelboxcontrol.data.service.ble.RemoteFscDataSource
import com.gasmonsoft.fuelboxcontrol.model.calibracion.Calibration
import com.gasmonsoft.fuelboxcontrol.model.calibracion.toDto
import com.gasmonsoft.fuelboxcontrol.model.login.Login
import com.gasmonsoft.fuelboxcontrol.model.login.LoginResponse
import com.gasmonsoft.fuelboxcontrol.model.login.toDto
import com.gasmonsoft.fuelboxcontrol.model.sensor.ConfVehiclesResponse
import com.gasmonsoft.fuelboxcontrol.model.sensor.SensorAlertasUnitario
import com.gasmonsoft.fuelboxcontrol.model.sensor.SensorAlertasUnitarioRequest
import com.gasmonsoft.fuelboxcontrol.model.sensor.SensorInfo
import com.gasmonsoft.fuelboxcontrol.model.sensor.UploadSensorResponse
import com.gasmonsoft.fuelboxcontrol.model.vehicle.ConfVehicle
import javax.inject.Inject

class FuelSoftwareControlRepository @Inject constructor(
    private val remoteFscDataSource: RemoteFscDataSource,
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

    suspend fun sendSensorData(body: SensorInfo): Result<UploadSensorResponse> {
        return remoteFscDataSource.addSensorDatosUnitariaList(
            token = body.token,
            body = listOf(body.data)
        )
    }

    suspend fun getDatFile(body: Calibration): Result<ByteArray> {
        val result = login(Login("9612406463", "1234", "test"))
        return if (result.isSuccess) {
            val userData = result.getOrNull()
                ?: return Result.failure(Exception("No se pudo obtener el token"))
            val user = userData.firstOrNull()
                ?: return Result.failure(Exception("No se pudo obtener el token"))
            remoteFscDataSource.getDatFile(
                token = user.token,
                body = body.toDto()
            )
        } else {
            Result.failure(Exception("No se pudo obtener el token"))
        }
    }

    suspend fun sendAlertGenerales(token: String, body: SensorAlertasUnitario) {
        remoteFscDataSource.addSensorAlertasData(
            token = token,
            body = SensorAlertasUnitarioRequest(listOf(body))
        )
    }
}