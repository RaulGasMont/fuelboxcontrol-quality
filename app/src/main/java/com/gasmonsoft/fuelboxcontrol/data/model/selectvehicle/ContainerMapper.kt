package com.gasmonsoft.fuelboxcontrol.data.model.selectvehicle

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