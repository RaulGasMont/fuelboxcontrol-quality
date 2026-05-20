package com.gasmonsoft.fbccalidad.ui.sensor.viewmodel.calibrate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gasmonsoft.fbccalidad.data.repository.ble.SensorReceiveManager
import com.gasmonsoft.fbccalidad.utils.LoadState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import kotlin.math.floor
import kotlin.time.Duration.Companion.seconds

data class CalibrationUiState(
    val initialCalibration: LoadState = LoadState.Idle,
    val refineCalibration: LoadState = LoadState.Idle,
    val gettingQualityFuelValue: LoadState = LoadState.Idle,
    val gettingAirValue: LoadState = LoadState.Idle,
    val qualityData: Double? = null,
    val aireData: Double? = null,
)

@HiltViewModel
class CalibrationViewModel @Inject constructor(private val sensorReceiveManager: SensorReceiveManager) :
    ViewModel() {
    private var _uiState = MutableStateFlow(CalibrationUiState())
    val uiState = _uiState.asStateFlow()

    private val sensorData = sensorReceiveManager.sensorData
    private val tonSensorData = mutableListOf<String>()
    private val tonAirData = mutableListOf<String>()
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
                delay(10.seconds)
                _uiState.update { it.copy(initialCalibration = LoadState.Success) }
            } else {
                _uiState.update { it.copy(initialCalibration = LoadState.Error) }
            }
        }
    }

    fun getQualityValue() {
        viewModelScope.launch {
            _uiState.update { it.copy(gettingQualityFuelValue = LoadState.Loading) }
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
                _uiState.update {
                    it.copy(
                        gettingQualityFuelValue = LoadState.Success,
                        qualityData = averageQuality
                    )
                }
            } else {
                _uiState.update { it.copy(gettingQualityFuelValue = LoadState.Error) }
            }
        }
    }

    fun getAirValue() {
        viewModelScope.launch {
            _uiState.update { it.copy(gettingAirValue = LoadState.Loading) }
            tonAirData.clear()
            withTimeoutOrNull(10_000L) {
                sensorData.map { it.sensor1 }.collect { data ->
                    if (!data.error) {
                        tonAirData.add(data.calidad)
                    }
                }
            }
            val averageAirValue = tonAirData.mapNotNull { it.toDoubleOrNull() }.average()
            if (!averageAirValue.isNaN()) {
                _uiState.update {
                    it.copy(
                        gettingAirValue = LoadState.Success,
                        aireData = averageAirValue
                    )
                }
            } else {
                _uiState.update { it.copy(gettingAirValue = LoadState.Error) }
            }
        }
    }

    fun proCalibrate() {
        val quality = _uiState.value.qualityData
        val air = _uiState.value.aireData

        if (quality != null && air != null) {
            viewModelScope.launch {
                _uiState.update { it.copy(refineCalibration = LoadState.Loading) }

                val diferencia = quality - air
                val a1 = air - diferencia
                val a2 = (a1 / 5900.0) * 0.1

                // a3 = redondeo a menos a un decimal (ej: 16.5485 -> 16.5)
                val a3 = floor(a2 * 10) / 10.0

                sensorReceiveManager.writeInitialValuese(
                    valor = "C04 $a3",
                    opcion = 10
                )
                delay(10.seconds)

                val b2 = (5900.0 * a3 * 10.0) - diferencia

                // Resultado final: b2 redondeado a menos sin decimales
                val finalValue = floor(b2).toInt()

                sensorReceiveManager.writeInitialValuese(
                    valor = "C09 $finalValue",
                    opcion = 10
                )
                delay(10.seconds)
                _uiState.update { it.copy(refineCalibration = LoadState.Success) }
            }
        } else {
            _uiState.update {
                it.copy(refineCalibration = LoadState.Error)
            }
        }
    }
}