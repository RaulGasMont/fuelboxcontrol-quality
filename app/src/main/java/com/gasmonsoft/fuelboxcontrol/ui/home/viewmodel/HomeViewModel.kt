package com.gasmonsoft.fuelboxcontrol.ui.home.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gasmonsoft.fuelboxcontrol.data.repository.ble.SensorReceiveManager
import com.gasmonsoft.fuelboxcontrol.data.repository.user.UserRepository
import com.gasmonsoft.fuelboxcontrol.domain.session.SessionUseCase
import com.gasmonsoft.fuelboxcontrol.utils.NetworkConfig
import com.gasmonsoft.fuelboxcontrol.utils.ProcessingEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeState(
    val logoutEvent: ProcessingEvent = ProcessingEvent.Idle,
    val sessionExpired: Boolean = false
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val sensorReceiveManager: SensorReceiveManager,
    private val userRepository: UserRepository,
    private val sessionUseCase: SessionUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(HomeState())
    val state = _state.asStateFlow()

    var logoutEvent: MutableStateFlow<ProcessingEvent> = MutableStateFlow(ProcessingEvent.Idle)
    val connectionStatus = sensorReceiveManager.connectionState
    val availableDevices = sensorReceiveManager.discoveredDevices

    init {
        viewModelScope.launch {
            userRepository.getUser().collect { user ->
                sessionUseCase(user).fold(
                    onSuccess = {
                        _state.update { it.copy(sessionExpired = false) }
                    },
                    onFailure = {
                        _state.update { it.copy(sessionExpired = true) }
                    }
                )
            }
        }
    }

    fun scanDevices() {
        sensorReceiveManager.startReceiving()
    }

    fun selectDevice(mac: String) {
        NetworkConfig.nombreconfiguracion = mac
        NetworkConfig.configuracion = "mac"
        sensorReceiveManager.startReceiving()
    }

    fun logout() {
        viewModelScope.launch {
            _state.update { currentUiState ->
                currentUiState.copy(
                    logoutEvent = ProcessingEvent.Loading
                )
            }
            userRepository.deleteUser().onSuccess {
                _state.update { currentUiState ->
                    currentUiState.copy(
                        logoutEvent = ProcessingEvent.Success
                    )
                }
            }.onFailure {
                _state.update { currentUiState ->
                    currentUiState.copy(
                        logoutEvent = ProcessingEvent.Error
                    )
                }
            }
        }
    }

    fun dismissLogoutError() {
        _state.update { currentUiState ->
            currentUiState.copy(
                logoutEvent = ProcessingEvent.Idle
            )
        }
    }
}