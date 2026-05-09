package com.gasmonsoft.fbccalidad.data.service.sensor

import com.gasmonsoft.fbccalidad.data.service.ble.FileReadEvent
import com.gasmonsoft.fbccalidad.data.service.usb.DataSource
import kotlinx.coroutines.flow.Flow

interface SensorDataSource {
    val incomingFileData: Flow<FileReadEvent>
    suspend fun startReading()
    fun isConnected(): Boolean
    val dataSourceType: DataSource
}