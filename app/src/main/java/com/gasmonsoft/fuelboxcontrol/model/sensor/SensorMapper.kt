package com.gasmonsoft.fuelboxcontrol.model.sensor

fun ConfVehiclesResponse.toEntity() = ConfigVehiculo(
    tarjeta = tarjeta,
    caracteristicas = caracteristicas,
    noEconomico = noEconomico,
    totalSensor = totalSensor
)
