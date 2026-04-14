package com.gasmonsoft.fuelboxcontrol.data.model.sensor

import com.gasmonsoft.fuelboxcontrol.data.model.vehicle.ConfVehiclesResponse
import com.gasmonsoft.fuelboxcontrol.data.model.vehicle.ConfigVehiculo

fun ConfVehiclesResponse.toEntity() = ConfigVehiculo(
    tarjeta = tarjeta,
    caracteristicas = caracteristicas,
    noEconomico = noEconomico,
    totalSensor = totalSensor
)

fun SensorCalidadData.toDto(idUsuario: Int, fecha: String) = SensorCalidadUnitario(
    idCajaCalidad = idCajaCalidad,
    idUsuario = idUsuario,
    tipoContenedor = idTipoContenedor,
    fecha = fecha,
    calidad = calidad,
    temperatura = temperatura
)
