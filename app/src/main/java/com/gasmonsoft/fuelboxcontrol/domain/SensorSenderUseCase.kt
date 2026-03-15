package com.gasmonsoft.fuelboxcontrol.domain

import com.gasmonsoft.fuelboxcontrol.data.repository.FuelSoftwareControlRepository
import com.gasmonsoft.fuelboxcontrol.model.sensor.SensorDataUnitario
import com.gasmonsoft.fuelboxcontrol.model.sensor.SensorInfo
import com.gasmonsoft.fuelboxcontrol.ui.vehiculo.SensorSendingStatus
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject

class SensorSenderUseCase @Inject constructor(
    private val repository: FuelSoftwareControlRepository
) {
    private var _sensorSenderStatus = MutableSharedFlow<SensorSendingStatus>()
    val sensorSenderStatus = _sensorSenderStatus.asSharedFlow()

    val sensorInfo = repository.sensorPackages

    suspend operator fun invoke(
        token: String,
        idUsuario: Int,
        idCaja: String,
        data: SensorPackage
    ) {
        _sensorSenderStatus.emit(SensorSendingStatus.SENDING)
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
                _sensorSenderStatus.emit(SensorSendingStatus.SENT)
            },
            onFailure = {
                _sensorSenderStatus.emit(SensorSendingStatus.ERROR)
            }
        )
    }

    private fun convertDate(
        date: String,
        initialFormat: String = "yyyy-MM-dd'T'HH:mm:ss",
        convertFormat: String = "dd/MM/yyyy HH:mm:ss"
    ): String {
        val sdf = SimpleDateFormat(initialFormat, Locale.getDefault())
        val date = sdf.parse(date)!!
        val outDate = SimpleDateFormat(convertFormat, Locale.getDefault())
        return outDate.format(date)
    }
}

