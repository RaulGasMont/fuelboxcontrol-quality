package com.gasmonsoft.fbccalidad.data.model.selectvehicle

import com.gasmonsoft.fbccalidad.data.model.database.ContainerEntity
import com.gasmonsoft.fbccalidad.data.repository.datastore.TankSelection

fun ContainerEntity.toVehicle(): Vehicle = Vehicle(
    id = tankId,
    name = tankName,
    type = TankType.VEHICLE
)

fun ContainerEntity.toOther(): Other = Other(
    id = tankId,
    name = tankName,
    type = TankType.OTHER
)

fun Tank.toTankSelection() = TankSelection(
    tankId = id,
    tankName = name,
    tankType = type.value
)

fun ContenedoresAMedirResponse.toEntity() = ContainerEntity(
    tankId = id,
    tankName = name,
    tankType = type
)