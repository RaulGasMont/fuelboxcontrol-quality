package com.gasmonsoft.fuelboxcontrol.ui.home.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gasmonsoft.fuelboxcontrol.data.model.boxlogin.QualityBox
import com.gasmonsoft.fuelboxcontrol.data.repository.ble.SensorReceiveManager
import com.gasmonsoft.fuelboxcontrol.data.repository.container.ContainerRepository
import com.gasmonsoft.fuelboxcontrol.data.repository.datastore.DataStoreRepository
import com.gasmonsoft.fuelboxcontrol.data.repository.user.UserRepository
import com.gasmonsoft.fuelboxcontrol.domain.session.SessionUseCase
import com.gasmonsoft.fuelboxcontrol.utils.NetworkConfig
import com.gasmonsoft.fuelboxcontrol.utils.ProcessingEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeState(
    val logoutEvent: ProcessingEvent = ProcessingEvent.Idle,
    val sessionExpired: Boolean = false,
    val boxes: List<QualityBox> = emptyList(),
    val connectingBoxName: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val sensorReceiveManager: SensorReceiveManager,
    private val userRepository: UserRepository,
    private val containerRepository: ContainerRepository,
    private val sessionUseCase: SessionUseCase,
    private val dataStoreRepository: DataStoreRepository
) : ViewModel() {

    private val _state = MutableStateFlow(HomeState())
    val state = _state.asStateFlow()

    var logoutEvent: MutableStateFlow<ProcessingEvent> = MutableStateFlow(ProcessingEvent.Idle)
    val connectionStatus = sensorReceiveManager.connectionState

    init {
        viewModelScope.launch {
            userRepository.getUser().collect { user ->
                sessionUseCase(user).fold(
                    onSuccess = {
                        val boxes = user?.cajasCalidad ?: ""
                        _state.update {
                            it.copy(
                                sessionExpired = false,
                                boxes = rawBoxesToBoxes(boxes)
                            )
                        }
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

    fun selectDevice(box: QualityBox) {
        viewModelScope.launch {
            _state.update { it.copy(connectingBoxName = box.name) }
            NetworkConfig.nombreconfiguracion = box.mac
            NetworkConfig.configuracion = "mac"
            dataStoreRepository.saveIdCaja(box.id)
            sensorReceiveManager.startReceiving()
        }
    }

    fun logout() {
        viewModelScope.launch {
            logoutEvent.value = ProcessingEvent.Loading
            val deleteContainers = async { containerRepository.deleteContainer() }
            val deleteUsers = async { userRepository.deleteUser() }
            if (deleteContainers.await().isSuccess && deleteUsers.await().isSuccess) {
                logoutEvent.value = ProcessingEvent.Success
            } else {
                logoutEvent.value = ProcessingEvent.Error
            }
        }
    }

    fun dismissLogoutError() {
        logoutEvent.value = ProcessingEvent.Idle
    }

    private fun rawBoxesToBoxes(rawBoxes: String): List<QualityBox> {
        return rawBoxes.split("|").mapNotNull { data ->
            val boxInfo = data.split(" ")
            val boxDataConnection = boxInfo.first().split(",")
            val boxId = boxDataConnection.first().toIntOrNull()
            if (boxInfo.size >= 2 && boxId != null) {
                QualityBox(
                    id = boxId,
                    mac = "10:06:1C:71:80:1E",
                    name = boxInfo.drop(1).joinToString(" ")
                )
            } else null
        }
    }
}