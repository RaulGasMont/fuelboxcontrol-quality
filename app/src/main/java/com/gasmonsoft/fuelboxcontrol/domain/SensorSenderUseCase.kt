package com.gasmonsoft.fuelboxcontrol.domain

import com.gasmonsoft.fuelboxcontrol.data.repository.ble.FuelSoftwareControlRepository
import com.gasmonsoft.fuelboxcontrol.model.sensor.SensorDataUnitario
import com.gasmonsoft.fuelboxcontrol.model.sensor.SensorInfo
import com.gasmonsoft.fuelboxcontrol.ui.vehiculo.viewmodel.SensorSendingEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject

class SensorSenderUseCase @Inject constructor(
    private val repository: FuelSoftwareControlRepository
) {
    private var _sensorSenderStatus: MutableStateFlow<SensorSendingEvent> =
        MutableStateFlow(SensorSendingEvent.Idle)
    val sensorSenderStatus = _sensorSenderStatus.asStateFlow()

    val sensorInfo = repository.sensorPackages

    suspend operator fun invoke(
        token: String,
        idUsuario: Int,
        idCaja: String,
        data: SensorPackage
    ) {
        _sensorSenderStatus.update { SensorSendingEvent.Loading }
        val idCajaComunicaciones = idCaja.toIntOrNull() ?: return
        repository.sendSensorData(
            SensorInfo(
                token = token,
                data = SensorDataUnitario(
                    idCajaComunicaciones = idCajaComunicaciones,
                    idUsuario = idUsuario,
                    fecha = convertDate(
                        date = data.date,
                        initialFormat = "yyyy/MM/dd HH:mm:ss",
                        convertFormat = "yyyy-MM-dd'T'HH:mm:ss"
                    ),
                    tipoUsuario = false,
                    jsonData = data.data
                )
            )
        ).fold(
            onSuccess = {
                _sensorSenderStatus.update { SensorSendingEvent.Success(message = "Datos enviados correctamente.") }
            },
            onFailure = {
                val result = it.message ?: "Error desconocido"
                _sensorSenderStatus.update { SensorSendingEvent.Error(message = result) }
            }
        )
    }

    private fun convertDate(
        date: String,
        initialFormat: String = "yyyy-MM-dd'T'HH:mm:ss",
        convertFormat: String = "dd/MM/yyyy HH:mm:ss"
    ): String {
        val sdf = SimpleDateFormat(initialFormat, Locale.getDefault())
        val dateValue = sdf.parse(date)!!
        val outDate = SimpleDateFormat(convertFormat, Locale.getDefault())
        return outDate.format(dateValue)
    }
}
