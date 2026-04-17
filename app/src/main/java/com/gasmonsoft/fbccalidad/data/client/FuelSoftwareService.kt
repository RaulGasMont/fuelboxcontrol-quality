package com.gasmonsoft.fbccalidad.data.client

import com.gasmonsoft.fbccalidad.data.model.calibracion.CalibrationDto
import com.gasmonsoft.fbccalidad.data.model.login.CalidadLoginResponse
import com.gasmonsoft.fbccalidad.data.model.login.LoginDto
import com.gasmonsoft.fbccalidad.data.model.login.LoginResponse
import com.gasmonsoft.fbccalidad.data.model.selectvehicle.ContenedoresAMedirResponse
import com.gasmonsoft.fbccalidad.data.model.sensor.SensorCalidadUnitarioPkg
import com.gasmonsoft.fbccalidad.data.model.sensor.UploadSensorResponse
import com.gasmonsoft.fbccalidad.data.model.vehicle.ConfVehiclesResponse
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Streaming

interface FuelSoftwareService {

    @POST("api/authenticateCalidad")
    suspend fun boxLogin(@Body loginDto: LoginDto): Response<List<CalidadLoginResponse>>

    @GET("api/SensorCalidadCajaApi/ContenedoresAMedirCalidad")
    suspend fun getContenedoresAMedir(
        @Header("Authorization") token: String,
        @Query("id_empresa") idEmpresa: String,
    ): Response<List<ContenedoresAMedirResponse>>

    @POST("api/authenticate")
    suspend fun login(@Body loginDto: LoginDto): Response<List<LoginResponse>>

    @POST("api/SensorCalidadCajaApi/AddSensorCalidadDataUnitarioList")
    suspend fun addSensorDatosUnitariaList(
        @Header("Authorization") token: String,
        @Body datos: SensorCalidadUnitarioPkg,
    ): Response<UploadSensorResponse>

    @GET("Vehiculo/ConfVehiculoB")
    suspend fun doConfVehicle(
        @Header("Authorization") token: String,
        @Query("id_vehiculo") idVehiculo: Int
    ): Response<List<ConfVehiclesResponse>>

    @Streaming
    @POST("api/SensorCajaApi/ParametrosCalibracion")
    suspend fun getDatFile(
        @Header("Authorization") token: String,
        @Body datos: CalibrationDto,
    ): Response<ResponseBody>
}