package com.gasmonsoft.fuelboxcontrol.data.model.selectvehicle

import com.gasmonsoft.fuelboxcontrol.data.model.database.ContainerEntity
import com.gasmonsoft.fuelboxcontrol.data.repository.datastore.TankSelection

fun ContenedoresAMedirResponse.toVehicle(): Vehicle = Vehicle(
    id = id,
    name = name,
    type = TankType.VEHICLE
)

fun ContenedoresAMedirResponse.toOther(): Other = Other(
    id = id,
    name = name,
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