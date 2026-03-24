package com.gasmonsoft.fuelboxcontrol.ui.vehiculo.viewmodel

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gasmonsoft.fuelboxcontrol.data.model.wifi.WifiConnectionState
import com.gasmonsoft.fuelboxcontrol.data.service.wifi.WifiStateObserver
import com.gasmonsoft.fuelboxcontrol.domain.api.ConfigVehicleUseCase
import com.gasmonsoft.fuelboxcontrol.domain.api.LoginUseCase
import com.gasmonsoft.fuelboxcontrol.domain.sensor.SensorSenderUseCase
import com.gasmonsoft.fuelboxcontrol.model.login.UserData
import com.gasmonsoft.fuelboxcontrol.model.vehicle.VehicleConfiguration
import com.gasmonsoft.fuelboxcontrol.model.vehicle.VehicleInfo
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VehiculosViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    private val configVehicle: ConfigVehicleUseCase,
    private val sensorSenderUseCase: SensorSenderUseCase,
    private val sharedPreferences: SharedPreferences,
    wifiStateObserver: WifiStateObserver
) : ViewModel() {
    private val _uiState = MutableStateFlow(VehiculoUiState())
    val uiState: StateFlow<VehiculoUiState> = _uiState.asStateFlow()

    val wifiState: StateFlow<WifiConnectionState> =
        wifiStateObserver.observe()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = WifiConnectionState()
            )

    val sensorData = sensorSenderUseCase.sensorInfo
    val dataSendStatus = sensorSenderUseCase.sensorSenderStatus

    private val gson = Gson()
    private val KEY_USER_DATA = "user_data_session"
    private val KEY_VEHICLE_CONFIG = "vehicle_config_session"
    private val KEY_SELECTED_VEHICLE = "selected_vehicle_session"

    init {
        restoreSession()
        sendSensorData()
    }

    private fun restoreSession() {
        val userDataJson = sharedPreferences.getString(KEY_USER_DATA, null)
        val vehicleConfigJson = sharedPreferences.getString(KEY_VEHICLE_CONFIG, null)
        val selectedVehicleJson = sharedPreferences.getString(KEY_SELECTED_VEHICLE, null)

        if (userDataJson != null) {
            val userData = gson.fromJson(userDataJson, UserData::class.java)
            val vehicleConfig =
                vehicleConfigJson?.let { gson.fromJson(it, VehicleConfiguration::class.java) }
            val selectedVehicle =
                selectedVehicleJson?.let { gson.fromJson(it, VehicleInfo::class.java) }

            _uiState.update {
                it.copy(
                    isLoggedIn = true,
                    userData = userData,
                    vehicleConfiguration = vehicleConfig,
                    vehicleInfo = selectedVehicle
                )
            }
        }
    }

    private fun saveSession(
        userData: UserData? = null,
        config: VehicleConfiguration? = null,
        vehicle: VehicleInfo? = null
    ) {
        sharedPreferences.edit().apply {
            userData?.let { putString(KEY_USER_DATA, gson.toJson(it)) }
            config?.let { putString(KEY_VEHICLE_CONFIG, gson.toJson(it)) }
            vehicle?.let { putString(KEY_SELECTED_VEHICLE, gson.toJson(it)) }
            apply()
        }
    }

    fun doLogin(username: String, password: String) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    loginEvent = NetworkEvent.Loading
                )
            }

            val login = loginUseCase(
                username = username,
                password = password
            )

            login.fold(
                onSuccess = { data ->
                    _uiState.update { currentUiState ->
                        if (data != null) {
                            saveSession(userData = data)
                            currentUiState.copy(
                                isLoggedIn = true,
                                userData = data,
                                loginEvent = NetworkEvent.Success("Ingreso exitoso.")
                            )
                        } else {
                            currentUiState.copy(
                                loginEvent = NetworkEvent.Error("El usuario no cuenta con vehiculos.")
                            )
                        }
                    }
                },
                onFailure = {
                    val errorMessage = it.message ?: "Error desconocido"
                    _uiState.update {
                        it.copy(
                            isLoggedIn = false,
                            loginEvent = NetworkEvent.Error("Error del servidor: $errorMessage")
                        )
                    }
                }
            )
        }
    }

    fun dismissLoginDialog() {
        _uiState.update {
            it.copy(
                loginEvent = null
            )
        }
    }

    fun getVehicleData(idVehicle: Int) {
        viewModelScope.launch {
            val token = _uiState.value.userData?.token ?: return@launch
            val vehicle = _uiState.value.userData?.vehiculos?.find { it.id == idVehicle.toString() }
            _uiState.update {
                it.copy(
                    vehicleInfo = vehicle,
                    vehicleEvent = NetworkEvent.Loading
                )
            }
            configVehicle(token, idVehicle).fold(
                onSuccess = { config ->
                    saveSession(config = config, vehicle = vehicle)
                    _uiState.update { currentUiState ->
                        currentUiState.copy(
                            vehicleConfiguration = config,
                            vehicleEvent = NetworkEvent.Success("Informacion del vehiculo traida con exito.")
                        )
                    }
                },
                onFailure = {
                    _uiState.update { currentUiState ->
                        currentUiState.copy(
                            vehicleEvent = NetworkEvent.Error(
                                it.message
                                    ?: "Ocurrio un problema al traer la informacion del servidor."
                            )
                        )
                    }
                }
            )
        }
    }

    fun logout() {
        _uiState.update { VehiculoUiState() }
        sharedPreferences.edit().clear().apply()
    }

    private fun sendSensorData() {
        viewModelScope.launch {
            sensorData.collect { data ->
                val state = _uiState.value
                val token = state.userData?.token
                val idUsuario = state.userData?.idUser
                val idCaja =
                    state.vehicleConfiguration?.idCaja

                if (!token.isNullOrEmpty() && idUsuario != null && !idCaja.isNullOrEmpty()) {
                    sensorSenderUseCase(token, idUsuario, idCaja, data)
                }
            }
        }
    }
}
