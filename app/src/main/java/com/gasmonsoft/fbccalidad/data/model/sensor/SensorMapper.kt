package com.gasmonsoft.fbccalidad.data.model.sensor

import com.gasmonsoft.fbccalidad.data.model.vehicle.ConfVehiclesResponse
import com.gasmonsoft.fbccalidad.data.model.vehicle.ConfigVehiculo

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
