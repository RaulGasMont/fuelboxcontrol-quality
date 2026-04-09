package com.gasmonsoft.fuelboxcontrol.data.model.selectvehicle

enum class TankType(val label: String = "") {
    VEHICLE, OTHER
}

interface Tank {
    val id: Int
    val name: String
    val type: TankType
}

data class Vehicle(
    override val id: Int,
    override val name: String,
    override val type: TankType = TankType.VEHICLE
) : Tank

data class Other(
    override val id: Int,
    override val name: String,
    override val type: TankType = TankType.OTHER
) : Tank


