package com.gasmonsoft.fuelboxcontrol.data.repository

import com.gasmonsoft.fuelboxcontrol.data.service.RemoteFscDataSource
import com.gasmonsoft.fuelboxcontrol.model.ConfVehicle
import com.gasmonsoft.fuelboxcontrol.model.login.Login
import com.gasmonsoft.fuelboxcontrol.model.login.LoginResponse
import com.gasmonsoft.fuelboxcontrol.model.login.toDto
import com.gasmonsoft.fuelboxcontrol.model.sensor.AlertasInfo
import com.gasmonsoft.fuelboxcontrol.model.sensor.ConfVehiclesResponse
import com.gasmonsoft.fuelboxcontrol.model.sensor.SensorInfo
import javax.inject.Inject

class FuelSoftwareControlRepository @Inject constructor(
    private val remoteFscDataSource: RemoteFscDataSource,
) {

    suspend fun login(login: Login): Result<List<LoginResponse>> {
        return remoteFscDataSource.login(login.toDto())
    }

    suspend fun getVehicleData(body: ConfVehicle): Result<List<ConfVehiclesResponse>> {
        return remoteFscDataSource.getVehicleData(
            token = body.token,
            idVehiculo = body.idVehiculo
        )
    }

    suspend fun sendSensorData(body: SensorInfo) {
        remoteFscDataSource.addSensorDatosUnitariaList(
            token = body.token,
            body = listOf(body.data)
        )
    }

    suspend fun sendAlertGenerales(body: AlertasInfo) {
        remoteFscDataSource.addSensorAlertasData(
            token = body.token,
            body = body.data
        )
    }
}