package com.gasmonsoft.fuelboxcontrol.data.client

import com.gasmonsoft.fuelboxcontrol.model.calibracion.CalibrationDto
import com.gasmonsoft.fuelboxcontrol.model.login.LoginDto
import com.gasmonsoft.fuelboxcontrol.model.login.LoginResponse
import com.gasmonsoft.fuelboxcontrol.model.sensor.ConfVehiclesResponse
import com.gasmonsoft.fuelboxcontrol.model.sensor.SensorAlertasUnitarioRequest
import com.gasmonsoft.fuelboxcontrol.model.sensor.SensorDataUnitario
import com.gasmonsoft.fuelboxcontrol.model.sensor.UploadSensorResponse
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Streaming

interface FuelSoftwareService {

    @POST("api/authenticate")
    suspend fun login(@Body loginDto: LoginDto): Response<List<LoginResponse>>

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

    @GET("Vehiculo/ConfVehiculoB")
    suspend fun doConfVehicle(
        @Header("Authorization") token: String,
        @Query("id_vehiculo") idVehiculo: Int
    ): Response<List<ConfVehiclesResponse>>

    @Streaming
    @GET("api/SensorCajaApi/AddSensorDataList")
    suspend fun getDatFile(
//        @Header("Authorization") token: String,
        @Body datos: CalibrationDto,
    ): Response<ResponseBody>
}