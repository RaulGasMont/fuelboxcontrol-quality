package com.gasmonsoft.fuelboxcontrol.data.model.sensor

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

data class SensorCalidadUnitarioPkg(
    val datos: List<SensorCalidadUnitario>
)

data class SensorInfo(
    val token: String,
    val data: SensorCalidadUnitario
)

data class SensorCalidadUnitario(
    @SerializedName("id_cajaCalidad")
    val idCajaCalidad: Int,
    @SerializedName("id_usuario")
    val idUsuario: Int,
    @SerializedName("id_tipoContenedor")
    val idTipoContenedor: Boolean,
    @SerializedName("fld_fecha")
    val fecha: String,
    @SerializedName("fld_calidad")
    val calidad: Int,
    @SerializedName("fld_temperatura")
    val temperatura: Int
)
