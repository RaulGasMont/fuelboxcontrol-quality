package com.gasmonsoft.fuelboxcontrol.ui.vehiculo.viewmodel

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gasmonsoft.fuelboxcontrol.data.model.wifi.WifiConnectionState
import com.gasmonsoft.fuelboxcontrol.data.service.wifi.WifiStateObserver
import com.gasmonsoft.fuelboxcontrol.domain.api.ConfigVehicleUseCase
import com.gasmonsoft.fuelboxcontrol.domain.api.LoginUseCase
import com.gasmonsoft.fuelboxcontrol.domain.sensor.SensorSenderUseCase
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
    val logSensorData = sensorSenderUseCase.logSensorData
    val dataSendStatus = sensorSenderUseCase.sensorSenderStatus

    init {
        sendSensorData()
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
                            currentUiState.copy(
                                isLoggedIn = true,
                                userData = data,
                                loginEvent = NetworkEvent.Success("Ingreso exitoso.")
                            )
                        } else {
                            currentUiState.copy(
                                loginEvent = NetworkEvent.Error("Usuario o contraseña incorrectos.")
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
