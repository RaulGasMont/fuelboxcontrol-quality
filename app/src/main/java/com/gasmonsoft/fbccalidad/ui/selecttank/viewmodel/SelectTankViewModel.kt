package com.gasmonsoft.fbccalidad.ui.selecttank.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gasmonsoft.fbccalidad.data.model.selectvehicle.Tank
import com.gasmonsoft.fbccalidad.data.model.selectvehicle.toTankSelection
import com.gasmonsoft.fbccalidad.data.repository.datastore.DataStoreRepository
import com.gasmonsoft.fbccalidad.domain.containers.ContainersUseCase
import com.gasmonsoft.fbccalidad.utils.LoadState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SelectTankViewModel @Inject constructor(
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
                    seeTankEvent = LoadState.Loading
                )
            }

            containersUseCase().fold(
                onSuccess = {
                    _uiState.update { currentUiState ->
                        currentUiState.copy(
                            seeTankEvent = LoadState.Success,
                            vehicles = it.vehicleList,
                            otros = it.othersList
                        )
                    }
                },
                onFailure = {
                    _uiState.update { currentUiState ->
                        currentUiState.copy(
                            seeTankEvent = LoadState.Error
                        )
                    }
                }
            )
        }
    }

    fun dismissedError() {
        _uiState.update { currentUiState ->
            currentUiState.copy(
                seeTankEvent = LoadState.Idle
            )
        }
    }

    fun saveSelectedTank(tank: Tank) {
        viewModelScope.launch {
            _uiState.update { currentUiState ->
                currentUiState.copy(
                    saveTankEvent = LoadState.Loading
                )
            }

            dataStoreRepository.saveTank(
                tank = tank.toTankSelection()
            )

            _uiState.update { currentUiState ->
                currentUiState.copy(
                    saveTankEvent = LoadState.Success
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