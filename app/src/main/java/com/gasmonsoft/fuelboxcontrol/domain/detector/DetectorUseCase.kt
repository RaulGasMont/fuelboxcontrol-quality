package com.gasmonsoft.fuelboxcontrol.domain.detector

import com.gasmonsoft.fuelboxcontrol.data.model.ble.SensorState
import com.gasmonsoft.fuelboxcontrol.ui.detector.screen.FuelType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

data class MatterUnity(
    val type: FuelType,
    val value: Float?,
    val temperature: Float?
)

class DetectorUseCase @Inject constructor() {

    private val fuelType = mutableListOf<MatterUnity>()

    suspend operator fun invoke(data: Flow<SensorState>): MatterUnity {
        fuelType.clear()

        withTimeoutOrNull(60_000L) {
            data.collect { value ->
                val calidad = value.sensor1.calidad.toFloatOrNull()
                val temperature = value.sensor1.temperatura.toFloatOrNull()

                val type = when {
                    calidad == null -> FuelType.DESCONOCIDO
                    calidad > 20.0f -> FuelType.AGUA
                    calidad < 0.0f -> FuelType.ADULTERADO
                    calidad in 0.0f..1.9f -> FuelType.AIRE
                    calidad in 2.0f..2.2f -> FuelType.ACEITE
                    calidad in 2.4f..2.8f -> FuelType.DIESEL
                    calidad in 6.0f..18.0f -> FuelType.ALCOHOL
                    else -> FuelType.DESCONOCIDO
                }

                fuelType.add(MatterUnity(type, calidad, temperature))
            }
        }

        if (fuelType.isEmpty()) {
            return MatterUnity(FuelType.DESCONOCIDO, null, null)
        }

        val mostCommonType = fuelType
            .groupBy { it.type }
            .maxByOrNull { it.value.size }
            ?.key
            ?: return MatterUnity(FuelType.DESCONOCIDO, null, null)

        val commonValues = fuelType.filter { it.type == mostCommonType }

        val average = commonValues
            .mapNotNull { it.value }
            .takeIf { it.isNotEmpty() }
            ?.average()
            ?.toFloat()

        val tempAverage = commonValues
            .mapNotNull { it.temperature }
            .takeIf { it.isNotEmpty() }
            ?.average()
            ?.toFloat()


        return MatterUnity(mostCommonType, average, tempAverage)
    }
}