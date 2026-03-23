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

    @SerializedName("fld_tarjeta") var tarjeta: String,
    @SerializedName("fld_caracteristicas") var caracteristicas: String,
    @SerializedName("fld_totalSensor") var totalSensor: Int,
    @SerializedName("fld_rutas") var rutas: String,
    @SerializedName("fld_placas") var placas: String,
    @SerializedName("fld_noEconomico") var noEconomico: String
)

data class Tendencia(
    val pendiente: Double,
    val intercepto: Double,
    val puntoInicial: Int,
    val puntoFinal: Int,
    val sampleValue: Pair<Double, Double>,
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
