package com.gasmonsoft.fuelboxcontrol.domain

import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

enum class SensorDataType {
    FIRST,
    SECOND,
    THIRD,
    FOURTH,
    ACCELEROMETER,
    ALERTAS_GLOBALES,
}

data class SensorPackage(
    val date: String,
    val data: String,
)

data class SensorGroup(
    var first: String? = null,
    var second: String? = null,
    var third: String? = null,
    var fourth: String? = null,
    var accelerometer: String? = null
)

@Singleton
class SensorCollectorUseCase @Inject constructor(
    private val sharedPreferences: SharedPreferences
) {

    private val _sensorPackages = MutableSharedFlow<SensorPackage>(
        extraBufferCapacity = 64
    )
    val sensorPackages = _sensorPackages.asSharedFlow()
    private val sensorBuffer = mutableMapOf<String, SensorGroup>() // Date - Data

    operator fun invoke(sensor: SensorDataType, rawData: ByteArray) {
        val boxType = sharedPreferences.getString("tipoCaja", null) ?: return

        val parts = rawData.toString(Charsets.UTF_8).trim().split(" ")
        if (parts.size < 2) return

        val date = "${parts[0]} ${parts[1]}"
        val data = parts.drop(2).joinToString(",")

        val group = sensorBuffer.getOrPut(date) { SensorGroup() }

        when (sensor) {
            SensorDataType.FIRST -> group.first = data
            SensorDataType.SECOND -> group.second = data
            SensorDataType.THIRD -> group.third = data
            SensorDataType.FOURTH -> group.fourth = data
            SensorDataType.ACCELEROMETER -> group.accelerometer = data
            else -> {}
        }

        val isDataComplete = if (boxType == "1") {
            group.isComplete()
        } else {
            group.isQualityComplete()
        }

        if (isDataComplete) {
            val payload =
                "${group.first},${group.second},${group.third},${group.fourth},${group.accelerometer}"

            _sensorPackages.tryEmit(
                SensorPackage(date, payload)
            )

            sensorBuffer.remove(date)
        }
    }

    private fun SensorGroup.isComplete(): Boolean =
        first != null && second != null && third != null && fourth != null && accelerometer != null

    private fun SensorGroup.isQualityComplete(): Boolean =
        first != null && second != null && third != null && fourth != null
}