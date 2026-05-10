package com.gasmonsoft.fbccalidad.domain.detector

import com.gasmonsoft.fbccalidad.data.model.ble.SensorState
import com.gasmonsoft.fbccalidad.domain.model.QualityRange
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

data class MatterUnity(
    val type: QualityRange,
    val value: Float?,
    val temperature: Float?
)

class DetectorUseCase @Inject constructor() {

    private val fuelTypeResults = mutableListOf<MatterUnity>()

    suspend operator fun invoke(
        data: Flow<SensorState>,
        matterUnities: List<QualityRange>
    ): MatterUnity {
        fuelTypeResults.clear()

        withTimeoutOrNull(15_000L) {
            data.collect { value ->
                val calidad = value.sensor1.calidad.toDoubleOrNull()
                val temperature = value.sensor1.temperatura.toFloatOrNull()

                if (calidad != null) {
                    val matchedRange = matterUnities.find { range ->
                        val min = range.min ?: Double.MIN_VALUE
                        val max = range.max ?: Double.MAX_VALUE
                        calidad in min..max
                    } ?: QualityRange.DESCONOCIDO

                    fuelTypeResults.add(
                        MatterUnity(
                            type = matchedRange,
                            value = calidad.toFloat(),
                            temperature = temperature
                        )
                    )
                }
            }
        }

        if (fuelTypeResults.isEmpty()) {
            return MatterUnity(QualityRange.DESCONOCIDO, null, null)
        }

        val mostCommonType = fuelTypeResults
            .groupBy { it.type }
            .maxByOrNull { it.value.size }
            ?.key
            ?: return MatterUnity(QualityRange.DESCONOCIDO, null, null)

        val commonValues = fuelTypeResults.filter { it.type == mostCommonType }

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
