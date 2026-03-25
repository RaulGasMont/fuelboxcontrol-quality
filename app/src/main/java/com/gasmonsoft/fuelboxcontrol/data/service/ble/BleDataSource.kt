package com.gasmonsoft.fuelboxcontrol.data.service.ble

import com.gasmonsoft.fuelboxcontrol.data.repository.ble.SensorReceiveManager
import com.gasmonsoft.fuelboxcontrol.domain.sensor.SensorCollectorUseCase
import com.gasmonsoft.fuelboxcontrol.domain.sensor.SensorPackage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BleDataSource @Inject constructor(
    private val bleManager: SensorReceiveManager,
    private val sensorCollectorUseCase: SensorCollectorUseCase
) {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var _sensorInfo = MutableSharedFlow<SensorPackage>()
    val sensorState = _sensorInfo.asSharedFlow()

    private var _logDataframe = MutableSharedFlow<String>()
    val logDataframe = _logDataframe.asSharedFlow()

    init {
        startCollecting()
    }

    private fun startCollecting() {
        serviceScope.launch {
            bleManager.sensorEvents.collect { sensorEvent ->
                if (sensorEvent.type == null) return@collect
                sensorCollectorUseCase(sensorEvent.type, sensorEvent.value)
            }
        }

        serviceScope.launch {
            sensorCollectorUseCase.sensorPackages.collect { sensorPackage ->
                _sensorInfo.emit(sensorPackage)
            }
        }

        serviceScope.launch {
            sensorCollectorUseCase.currentDataFrame.collect {
                _logDataframe.emit(it)
            }
        }
    }
}