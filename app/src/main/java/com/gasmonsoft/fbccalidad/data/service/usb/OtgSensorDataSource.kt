package com.gasmonsoft.fbccalidad.data.service.usb

import com.gasmonsoft.fbccalidad.data.datasource.usb.UsbOtgEventSource
import com.gasmonsoft.fbccalidad.data.repository.ble.SensorReceiveManager
import com.gasmonsoft.fbccalidad.data.service.ble.FileReadEvent
import com.gasmonsoft.fbccalidad.data.service.sensor.SensorDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import javax.inject.Inject
import javax.inject.Singleton

enum class DataSource { BLE, OTG }

@Singleton
class OtgSensorDataSource @Inject constructor(
    private val usbOtgEventSource: UsbOtgEventSource,
    private val bleReceiverManager: SensorReceiveManager
) : SensorDataSource {

    override val incomingFileData: Flow<FileReadEvent>
        get() = merge(
            usbOtgEventSource.events,
            usbOtgEventSource.errors.map { error ->
                throw Exception(error)
            }
        )

    override suspend fun startReading() {
        bleReceiverManager.writeInitialValuese("", 8)
    }

    override fun isConnected(): Boolean =
        usbOtgEventSource.isConnected

    override val dataSourceType = DataSource.OTG
}