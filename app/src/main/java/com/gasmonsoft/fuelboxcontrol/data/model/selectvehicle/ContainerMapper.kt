package com.gasmonsoft.fuelboxcontrol.data.model.selectvehicle

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
    nameTankId = name,
    tankType = type.value
)