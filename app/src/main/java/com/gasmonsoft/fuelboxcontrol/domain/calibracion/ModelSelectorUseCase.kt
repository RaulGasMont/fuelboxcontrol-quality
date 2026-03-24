package com.gasmonsoft.fuelboxcontrol.domain.calibracion

import com.gasmonsoft.fuelboxcontrol.model.sensor.Tendencia
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.pow

class ModelSelectorUseCase @Inject constructor() {
    val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    suspend operator fun invoke(
        linealCoefficients: List<Tendencia>,
        poliCoefficients: DoubleArray
    ): Boolean {
        val isPoliApproachBetter = mutableListOf<Boolean>()

        linealCoefficients.forEach {
            val linearResultDeferred = scope.async {
                predictLineal(
                    x = it.sampleValue.first,
                    intercepto = it.intercepto,
                    pendiente = it.pendiente
                )
            }

            val poliResultDeferred = scope.async {
                predictPolynomial(
                    x = it.sampleValue.first,
                    coefficients = poliCoefficients,
                )
            }

            val linearResult = linearResultDeferred.await()
            val poliResult = poliResultDeferred.await()

            val linearApproach = abs(linearResult - it.sampleValue.second)
            val poliApproach = abs(poliResult - it.sampleValue.second)

            isPoliApproachBetter.add(linearApproach < poliApproach)
        }

        return isPoliApproachBetter.all { it }
    }

    private fun predictPolynomial(x: Double, coefficients: DoubleArray): Double {
        var result = 0.0
        for (i in coefficients.indices) {
            result += coefficients[i] * x.pow(i)
        }
        return result
    }

    private fun predictLineal(x: Double, intercepto: Double, pendiente: Double): Double {
        return intercepto + pendiente * x
    }
}