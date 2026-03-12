package com.gasmonsoft.fuelboxcontrol.model

data class VehiclesOption(
    val id: String,
    var description: String,
    val idempresareembolso: Int,
    val tipo: Int,
    val mac: String,
    val idcaja: String,
    val ssid: String,
    val pass: String,
    val gps: String,
    val cardType: String = ""
)
