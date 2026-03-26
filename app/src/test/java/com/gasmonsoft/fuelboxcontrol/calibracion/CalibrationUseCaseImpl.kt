package com.gasmonsoft.fuelboxcontrol.calibracion

import com.gasmonsoft.fuelboxcontrol.domain.calibracion.CalibrationUseCase
import com.gasmonsoft.fuelboxcontrol.domain.calibracion.ModelSelectorUseCase
import com.gasmonsoft.fuelboxcontrol.domain.calibracion.PoliRegressionUseCase
import com.gasmonsoft.fuelboxcontrol.model.calibracion.Calibration
import com.gasmonsoft.fuelboxcontrol.model.sensor.Tendencia
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.math3.stat.regression.SimpleRegression
import javax.inject.Inject
import kotlin.math.ln
import kotlin.math.sqrt

class CalibrationUseCaseImpl @Inject constructor(
    private val polinomialRegression: PoliRegressionUseCase,
    private val modelSelectorUseCase: ModelSelectorUseCase
) : CalibrationUseCase {

    override suspend fun invoke(rawMeasurements: List<Pair<String, String>>): Calibration =
        withContext(Dispatchers.IO) {

            val puntos = rawMeasurements.mapIndexedNotNull { index, pair ->
                val litros = pair.first.toDoubleOrNull()
                val nivel = pair.second.toDoubleOrNull()

                if (litros == null || nivel == null) {
                    null
                } else {
                    Punto(
                        index = index,
                        nivel = nivel,
                        litros = litros
                    )
                }
            }

            if (puntos.size < 2) {
                return@withContext Calibration(formula = "")
            }

            val tendencias = construirTendenciasAutomaticas(puntos)

            println("\nTendencias encontradas:")
            tendencias.forEachIndexed { i, t -> println("$i = $t") }

            val modelo = polinomialRegression(
                tendencias = tendencias,
                data = puntos.map { it.nivel to it.litros }
            )

            if (modelo != null) {
                val result = modelSelectorUseCase(
                    linealCoefficients = tendencias,
                    poliCoefficients = modelo.coefficients
                )

                val decision = if (result) "Polinomial" else "Escalonada"
                println("Modelo seleccionado: $decision")
            }

            Calibration(
                formula = ""
            )
        }

    private fun construirTendenciasAutomaticas(
        puntos: List<Punto>
    ): MutableList<Tendencia> {
        val n = puntos.size

        if (n < MIN_POINTS_PER_SEGMENT) {
            return mutableListOf(
                construirTendenciaDesdeRango(
                    puntos = puntos,
                    start = 0,
                    end = puntos.lastIndex
                )
            )
        }

        val fits = Array(n) { arrayOfNulls<SegmentFit>(n) }

        for (start in 0 until n) {
            for (end in start until n) {
                val size = end - start + 1
                if (size >= MIN_POINTS_PER_SEGMENT) {
                    fits[start][end] = fitSegment(
                        puntos = puntos,
                        start = start,
                        end = end
                    )
                }
            }
        }

        val maxSegments = (n / MIN_POINTS_PER_SEGMENT).coerceAtLeast(1)

        val dp = Array(maxSegments + 1) { DoubleArray(n) { Double.POSITIVE_INFINITY } }
        val prev = Array(maxSegments + 1) { IntArray(n) { -1 } }

        // Caso base: 1 segmento
        for (end in 0 until n) {
            val fit = fits[0][end]
            if (fit != null) {
                dp[1][end] = fit.sse
                prev[1][end] = -1
            }
        }

        // DP para K = 2..maxSegments
        for (k in 2..maxSegments) {
            for (end in 0 until n) {
                if (end + 1 < k * MIN_POINTS_PER_SEGMENT) continue

                val minCut = (k - 1) * MIN_POINTS_PER_SEGMENT - 1
                val maxCut = end - MIN_POINTS_PER_SEGMENT

                for (cut in minCut..maxCut) {
                    val fit = fits[cut + 1][end] ?: continue
                    if (!dp[k - 1][cut].isFinite()) continue

                    val candidate = dp[k - 1][cut] + fit.sse

                    if (candidate < dp[k][end]) {
                        dp[k][end] = candidate
                        prev[k][end] = cut
                    }
                }
            }
        }

        var bestK = 1
        var bestBic = Double.POSITIVE_INFINITY

        for (k in 1..maxSegments) {
            val sse = dp[k][n - 1]
            if (!sse.isFinite()) continue

            val bic = calcularBic(
                n = n,
                sse = sse,
                k = k
            )

            println("K=$k  SSE=$sse  BIC=$bic")

            if (bic < bestBic) {
                bestBic = bic
                bestK = k
            }
        }

        println("K seleccionado automáticamente: $bestK")

        val ranges = reconstruirSegmentos(
            prev = prev,
            n = n,
            kFinal = bestK
        )

        return ranges.map {
            construirTendenciaDesdeRango(
                puntos = puntos,
                start = it.start,
                end = it.end
            )
        }.toMutableList()
    }

    private fun calcularBic(
        n: Int,
        sse: Double,
        k: Int
    ): Double {
        val sseSeguro = sse.coerceAtLeast(1e-12)
        val p = 3 * k - 1
        return n * ln(sseSeguro / n.toDouble()) + p * ln(n.toDouble())
    }

    private fun reconstruirSegmentos(
        prev: Array<IntArray>,
        n: Int,
        kFinal: Int
    ): List<RangeCandidate> {
        val ranges = mutableListOf<RangeCandidate>()

        var currentEnd = n - 1
        var currentK = kFinal

        while (currentK >= 1 && currentEnd >= 0) {
            val cut = prev[currentK][currentEnd]
            val start = if (cut == -1) 0 else cut + 1
            ranges.add(RangeCandidate(start, currentEnd))
            currentEnd = cut
            currentK--
        }

        ranges.reverse()
        return ranges
    }

    private fun construirTendenciaDesdeRango(
        puntos: List<Punto>,
        start: Int,
        end: Int
    ): Tendencia {
        require(start in puntos.indices) { "start fuera de rango" }
        require(end in puntos.indices) { "end fuera de rango" }
        require(start <= end) { "start no puede ser mayor que end" }

        val regression = SimpleRegression(true)

        for (i in start..end) {
            regression.addData(puntos[i].nivel, puntos[i].litros)
        }

        val sampleIndex = start + ((end - start) / 2)
        val sample = puntos[sampleIndex]

        return Tendencia(
            pendiente = regression.slope,
            intercepto = regression.intercept,
            puntoInicial = start,
            puntoFinal = end,
            initialValue = puntos[start].nivel,
            sampleValue = Pair(sample.nivel, sample.litros)
        )
    }

    private fun fitSegment(
        puntos: List<Punto>,
        start: Int,
        end: Int
    ): SegmentFit {
        val regression = SimpleRegression(true)

        for (i in start..end) {
            regression.addData(puntos[i].nivel, puntos[i].litros)
        }

        val slope = regression.slope
        val intercept = regression.intercept

        var sse = 0.0
        for (i in start..end) {
            val yPred = intercept + slope * puntos[i].nivel
            val error = puntos[i].litros - yPred
            sse += error * error
        }

        return SegmentFit(
            slope = slope,
            intercept = intercept,
            sse = sse
        )
    }

    private data class SegmentFit(
        val slope: Double,
        val intercept: Double,
        val sse: Double
    )

    private data class RangeCandidate(
        val start: Int,
        val end: Int
    )

    data class Punto(
        val index: Int,
        val nivel: Double,
        val litros: Double
    )

    companion object {
        private const val MIN_POINTS_PER_SEGMENT = 3
    }
}