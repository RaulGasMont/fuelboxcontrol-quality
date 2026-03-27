package com.gasmonsoft.fuelboxcontrol.ui.calibracion.viewmodel

import com.gasmonsoft.fuelboxcontrol.data.model.ble.ConnectionState


enum class CalibrationSensor(val value: String, val id: String) {
    SENSOR_1("Sensor 1", "1"),
    SENSOR_2("Sensor 2", "2"),
    SENSOR_3("Sensor 3", "3"),
    SENSOR_4("Sensor 4", "4"),
}

data class CalibrationUiState(
    val sensors: List<CalibrationSensor> = listOf(
        CalibrationSensor.SENSOR_1,
        CalibrationSensor.SENSOR_2,
        CalibrationSensor.SENSOR_3,
        CalibrationSensor.SENSOR_4
    ),
    val selectedSensor: CalibrationSensor? = null,
    val connectionState: ConnectionState = ConnectionState.Uninitialized,
    val initializingMessage: String? = null,
    val errorMessage: String? = null,
    val measurements: List<Pair<String, String>> = emptyList(),
    val currentSensorValue: String = "",
    val calibrationEvent: SenderCalibrationEvent = SenderCalibrationEvent.Idle,
    val capacidad: Double = 0.0,
    val capacitancia: Double = 0.0
)

sealed class SenderCalibrationEvent(open val title: String, open val message: String) {
    data object Idle : SenderCalibrationEvent(title = "No enviado", message = "")
    data object Calibrating : SenderCalibrationEvent(title = "Calibrando...", message = "")
    data object Loading : SenderCalibrationEvent(title = "Enviando...", message = "")
    data object Updating : SenderCalibrationEvent(title = "Actualizando...", message = "")
    data class Error(
        override val message: String,
        override val title: String = "Error del servidor"
    ) : SenderCalibrationEvent(title = title, message = message)

    data object Success : SenderCalibrationEvent(title = "Sensor calibrado.", message = "")
}