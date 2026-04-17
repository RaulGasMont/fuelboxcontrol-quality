package com.gasmonsoft.fbccalidad.data.model.vehicle

data class VehicleInfo(
    val id: String,
    var description: String,
    val mac: String
)

data class VehicleConfiguration(
    val idVehiculo: Int,
    val idCaja: String,
    val tipoCaja: String,
)