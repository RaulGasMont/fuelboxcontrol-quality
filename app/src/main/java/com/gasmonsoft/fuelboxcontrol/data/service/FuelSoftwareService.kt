package com.gasmonsoft.fuelboxcontrol.data.service

import com.gasmonsoft.fuelboxcontrol.model.login.LoginDto
import com.gasmonsoft.fuelboxcontrol.model.login.LoginResponse
import com.gasmonsoft.fuelboxcontrol.model.sensor.SensorAlertasUnitarioRequest
import com.gasmonsoft.fuelboxcontrol.model.sensor.SensorDataUnitario
import com.gasmonsoft.fuelboxcontrol.model.sensor.UploadSensorResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface FuelSoftwareService {

    @POST("api/authenticate")
    fun login(@Body loginDto: LoginDto): Result<List<LoginResponse>>

    @POST("api/SensorCajaApi/AddSensorDataList")
    suspend fun addSensorDatosUnitariaList(
        @Header("Authorization") token: String,
        @Body datos: List<SensorDataUnitario>,
    ): Response<UploadSensorResponse>

    @POST("api/SensorCajaApi/AddSensorAlertaUnitariaList")
    suspend fun addSensorAlertasData(
        @Header("Authorization") token: String,
        @Body body: SensorAlertasUnitarioRequest,
    ): Response<UploadSensorResponse>
}