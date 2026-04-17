package com.gasmonsoft.fbccalidad.ui.home.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gasmonsoft.fbccalidad.data.model.boxlogin.QualityBox
import com.gasmonsoft.fbccalidad.data.repository.ble.SensorReceiveManager
import com.gasmonsoft.fbccalidad.data.repository.container.ContainerRepository
import com.gasmonsoft.fbccalidad.data.repository.datastore.DataStoreRepository
import com.gasmonsoft.fbccalidad.data.repository.user.UserRepository
import com.gasmonsoft.fbccalidad.domain.session.SessionUseCase
import com.gasmonsoft.fbccalidad.utils.NetworkConfig
import com.gasmonsoft.fbccalidad.utils.ProcessingEvent
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
    val connectingBoxName: String? = null,
    val isConnecting: Boolean = false
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

    fun setAsNoConnecting() {
        _state.update { currentUiState ->
            currentUiState.copy(isConnecting = false)
        }
    }

    fun selectDevice(box: QualityBox) {
        viewModelScope.launch {
            _state.update { it.copy(connectingBoxName = box.name, isConnecting = true) }
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
                    mac = "10:06:1C:71:80:16",
                    name = boxInfo.drop(1).joinToString(" ")
                )
            } else null
        }
    }
}