package com.gasmonsoft.fuelboxcontrol.data.model.selectvehicle

import com.google.gson.annotations.SerializedName



enum class TankType(val value: String) {
    VEHICLE("VEHICLE"),
    OTHER("OTHER")
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

data class ContenedoresAMedirResponse(
    val id: Int,
    @SerializedName("fld_descripcion") val name: String,
    @SerializedName("tipo") val type: Int
)


