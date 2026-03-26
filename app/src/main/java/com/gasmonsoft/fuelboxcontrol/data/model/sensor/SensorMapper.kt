package com.gasmonsoft.fuelboxcontrol.data.model.sensor

import com.gasmonsoft.fuelboxcontrol.data.model.vehicle.ConfVehiclesResponse
import com.gasmonsoft.fuelboxcontrol.data.model.vehicle.ConfigVehiculo

fun ConfVehiclesResponse.toEntity() = ConfigVehiculo(
    tarjeta = tarjeta,
    caracteristicas = caracteristicas,
    noEconomico = noEconomico,
    totalSensor = totalSensor
)
