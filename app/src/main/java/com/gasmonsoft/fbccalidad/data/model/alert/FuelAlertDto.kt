package com.gasmonsoft.fbccalidad.data.model.alert

import com.google.gson.annotations.SerializedName

data class FuelAlertDto(
    @SerializedName("id_alerta_expo")
    val idAlertaExpo: Int = 0,
    @SerializedName("id_usuario")
    val idUsuario: Int,
    @SerializedName("fld_alertas")
    val alertas: String = "Adulterado",
    @SerializedName("fld_fecha")
    val fecha: String,
    @SerializedName("fld_fechaRegistro")
    val fechaRegistro: String,
    @SerializedName("tipo")
    val tipo: Int,
    @SerializedName("fld_litros")
    val litros: Double = 0.0,
    @SerializedName("fld_activo")
    val activo: Boolean = true
)

data class FuelAlert(
    val idUsuario: Int,
    val fechaRegistro: String,
    val tipo: Int
)

fun FuelAlert.toDto(): FuelAlertDto =
    FuelAlertDto(
        idUsuario = idUsuario,
        fecha = fechaRegistro,
        fechaRegistro = fechaRegistro,
        tipo = tipo
    )
