package com.gasmonsoft.fbccalidad.data.model.login

import com.gasmonsoft.fbccalidad.data.model.vehicle.VehicleInfo
import com.google.gson.annotations.SerializedName

data class LoginDto(
    @SerializedName("fld_password") val password: String,
    @SerializedName("fld_usuario") val username: String,
    @SerializedName("fld_id") val fbToken: String
)

data class Login(
    val username: String,
    val password: String,
    val token: String
)

fun Login.toDto(): LoginDto = LoginDto(
    username = username,
    password = password,
    fbToken = token
)

data class CalidadLoginResponse(
    @SerializedName("id") var id: Int,
    @SerializedName("message") var message: String,
    @SerializedName("c_tipo_usuario") var c_tipo_usuario: Int,
    @SerializedName("id_usuario") var id_usuario: Int,
    @SerializedName("fld_usuario") var fld_usuario: String,
    @SerializedName("id_empresa") var id_empresa: Int,
    @SerializedName("fld_nombreCompleto") var nombreCompleto: String,
    @SerializedName("fld_correo") var fld_correo: String,
    @SerializedName("token") var token: String,
    @SerializedName("fld_nombreEmpresa") var nombreEmpresa: String,
    @SerializedName("fld_terminos") var terminos: Boolean,
    @SerializedName("fld_cajasCalidad") val cajasCalidad: String
)

data class LoginResponse(
    @SerializedName("id") var id: Int,
    @SerializedName("message") var message: String,
    @SerializedName("c_tipo_usuario") var c_tipo_usuario: Int,
    @SerializedName("id_usuario") var id_usuario: Int,
    @SerializedName("fld_usuario") var fld_usuario: String,
    @SerializedName("id_empresa") var id_empresa: Int,
    @SerializedName("fld_encontrado") var fld_encontrado: Int,
    @SerializedName("fld_correo") var fld_correo: String,
    @SerializedName("token") var token: String,
    @SerializedName("id_empresaReembolso") var id_empresaReembolso: Int,
    @SerializedName("id_tipoEmpresaSoftware") var id_tipoEmpresaSoftware: Int,
    @SerializedName("id_tipoEmpresaTarjeta") var idTipoEmpresaTarjeta: Int,
    @SerializedName("fld_nombreEmpresa") var fld_nombreEmpresa: String,
    @SerializedName("vehiculos") var vehiculos: String,
    @SerializedName("fecha") var fecha: String,
    @SerializedName("fld_joinAutocnsumo") var joinAutoconsumo: Boolean,
    @SerializedName("fld_terminos") var terminos: Boolean,
    @SerializedName("id_tarjetaEmpleado") var idTarjetaEmpleado: Int
) {
    fun toLoginResult(): LoginResult {
        return LoginResult(
            valueResponse = id,
            tipoUsuario = c_tipo_usuario,
            errorMessage = message
        )
    }
}

data class UserData(
    val token: String,
    val username: String,
    val idUser: Int,
    val vehiculos: List<VehicleInfo>
)

fun LoginResponse.toEntity(vehicles: List<VehicleInfo>) = UserData(
    token = token,
    username = fld_usuario,
    idUser = id_usuario,
    vehiculos = vehicles
)

data class LoginResult(
    val valueResponse: Int,
    val tipoUsuario: Int,
    val errorMessage: String
)

