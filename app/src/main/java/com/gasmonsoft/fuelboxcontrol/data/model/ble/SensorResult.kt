package com.gasmonsoft.fuelboxcontrol.data.model.ble

data class SensorResult(
    val SensorId: String,
    val Volumen: String,
    val Temperatura: String,
    val Constante: String,
    val Fecha: String,
    var Alertas: String,
    val connectionState: ConnectionState
)