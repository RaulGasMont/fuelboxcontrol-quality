package com.gasmonsoft.fuelboxcontrol.ui.calibracion.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gasmonsoft.fuelboxcontrol.data.model.ble.SensorState
import com.gasmonsoft.fuelboxcontrol.data.model.calibracion.Calibracion
import com.gasmonsoft.fuelboxcontrol.data.repository.api.FuelSoftwareControlRepository
import com.gasmonsoft.fuelboxcontrol.data.repository.ble.SensorReceiveManager
import com.gasmonsoft.fuelboxcontrol.data.service.firmware.NTF_ACK
import com.gasmonsoft.fuelboxcontrol.data.service.firmware.UpgradeFileType
import com.gasmonsoft.fuelboxcontrol.domain.calibracion.CalibrationUseCaseImpl
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CalibrationViewModel @Inject constructor(
    private val sensorReceiveManager: SensorReceiveManager,
    private val calibrationUseCase: CalibrationUseCaseImpl,
    private val repository: FuelSoftwareControlRepository
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
            if (_calibrationUiState.value.selectedSensor == null) return@launch
            if (_calibrationUiState.value.measurements.isEmpty()) return@launch

            _calibrationUiState.update { currentUiState ->
                currentUiState.copy(
                    calibrationEvent = SenderCalibrationEvent.Calibrating
                )
            }

            val regressionResponse = calibrationUseCase(_calibrationUiState.value.measurements)

            if (regressionResponse == null) {
                _calibrationUiState.update { currentUiState ->
                    currentUiState.copy(
                        calibrationEvent = SenderCalibrationEvent.Error(
                            message = "No se pudo realizar el análisis de regresión",
                            title = "Error en el análisis de regresión"
                        )
                    )
                }
                return@launch
            }


            _calibrationUiState.update { currentUiState ->
                currentUiState.copy(
                    calibrationEvent = SenderCalibrationEvent.Loading
                )
            }
            val response = repository.getDatFile(
                Calibracion(
                    tendencias = regressionResponse.tendencias,
                    capacidad = _calibrationUiState.value.capacidad,
                    capacitancia = _calibrationUiState.value.capacitancia.toInt(),
                    intervalos = regressionResponse.tendencias.size,
                    coefPolinomial = regressionResponse.coefPolinomial
                )
            )

            if (response.isSuccess) {
                val data = response.getOrNull() ?: return@launch
                sendToBox(data)
            } else {
                _calibrationUiState.update { currentUiState ->
                    currentUiState.copy(
                        calibrationEvent = SenderCalibrationEvent.Error(
                            "Error al obtener el archivo de datos",
                            "Error"
                        )
                    )
                }
            }


        }
    }

    private suspend fun sendToBox(data: ByteArray) {
        _calibrationUiState.update { currentUiState ->
            currentUiState.copy(
                calibrationEvent = SenderCalibrationEvent.Updating
            )
        }
        val sensorId = _calibrationUiState.value.selectedSensor?.id ?: return
        val (result, msg) = sensorReceiveManager.sendConfFile(
            data = data,
            name = "0000.dat",
            sensorId = sensorId,
            upgradeType = UpgradeFileType.DATA
        )

        if (result == null) {
            _calibrationUiState.update { currentUiState ->
                currentUiState.copy(
                    calibrationEvent = SenderCalibrationEvent.Error(
                        message = msg,
                        title = "Error en la actualización"
                    )
                )
            }
            return
        }

        if (result.type == NTF_ACK) {
            _calibrationUiState.update { currentUiState ->
                currentUiState.copy(
                    calibrationEvent = SenderCalibrationEvent.Success
                )
            }
        } else {
            _calibrationUiState.update { currentUiState ->
                currentUiState.copy(
                    calibrationEvent = SenderCalibrationEvent.Error(
                        message = msg,
                        title = "Error en la actualización"
                    )
                )
            }
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

    fun restarCalibrationState() {
        _calibrationUiState.update { currentUiState ->
            currentUiState.copy(
                calibrationEvent = SenderCalibrationEvent.Idle

            )
        }
    }

    fun saveCapacidadAndCapacitancia(capacidad: String, capacitancia: String) {
        val capacity = capacidad.toDoubleOrNull() ?: 0.0
        val capacitance = capacitancia.toDoubleOrNull() ?: 0.0

        _calibrationUiState.update { currentUiState ->
            currentUiState.copy(
                capacidad = capacity,
                capacitancia = capacitance
            )
        }
    }
}