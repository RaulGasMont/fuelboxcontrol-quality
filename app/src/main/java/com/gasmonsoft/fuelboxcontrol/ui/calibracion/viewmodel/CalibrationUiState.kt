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
    val excelReadingEvent: GeneralEvent = GeneralEvent.Idle,
    val capacidad: Double = 0.0,
    val capacitancia: Double = 0.0,
    val datFile: ByteArray? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CalibrationUiState

        if (capacidad != other.capacidad) return false
        if (capacitancia != other.capacitancia) return false
        if (sensors != other.sensors) return false
        if (selectedSensor != other.selectedSensor) return false
        if (connectionState != other.connectionState) return false
        if (initializingMessage != other.initializingMessage) return false
        if (errorMessage != other.errorMessage) return false
        if (measurements != other.measurements) return false
        if (currentSensorValue != other.currentSensorValue) return false
        if (calibrationEvent != other.calibrationEvent) return false
        if (!datFile.contentEquals(other.datFile)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = capacidad.hashCode()
        result = 31 * result + capacitancia.hashCode()
        result = 31 * result + sensors.hashCode()
        result = 31 * result + (selectedSensor?.hashCode() ?: 0)
        result = 31 * result + connectionState.hashCode()
        result = 31 * result + (initializingMessage?.hashCode() ?: 0)
        result = 31 * result + (errorMessage?.hashCode() ?: 0)
        result = 31 * result + measurements.hashCode()
        result = 31 * result + currentSensorValue.hashCode()
        result = 31 * result + calibrationEvent.hashCode()
        result = 31 * result + (datFile?.contentHashCode() ?: 0)
        return result
    }
}

sealed class GeneralEvent(open val title: String, open val message: String) {
    data object Idle : GeneralEvent(title = "Sin evento", message = "")
    data object Loading : GeneralEvent(title = "Cargando...", message = "")
    data class Error(override val message: String) :
        GeneralEvent(title = "Ocurrió un problema", message = message)

    data object Success : GeneralEvent(title = "Datos Extraidos", message = "")
}

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