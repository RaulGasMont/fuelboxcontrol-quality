package com.gasmonsoft.fuelboxcontrol.ui.detector.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gasmonsoft.fuelboxcontrol.data.model.sensor.SensorCalidadData
import com.gasmonsoft.fuelboxcontrol.data.repository.api.FuelSoftwareControlRepository
import com.gasmonsoft.fuelboxcontrol.data.repository.ble.SensorReceiveManager
import com.gasmonsoft.fuelboxcontrol.data.repository.datastore.DataStoreRepository
import com.gasmonsoft.fuelboxcontrol.domain.detector.DetectorUseCase
import com.gasmonsoft.fuelboxcontrol.utils.ProcessingEvent
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
    private val fscApiRepository: FuelSoftwareControlRepository,
    dataStoreRepository: DataStoreRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(DetectorUiState())
    val uiState: StateFlow<DetectorUiState> = _uiState.asStateFlow()

    val selectedTankState = dataStoreRepository.selectedTank.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        null
    )


    val selectedBox = dataStoreRepository.selectedCaja.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        null
    )

    private val sensorData = sensorReceiveManager.sensorData

    init {
        observeTankSelection()
        observeBoxSelection()
    }

    fun observeBoxSelection() {
        viewModelScope.launch {
            selectedBox.collect {
                if (it != null) {
                    _uiState.update { currentUiState ->
                        currentUiState.copy(
                            idCaja = it
                        )
                    }
                }
            }
        }
    }

    fun observeTankSelection() {
        viewModelScope.launch {
            viewModelScope.launch {
                selectedTankState.collect {
                    if (it != null) {
                        _uiState.update { currentUiState ->
                            currentUiState.copy(
                                tankId = it.tankId,
                                tankType = it.tankType,
                                tankName = it.tankName.ifEmpty { "Tanque ${it.tankId}" }
                            )
                        }
                    }
                }
            }
        }
    }

    fun analyzeData() {
        updateDetectionEvent(ProcessingEvent.Loading)
        viewModelScope.launch {
            val result = detectorUseCase(sensorData)

            fscApiRepository.sendSensorCalidadData(
                SensorCalidadData(
                    idCajaCalidad = _uiState.value.idCaja,
                    idTipoContenedor = _uiState.value.tankType,
                    calidad = result.value?.toDouble() ?: 0.0,
                    temperatura = result.temperature?.toDouble() ?: 0.0
                )
            )

            _uiState.update { currentUiState ->
                currentUiState.copy(
                    fuelType = result.type,
                    valueDetection = result.value ?: 0.0f
                )
            }

            updateDetectionEvent(ProcessingEvent.Success)
        }
    }

    fun updateDetectionEvent(status: ProcessingEvent) {
        _uiState.update { currentUiState ->
            currentUiState.copy(
                detectionEvent = status
            )
        }
    }
}