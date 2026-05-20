package com.gasmonsoft.fbccalidad.ui.home.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gasmonsoft.fbccalidad.data.model.boxlogin.QualityBox
import com.gasmonsoft.fbccalidad.data.repository.ble.SensorReceiveManager
import com.gasmonsoft.fbccalidad.data.repository.container.ContainerRepository
import com.gasmonsoft.fbccalidad.data.repository.datastore.DataStoreRepository
import com.gasmonsoft.fbccalidad.data.repository.user.UserRepository
import com.gasmonsoft.fbccalidad.domain.session.SessionUseCase
import com.gasmonsoft.fbccalidad.utils.LoadState
import com.gasmonsoft.fbccalidad.utils.NetworkConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeState(
    val logoutEvent: LoadState = LoadState.Idle,
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

    var logoutEvent: MutableStateFlow<LoadState> = MutableStateFlow(LoadState.Idle)
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
            logoutEvent.value = LoadState.Loading
            val deleteContainers = async { containerRepository.deleteContainer() }
            val deleteUsers = async { userRepository.deleteUser() }
            if (deleteContainers.await().isSuccess && deleteUsers.await().isSuccess) {
                logoutEvent.value = LoadState.Success
            } else {
                logoutEvent.value = LoadState.Error
            }
        }
    }

    fun dismissLogoutError() {
        logoutEvent.value = LoadState.Idle
    }

    private fun rawBoxesToBoxes(rawBoxes: String): List<QualityBox> {
        return rawBoxes.split("|").mapNotNull { data ->
            val boxInfo = data.split(" ")
            val boxDataConnection = boxInfo.first().split(",")
            if (boxDataConnection.isEmpty()) return@mapNotNull null
            val mac = boxDataConnection[1]
            val boxId = boxDataConnection.first().toIntOrNull()
            if (boxInfo.isNotEmpty() && boxId != null) {
                QualityBox(
                    id = boxId,
                    mac = "10:06:1C:71:80:8E",
                    name = "Caja $boxId"
                )
            } else null
        }
    }
}