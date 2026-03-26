package com.gasmonsoft.fuelboxcontrol.domain.calibracion

import com.gasmonsoft.fuelboxcontrol.model.calibracion.Tendencia
import org.apache.commons.math3.stat.regression.OLSMultipleLinearRegression
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

class PoliRegressionUseCase @Inject constructor() {
    data class PolynomialModel(
        val degree: Int,
        val coefficients: DoubleArray,
        val rmse: Double
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as PolynomialModel

            if (degree != other.degree) return false
            if (rmse != other.rmse) return false
            if (!coefficients.contentEquals(other.coefficients)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = degree
            result = 31 * result + rmse.hashCode()
            result = 31 * result + coefficients.contentHashCode()
            return result
        }
    }

    operator fun invoke(
        tendencias: List<Tendencia>,
        data: List<Pair<Double, Double>>,
    ): PolynomialModel? {
        val gradoSugerido = gradoSugeridoPorSegmentos(tendencias)
        println("Grado sugerido por segmentos: $gradoSugerido")

        if (gradoSugerido > 6) return null

        val gradosAProbar = listOf(
            (gradoSugerido - 1).coerceAtLeast(1),
            gradoSugerido,
            gradoSugerido + 1
        ).distinct().filter { it < data.size }

        var mejor: PolynomialModel? = null

        for (grado in gradosAProbar) {
            val model = fitPolynomialRegression(data, grado)
            if (mejor == null || model.rmse < mejor!!.rmse) {
                mejor = model
            }
        }

        return mejor!!
    }

    private fun fitPolynomialRegression(
        data: List<Pair<Double, Double>>,
        degree: Int
    ): PolynomialModel {
        require(data.size >= degree + 1) {
            "Se necesitan al menos ${degree + 1} puntos para una regresión de grado $degree"
        }

        val y = data.map { it.second }.toDoubleArray()

        val xMatrix = Array(data.size) { i ->
            val x = data[i].first
            DoubleArray(degree) { j ->
                x.pow(j + 1)
            }
        }

        val regression = OLSMultipleLinearRegression()
        regression.newSampleData(y, xMatrix)

        val coefficients = regression.estimateRegressionParameters()
        val residuals = regression.estimateResiduals()
        val mse = residuals.map { it * it }.average()
        val rmse = sqrt(mse)

        return PolynomialModel(
            degree = degree,
            coefficients = coefficients,
            rmse = rmse
        )
    }

    private fun gradoSugeridoPorSegmentos(tendencias: List<Tendencia>): Int {
        return tendencias.size.coerceAtLeast(1)
    }


    fun polynomialToString(coefficients: DoubleArray): String {
        return coefficients.mapIndexed { index, coef ->
            when (index) {
                0 -> "%.6f".format(coef)
                1 -> {
                    val sign = if (coef >= 0) " + " else " - "
                    sign + "%.6f".format(abs(coef)) + "x"
                }

                else -> {
                    val sign = if (coef >= 0) " + " else " - "
                    sign + "%.6f".format(abs(coef)) + "x^$index"
                }
            }
        }.joinToString("")
            .let { "y = $it" }
    }
}