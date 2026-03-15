package com.gasmonsoft.fuelboxcontrol.data.repository

import com.gasmonsoft.fuelboxcontrol.data.service.BleDataSource
import com.gasmonsoft.fuelboxcontrol.data.service.RemoteFscDataSource
import com.gasmonsoft.fuelboxcontrol.model.ConfVehicle
import com.gasmonsoft.fuelboxcontrol.model.login.Login
import com.gasmonsoft.fuelboxcontrol.model.login.LoginResponse
import com.gasmonsoft.fuelboxcontrol.model.login.toDto
import com.gasmonsoft.fuelboxcontrol.model.sensor.ConfVehiclesResponse
import com.gasmonsoft.fuelboxcontrol.model.sensor.SensorAlertasUnitario
import com.gasmonsoft.fuelboxcontrol.model.sensor.SensorAlertasUnitarioRequest
import com.gasmonsoft.fuelboxcontrol.model.sensor.SensorInfo
import com.gasmonsoft.fuelboxcontrol.model.sensor.UploadSensorResponse
import javax.inject.Inject

class FuelSoftwareControlRepository @Inject constructor(
    private val remoteFscDataSource: RemoteFscDataSource,
    bleDataSource: BleDataSource
) {
    val sensorPackages = bleDataSource.sensorState

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

    suspend fun sendAlertGenerales(token: String, body: SensorAlertasUnitario) {
        remoteFscDataSource.addSensorAlertasData(
            token = token,
            body = SensorAlertasUnitarioRequest(listOf(body))
        )
    }
}