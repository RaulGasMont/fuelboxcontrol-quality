package com.gasmonsoft.fuelboxcontrol.domain.calibracion

import android.util.Log
import com.gasmonsoft.fuelboxcontrol.model.sensor.Tendencia
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.math3.stat.regression.SimpleRegression
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sqrt

class CalibrationUseCase @Inject constructor(
    private val polinomialRegression: PoliRegressionUseCase,
    private val modelSelectorUseCase: ModelSelectorUseCase
) {
    suspend operator fun invoke(rawMeasurements: List<Pair<String, String>>) =
        withContext(Dispatchers.IO) {
            val tendencias: MutableList<Tendencia> = mutableListOf()
            var puntoInicial = 0

            var regression = SimpleRegression(true)

            rawMeasurements.forEachIndexed { index, pair ->
                // Mientras no haya suficientes puntos para una recta, solo agregamos
                val litros = pair.first.toDoubleOrNull()
                val valSensor = pair.second.toDoubleOrNull()
                if (litros == null || valSensor == null) return@forEachIndexed
                if (regression.n < 2) {
                    regression.addData(litros, valSensor)
                    return@forEachIndexed
                }

                val mActual = regression.slope
                val bActual = regression.intercept

                if (pair.first.toDoubleOrNull() == null || pair.second.toDoubleOrNull() == null) return@forEachIndexed

                val xsSegmento = rawMeasurements
                    .subList(puntoInicial, index)
                    .map { it.first.toDouble() }

                val ysSegmento = rawMeasurements
                    .subList(puntoInicial, index)
                    .map { it.second.toDouble() }

                val rmse = calcularRmse(
                    xs = xsSegmento,
                    ys = ysSegmento,
                    pendiente = mActual,
                    intercepto = bActual
                )

                val corresponde = puntoCorresponde(
                    xNuevo = litros,
                    yReal = valSensor,
                    pendiente = mActual,
                    intercepto = bActual,
                    rmse = rmse,
                    factorTolerancia = 2.0
                )

                if (corresponde) {
                    regression.addData(litros, valSensor)
                } else {
                    tendencias.add(
                        Tendencia(
                            pendiente = mActual,
                            intercepto = bActual,
                            puntoInicial = puntoInicial,
                            puntoFinal = index - 1,
                            sampleValue = Pair(
                                rawMeasurements[index - 1].first.toDouble(),
                                rawMeasurements[index - 1].second.toDouble()
                            )
                        )
                    )

                    print("Hasta el punto ${index - 1} -> ")
                    printFormula(mActual, bActual)

                    // Inicia nueva tendencia con el punto actual
                    puntoInicial = index
                    regression = SimpleRegression(true)
                    regression.addData(litros, valSensor)
                }
            }

            // Guardar la última tendencia activa
            if (regression.n >= 2) {
                val mFinal = regression.slope
                val bFinal = regression.intercept
                val endPoint = rawMeasurements.lastIndex

                val sampleIndex = endPoint - ((endPoint - puntoInicial) / 2).coerceAtLeast(1)
                val sampleValue = rawMeasurements[sampleIndex - 1]

                tendencias.add(
                    Tendencia(
                        pendiente = mFinal,
                        intercepto = bFinal,
                        puntoInicial = puntoInicial,
                        puntoFinal = rawMeasurements.lastIndex,
                        sampleValue = Pair(
                            sampleValue.first.toDouble(),
                            sampleValue.second.toDouble()
                        )
                    )
                )

                print("Última tendencia -> ")
                Log.i("mFinal", "$mFinal")
                Log.i("bFinal", "$bFinal")
            }


            println("\nTendencias encontradas:")
            tendencias.forEach { println(it) }
            regression.clear()


            val modelo = polinomialRegression(
                tendencias = tendencias,
                data = rawMeasurements.map { it.first.toDouble() to it.second.toDouble() }
            )

            modelSelectorUseCase(
                linealCoefficients = tendencias,
                poliCoefficients = modelo.coefficients
            )

            Pair("", "")
        }

    private fun printFormula(pendiente: Double, intercepto: Double) {
        Log.i("Calibracion", "Pendiente (m): $pendiente")
        Log.i("Calibracion", "Intercepto (b): $intercepto")
        Log.i("Calibracion", "Fórmula: y = $intercepto + $pendiente x")
    }

    private fun predecir(x: Double, pendiente: Double, intercepto: Double): Double {
        return intercepto + pendiente * x // m + bx
    }

    private fun puntoCorresponde(
        xNuevo: Double,
        yReal: Double,
        pendiente: Double,
        intercepto: Double,
        rmse: Double,
        factorTolerancia: Double = 2.0,
        toleranciaMinima: Double = 0.02
    ): Boolean {
        val yPred = predecir(xNuevo, pendiente, intercepto)
        val errorAbs = abs(yReal - yPred)
        val umbral = max(rmse * factorTolerancia, toleranciaMinima)

        return errorAbs <= umbral
    }

    private fun calcularRmse(
        xs: List<Double>,
        ys: List<Double>,
        pendiente: Double,
        intercepto: Double
    ): Double {
        require(xs.size == ys.size) { "Las listas deben tener el mismo tamaño" }
        require(xs.isNotEmpty()) { "Las listas no pueden estar vacías" }

        val mse = xs.indices.sumOf { i ->
            val yPred = predecir(xs[i], pendiente, intercepto)
            val error = ys[i] - yPred
            error.pow(2)
        } / xs.size

        return sqrt(mse)
    }
}