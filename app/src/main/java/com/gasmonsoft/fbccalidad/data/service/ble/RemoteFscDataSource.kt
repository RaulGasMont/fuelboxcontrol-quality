package com.gasmonsoft.fbccalidad.data.service.ble

import com.gasmonsoft.fbccalidad.data.client.FuelSoftwareService
import com.gasmonsoft.fbccalidad.data.model.alert.FuelAlertDto
import com.gasmonsoft.fbccalidad.data.model.login.LoginDto
import com.gasmonsoft.fbccalidad.data.model.sensor.SensorCalidadUnitario
import com.gasmonsoft.fbccalidad.data.model.sensor.SensorCalidadUnitarioPkg
import com.gasmonsoft.fbccalidad.data.service.utils.networkRequestHelper
import javax.inject.Inject

class RemoteFscDataSource @Inject constructor(private val fscService: FuelSoftwareService) {
    suspend fun login(body: LoginDto) =
        networkRequestHelper {
            fscService.login(body)
        }

    suspend fun addSensorDatosUnitariaList(token: String, body: SensorCalidadUnitario) =
        networkRequestHelper {
            fscService.addSensorDatosUnitariaList(
                "Bearer $token",
                SensorCalidadUnitarioPkg(listOf(body))
            )
        }

    suspend fun getVehicleData(token: String, idVehiculo: Int) = networkRequestHelper {
        fscService.doConfVehicle(
            token = "Bearer $token",
            idVehiculo = idVehiculo
        )
    }

    suspend fun sendFuelAlert(token: String, body: FuelAlertDto) =
        networkRequestHelper {
            fscService.sendFuelAlert(
                token = "Bearer $token",
                alertDto = body
            )
        }

    suspend fun getMatters() =
        networkRequestHelper {
            fscService.getMatters()
        }
}