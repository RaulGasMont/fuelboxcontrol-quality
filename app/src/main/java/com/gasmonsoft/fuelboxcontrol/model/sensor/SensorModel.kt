package com.gasmonsoft.fuelboxcontrol.model.sensor

import com.google.gson.annotations.SerializedName

data class UploadSensorResponse(
    val success: Boolean,
    val message: String,
    val totalInsertadas: Int,
    val totalDuplicadas: Int,
    val totalErrores: Int,
    val idsInsertadas: List<Int>,
    val errores: List<String>
)

data class SensorAlertasUnitarioRequest(
    val alertas: List<SensorAlertasUnitario>
)

data class ConfVehiclesResponse(

    @SerializedName("fld_tarjeta") var fld_tarjeta: String,
    @SerializedName("fld_caracteristicas") var fld_caracteristicas: String,
    @SerializedName("fld_totalSensor") var fld_totalSensor: Int,
    @SerializedName("fld_rutas") var fld_rutas: String,
    @SerializedName("fld_placas") var fld_placas: String,
    @SerializedName("fld_noEconomico") var fld_noEconomico: String
)


data class ConfigVehiculo(
    val tarjeta: String,
    val caracteristicas: String,
    val noEconomico: String,
    val totalSensor: Int
)

data class SensorInfo(
    val token: String,
    val data: SensorDataUnitario
)

data class AlertasInfo(
    val token: String,
    val data: SensorAlertasUnitarioRequest
)

data class SensorDataUnitario(
    @SerializedName("id_datosSensor")
    val idDatosSensor: Int = 0,
    @SerializedName("id_cajaComunicaciones")
    val idCajaComunicaciones: Int,
    @SerializedName("id_usuario")
    val idUsuario: Int,
    @SerializedName("fld_fechaRegistro")
    val fecha: String,
    @SerializedName("fld_tipoUsuario")
    val tipoUsuario: Boolean,
    @SerializedName("fld_json")
    val jsonData: String
)

data class SensorAlertasUnitario(
    @SerializedName("id_cajaComunicaciones")
    val idCajaComunicaciones: Int,
    @SerializedName("id_usuario")
    val idUsuario: Int,
    @SerializedName("fld_fecha")
    val fecha: String,
    @SerializedName("fld_alertas")
    val alertas: String
)
