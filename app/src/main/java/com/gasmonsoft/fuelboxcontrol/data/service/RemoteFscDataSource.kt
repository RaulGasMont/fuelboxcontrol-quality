package com.gasmonsoft.fuelboxcontrol.data.service

import com.gasmonsoft.fuelboxcontrol.data.client.FuelSoftwareService
import com.gasmonsoft.fuelboxcontrol.model.login.LoginDto
import com.gasmonsoft.fuelboxcontrol.model.sensor.SensorAlertasUnitarioRequest
import com.gasmonsoft.fuelboxcontrol.model.sensor.SensorDataUnitario
import javax.inject.Inject

class RemoteFscDataSource @Inject constructor(private val fscService: FuelSoftwareService) {
    suspend fun login(body: LoginDto) = networkRequestHelper {
        fscService.login(body)
    }

    suspend fun addSensorDatosUnitariaList(token: String, body: List<SensorDataUnitario>) =
        networkRequestHelper {
            fscService.addSensorDatosUnitariaList(token, body)
        }

    suspend fun addSensorAlertasData(token: String, body: SensorAlertasUnitarioRequest) =
        networkRequestHelper {
            fscService.addSensorAlertasData(token, body)
        }

}