package com.gasmonsoft.fuelboxcontrol.model.sensor

import com.gasmonsoft.fuelboxcontrol.model.vehicle.ConfVehiclesResponse
import com.gasmonsoft.fuelboxcontrol.model.vehicle.ConfigVehiculo

fun ConfVehiclesResponse.toEntity() = ConfigVehiculo(
    tarjeta = tarjeta,
    caracteristicas = caracteristicas,
    noEconomico = noEconomico,
    totalSensor = totalSensor
)
