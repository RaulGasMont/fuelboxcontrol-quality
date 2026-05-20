package com.gasmonsoft.fbccalidad.ui.sensor.viewmodel.calibrate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gasmonsoft.fbccalidad.data.repository.ble.SensorReceiveManager
import com.gasmonsoft.fbccalidad.utils.LoadState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

data class CalibrationUiState(
    val initialCalibration: LoadState = LoadState.Idle,
    val refineCalibration: LoadState = LoadState.Idle,
    val fuelData: Double? = null,
    val aireData: Double? = null,
)

@HiltViewModel
class CalibrationViewModel @Inject constructor(private val sensorReceiveManager: SensorReceiveManager) :
    ViewModel() {
    private var _uiState = MutableStateFlow(CalibrationUiState())
    val uiState = _uiState.asStateFlow()

    private val sensorData = sensorReceiveManager.sensorData
    private val tonSensorData = mutableListOf<String>()
    fun calibrate() {
        viewModelScope.launch {
            _uiState.update { it.copy(initialCalibration = LoadState.Loading) }
            tonSensorData.clear()

            withTimeoutOrNull(10_000L) {
                sensorData.map { it.sensor1 }.collect { data ->
                    if (!data.error) {
                        tonSensorData.add(data.calidad)
                    }
                }
            }

            val averageQuality = tonSensorData
                .mapNotNull { it.toDoubleOrNull() }
                .average()

            if (!averageQuality.isNaN()) {
                sensorReceiveManager.writeInitialValuese(
                    valor = averageQuality.toString(),
                    opcion = 9
                )
                _uiState.update { it.copy(initialCalibration = LoadState.Success) }
            } else {
                _uiState.update { it.copy(initialCalibration = LoadState.Error) }
            }
        }
    }

    fun proCalibrate() {
        sensorReceiveManager.writeInitialValuese(
            valor = "",
            opcion = 10
        )
    }
}