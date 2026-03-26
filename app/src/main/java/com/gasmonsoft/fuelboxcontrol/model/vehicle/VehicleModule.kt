package com.gasmonsoft.fuelboxcontrol.model.vehicle

import com.google.gson.annotations.SerializedName

data class ConfVehicle(
    val token: String,
    val idVehiculo: Int
)

data class ConfVehiclesResponse(

    @SerializedName("fld_tarjeta") var tarjeta: String,
    @SerializedName("fld_caracteristicas") var caracteristicas: String,
    @SerializedName("fld_totalSensor") var totalSensor: Int,
    @SerializedName("fld_rutas") var rutas: String,
    @SerializedName("fld_placas") var placas: String,
    @SerializedName("fld_noEconomico") var noEconomico: String
)

data class ConfigVehiculo(
    val tarjeta: String,
    val caracteristicas: String,
    val noEconomico: String,
    val totalSensor: Int
)