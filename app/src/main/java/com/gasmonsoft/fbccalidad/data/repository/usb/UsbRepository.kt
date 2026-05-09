package com.gasmonsoft.fbccalidad.data.repository.usb

import android.hardware.usb.UsbDevice
import com.gasmonsoft.fbccalidad.data.datasource.usb.UsbOtgEventSource
import com.gasmonsoft.fbccalidad.data.model.ble.SensorData
import com.gasmonsoft.fbccalidad.data.model.ble.SensorState
import com.gasmonsoft.fbccalidad.data.repository.ble.getSensorData
import com.gasmonsoft.fbccalidad.data.service.usb.OtgSensorDataSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UsbRepository @Inject constructor(
    private val otgSensorDataSource: OtgSensorDataSource,
    private val usbOtgEventSource: UsbOtgEventSource,
) {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var collectionJob: Job? = null
    private var _sensorData = MutableSharedFlow<SensorState>()
    val sensorData = _sensorData.asSharedFlow()

    suspend fun connect(device: UsbDevice): Boolean =
        kotlinx.coroutines.withContext(Dispatchers.IO) {
            usbOtgEventSource.connect(device)
        }

    fun disconnect() = usbOtgEventSource.disconnect()

    suspend fun startReading() {
        //otgSensorDataSource.startReading()
        startCollecting()
    }

    private fun startCollecting() {
        if (collectionJob?.isActive == true) return
        collectionJob = serviceScope.launch {
            usbOtgEventSource.events.collect { sensorData ->
                processData(sensorData.value)
            }
        }
    }

    suspend fun processData(value: ByteArray) {
        val rawString = value.toString(Charsets.UTF_8).trim()
        val sensorData = rawString.split(" ")

        if (sensorData.size == 5) {
            _sensorData.emit(SensorState(getSensorData(sensorData)))
        } else if (rawString.startsWith("cal:") && sensorData.size >= 3) {
            // cal:2026-05-09 11:59:12 2.1
            val date = sensorData[0].removePrefix("cal:") + " " + sensorData[1]
            val calidad = sensorData.last()
            _sensorData.emit(
                SensorState(
                    SensorData(
                        date = date,
                        calidad = calidad,
                        temperatura = "0.0",
                        rawData = rawString
                    )
                )
            )
        }
    }
}