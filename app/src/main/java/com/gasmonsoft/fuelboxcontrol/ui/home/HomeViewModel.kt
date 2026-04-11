package com.gasmonsoft.fuelboxcontrol.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gasmonsoft.fuelboxcontrol.data.repository.ble.SensorReceiveManager
import com.gasmonsoft.fuelboxcontrol.data.repository.user.UserRepository
import com.gasmonsoft.fuelboxcontrol.utils.NetworkConfig
import com.gasmonsoft.fuelboxcontrol.utils.ProcessingEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val sensorReceiveManager: SensorReceiveManager,
    private val userRepository: UserRepository
) : ViewModel() {
    var logoutEvent: MutableStateFlow<ProcessingEvent> = MutableStateFlow(ProcessingEvent.Idle)
    val connectionStatus = sensorReceiveManager.connectionState
    val availableDevices = sensorReceiveManager.discoveredDevices

    val currentUser = userRepository.getUser().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    fun scanDevices() {
        sensorReceiveManager.startReceiving()
    }

    fun selectDevice(mac: String) {
        NetworkConfig.nombreconfiguracion = mac
        NetworkConfig.configuracion = "mac"
        // Disparamos el proceso de conexión inmediatamente
        sensorReceiveManager.startReceiving()
    }

    fun logout() {
        viewModelScope.launch {
            logoutEvent.value = ProcessingEvent.Loading
            userRepository.deleteUser().onSuccess {
                logoutEvent.value = ProcessingEvent.Success
            }.onFailure {
                logoutEvent.value = ProcessingEvent.Error
            }
        }
    }

    fun dismissLogoutError() {
        logoutEvent.value = ProcessingEvent.Idle
    }
}
