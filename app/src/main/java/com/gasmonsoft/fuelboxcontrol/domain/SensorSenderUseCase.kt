package com.gasmonsoft.fuelboxcontrol.domain

import com.gasmonsoft.fuelboxcontrol.data.repository.FuelSoftwareControlRepository
import com.gasmonsoft.fuelboxcontrol.model.sensor.SensorDataUnitario
import com.gasmonsoft.fuelboxcontrol.model.sensor.SensorInfo
import com.gasmonsoft.fuelboxcontrol.ui.vehiculo.SensorSendingStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject

class SensorSenderUseCase @Inject constructor(
    private val repository: FuelSoftwareControlRepository
) {
    private var _sensorSenderStatus = MutableStateFlow(SensorSendingStatus.NOT_SENT)
    val sensorSenderStatus = _sensorSenderStatus.asStateFlow()

    val sensorInfo = repository.sensorPackages

    suspend operator fun invoke(
        token: String,
        idUsuario: Int,
        idCaja: String,
        data: SensorPackage
    ) {
        _sensorSenderStatus.value = SensorSendingStatus.SENDING
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
                _sensorSenderStatus.value = SensorSendingStatus.SENT
            },
            onFailure = {
                _sensorSenderStatus.value = SensorSendingStatus.ERROR
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
