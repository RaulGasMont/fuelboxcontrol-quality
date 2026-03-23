package com.gasmonsoft.fuelboxcontrol.ui.calibracion.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gasmonsoft.fuelboxcontrol.data.ble.SensorReceiveManager
import com.gasmonsoft.fuelboxcontrol.data.ble.SensorState
import com.gasmonsoft.fuelboxcontrol.domain.calibracion.CalibrationUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class CalibrationSensor(val value: String) {
    SENSOR_1("Sensor 1"),
    SENSOR_2("Sensor 2"),
    SENSOR_3("Sensor 3"),
    SENSOR_4("Sensor 4"),
}

data class CalibrationUiState(
    val sensors: List<CalibrationSensor> = listOf(
        CalibrationSensor.SENSOR_1,
        CalibrationSensor.SENSOR_2,
        CalibrationSensor.SENSOR_3,
        CalibrationSensor.SENSOR_4
    ),
    val selectedSensor: CalibrationSensor? = null,
    val measurements: List<Pair<String, String>> = emptyList(),
    val currentSensorValue: String = "",
)

@HiltViewModel
class CalibrationViewModel @Inject constructor(
    sensorReceiveManager: SensorReceiveManager,
    private val calibrationUseCase: CalibrationUseCase
) : ViewModel() {

    private val sensorData = sensorReceiveManager.sensorData.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = SensorState()
    )
    private var _calibrationUiState = MutableStateFlow(CalibrationUiState())
    val calibrationUiState = _calibrationUiState.asStateFlow()

    init {
        observeSensorData()
    }

    fun observeSensorData() {
        viewModelScope.launch {
            sensorData.collect { sensorState ->
                val result = when (_calibrationUiState.value.selectedSensor) {
                    CalibrationSensor.SENSOR_1 -> sensorState.sensor1
                    CalibrationSensor.SENSOR_2 -> sensorState.sensor2
                    CalibrationSensor.SENSOR_3 -> sensorState.sensor3
                    CalibrationSensor.SENSOR_4 -> sensorState.sensor4
                    else -> null
                }

                if (result != null) {
                    _calibrationUiState.update { currentUiState ->
                        currentUiState.copy(
                            currentSensorValue = result.volumen
                        )
                    }
                }
            }
        }
    }

    fun selectSensor(sensor: CalibrationSensor) {
        _calibrationUiState.update { currentUiState ->
            currentUiState.copy(
                selectedSensor = sensor,
                measurements = emptyList(),
                currentSensorValue = ""
            )
        }
    }

    fun takeMeasurement(litos: String, valor: String) {
        _calibrationUiState.update { currentUiState ->
            val measurements = currentUiState.measurements.toMutableList()
            measurements.add(Pair(litos, valor))
            currentUiState.copy(
                measurements = measurements
            )
        }
    }

    fun startAnalise() {
        viewModelScope.launch {
            if (_calibrationUiState.value.measurements.isEmpty()) return@launch
            calibrationUseCase(_calibrationUiState.value.measurements)
        }
    }

    fun eliminarUltimaMedicion() {
        _calibrationUiState.update { currentUiState ->
            val measurements = currentUiState.measurements.toMutableList()
            measurements.removeLastOrNull()
            currentUiState.copy(
                measurements = measurements
            )
        }
    }
}