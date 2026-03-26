package com.gasmonsoft.fuelboxcontrol.domain.calibracion

import com.gasmonsoft.fuelboxcontrol.data.model.calibracion.Tendencia
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.pow

class ModelSelectorUseCase @Inject constructor() {

    suspend operator fun invoke(
        linealCoefficients: List<Tendencia>,
        poliCoefficients: DoubleArray
    ): Boolean = withContext(Dispatchers.Default) {

        val isPoliApproachBetter = mutableListOf<Boolean>()

        linealCoefficients.forEach { tendencia ->
            // sampleValue ya viene como (x, y) = (nivel, litros)
            val x = tendencia.sampleValue.first
            val yReal = tendencia.sampleValue.second

            val linearResult = predictLineal(
                x = x,
                intercepto = tendencia.intercepto,
                pendiente = tendencia.pendiente
            )

            val poliResult = predictPolynomial(
                x = x,
                coefficients = poliCoefficients
            )

            val linearApproach = abs(linearResult - yReal)
            val poliApproach = abs(poliResult - yReal)

            // true cuando la polinomial sí fue mejor
            isPoliApproachBetter.add(poliApproach < linearApproach)
        }

        isPoliApproachBetter.all { it }
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