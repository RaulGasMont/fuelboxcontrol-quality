package com.gasmonsoft.fuelboxcontrol.ui.detector.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gasmonsoft.fuelboxcontrol.data.repository.ble.SensorReceiveManager
import com.gasmonsoft.fuelboxcontrol.data.repository.datastore.DataStoreRepository
import com.gasmonsoft.fuelboxcontrol.domain.detector.DetectorUseCase
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
class DetectorViewModel @Inject constructor(
    sensorReceiveManager: SensorReceiveManager,
    private val detectorUseCase: DetectorUseCase,
    dataStoreRepository: DataStoreRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(DetectorUiState())
    val uiState: StateFlow<DetectorUiState> = _uiState.asStateFlow()

    val selectedTankState = dataStoreRepository.selectedTank.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        null
    )

    private val sensorData = sensorReceiveManager.sensorData

    init {
        viewModelScope.launch {
            selectedTankState.collect {
                if (it != null) {
                    _uiState.update { currentUiState ->
                        currentUiState.copy(
                            tankId = it.tankId,
                            tankType = it.tankType,
                            tankName = it.nameTankId.ifEmpty { "Tanque ${it.tankId}" }
                        )
                    }
                }
            }
        }
    }

    fun analyzeData() {
        viewModelScope.launch {
            val result = detectorUseCase(sensorData)
            _uiState.update { currentUiState ->
                currentUiState.copy(
                    fuelType = result.type,
                    level = result.value ?: 0.0f
                )
            }
        }
    }
}