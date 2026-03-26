package com.gasmonsoft.fuelboxcontrol.data.model.sensor

import com.google.gson.annotations.SerializedName

data class SensorResponse (
    @SerializedName("fld_fechaHoraEnvio") var fld_fechaHoraEnvio: String,
    @SerializedName("fld_latitud") var fld_latitud:String,
    @SerializedName("fld_longitud") var fld_longitud:String,
    @SerializedName("fld_valor") var  fld_valor:String,

    @SerializedName("id_vehiculo") var id_vehiculo:Int,
    @SerializedName("fld_uuid") var fld_uuid:String,
    @SerializedName("id_usuario") var id_usuario:Int
)