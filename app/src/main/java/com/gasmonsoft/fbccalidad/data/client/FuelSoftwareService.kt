package com.gasmonsoft.fbccalidad.data.client

import com.gasmonsoft.fbccalidad.data.model.alert.FuelAlertDto
import com.gasmonsoft.fbccalidad.data.model.login.CalidadLoginResponse
import com.gasmonsoft.fbccalidad.data.model.login.LoginDto
import com.gasmonsoft.fbccalidad.data.model.login.LoginResponse
import com.gasmonsoft.fbccalidad.data.model.matter.MatterResponseDto
import com.gasmonsoft.fbccalidad.data.model.selectvehicle.ContenedoresAMedirResponse
import com.gasmonsoft.fbccalidad.data.model.sensor.LastQualitySensorData
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

    @POST("api/alertasExpo/crear")
    suspend fun sendFuelAlert(
        @Header("Authorization") token: String,
        @Body alertDto: FuelAlertDto
    ): Response<ResponseBody>


    @GET("api/SensorCalidadCajaApi/UltimoDatoSensorCalidad")
    suspend fun getLastQualitySensorRecords(
        @Header("Authorization") token: String,
        @Query("id_cajaCalidad") idCajaCalidad: Int
    ): Response<List<LastQualitySensorData>>

    @GET("api/alertasExpo/GetListSustancias")
    suspend fun getMatters(): Response<List<MatterResponseDto>>
}