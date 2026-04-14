package com.gasmonsoft.fuelboxcontrol.ui.selecttank.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gasmonsoft.fuelboxcontrol.data.model.selectvehicle.Tank
import com.gasmonsoft.fuelboxcontrol.data.model.selectvehicle.toTankSelection
import com.gasmonsoft.fuelboxcontrol.data.repository.datastore.DataStoreRepository
import com.gasmonsoft.fuelboxcontrol.data.repository.user.UserRepository
import com.gasmonsoft.fuelboxcontrol.domain.containers.ContainersUseCase
import com.gasmonsoft.fuelboxcontrol.utils.ProcessingEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SelectTankViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val containersUseCase: ContainersUseCase,
    private val dataStoreRepository: DataStoreRepository
) :
    ViewModel() {
    private val _uiState = MutableStateFlow(SelectTankUiState())
    val uiState: StateFlow<SelectTankUiState> = _uiState.asStateFlow()

    fun getTankList() {
        viewModelScope.launch {
            _uiState.update { currentUiState ->
                currentUiState.copy(
                    seeTankEvent = ProcessingEvent.Loading
                )
            }

            userRepository.getContainers()
                .onSuccess {
                    val result = containersUseCase(it)
                    _uiState.update { currentUiState ->
                        currentUiState.copy(
                            seeTankEvent = ProcessingEvent.Success,
                            vehicles = result.vehicleList,
                            otros = result.othersList
                        )
                    }
                }
                .onFailure {
                    _uiState.update { currentUiState ->
                        currentUiState.copy(
                            seeTankEvent = ProcessingEvent.Error
                        )
                    }
                }
        }
    }

    fun dismissedError() {
        _uiState.update { currentUiState ->
            currentUiState.copy(
                seeTankEvent = ProcessingEvent.Error
            )
        }
    }

    fun saveSelectedTank(tank: Tank) {
        viewModelScope.launch {
            _uiState.update { currentUiState ->
                currentUiState.copy(
                    saveTankEvent = ProcessingEvent.Loading
                )
            }

            dataStoreRepository.saveTank(
                tank = tank.toTankSelection()
            )

            _uiState.update { currentUiState ->
                currentUiState.copy(
                    saveTankEvent = ProcessingEvent.Success
                )
            }
        }
    }

    fun changeScreen(screen: SelectTankScreen) {
        val screenToChange = if (screen == SelectVehicleScreen.Vehicles()) {
            SelectVehicleScreen.Vehicles()
        } else {
            SelectVehicleScreen.Otros()
        }
        _uiState.update { currentUiState ->
            currentUiState.copy(
                screen = screenToChange
            )
        }
    }
}