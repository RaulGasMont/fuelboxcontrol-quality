package com.gasmonsoft.fuelboxcontrol.model.sensor

fun ConfVehiclesResponse.toEntity() = ConfigVehiculo(
    tarjeta = fld_tarjeta,
    caracteristicas = fld_caracteristicas,
    noEconomico = fld_noEconomico,
    totalSensor = fld_totalSensor
)
