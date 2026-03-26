package com.gasmonsoft.fuelboxcontrol.domain.calibracion

import android.util.Log
import com.gasmonsoft.fuelboxcontrol.model.calibracion.Calibration
import com.gasmonsoft.fuelboxcontrol.model.calibracion.Tendencia
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.math3.stat.regression.SimpleRegression
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sqrt

interface CalibrationUseCase {
    suspend operator fun invoke(rawMeasurements: List<Pair<String, String>>): Calibration
}

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
                return@withContext Calibration(formula = "", message = "")
            }

            val tendencias = construirTendenciasDinamicas(puntos).toMutableList()

            ajustarUltimoSegmentoSiEsMuyCorto(
                tendencias = tendencias,
                puntos = puntos,
                minPointsPerSegment = MIN_POINTS_PER_SEGMENT
            )

            refinarFronterasSegmentos(
                tendencias = tendencias,
                puntos = puntos,
                minPointsPerSegment = MIN_POINTS_PER_SEGMENT
            )

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
                Log.i("Calibracion", "Modelo seleccionado: $decision")
            }

            Calibration(
                formula = "",
                message = ""
            )
        }

    private fun construirTendenciasDinamicas(
        puntos: List<Punto>
    ): List<Tendencia> {
        val tendencias = mutableListOf<Tendencia>()

        var puntoInicial = 0
        var regression = SimpleRegression(true)

        puntos.forEachIndexed { index, punto ->
            val litros = punto.litros
            val nivel = punto.nivel

            if (regression.n < MIN_POINTS_PER_SEGMENT.toLong()) {
                regression.addData(nivel, litros)
                return@forEachIndexed
            }

            val mActual = regression.slope
            val bActual = regression.intercept

            val puntosActuales = puntos.subList(puntoInicial, index)
            val rmseActual = calcularRmse(
                puntos = puntosActuales,
                pendiente = mActual,
                intercepto = bActual
            )

            val regressionNuevo = SimpleRegression(true)
            puntos.subList(puntoInicial, index + 1).forEach {
                regressionNuevo.addData(it.nivel, it.litros)
            }

            val mNuevo = regressionNuevo.slope
            val bNuevo = regressionNuevo.intercept
            val rmseNuevo = calcularRmse(
                puntos = puntos.subList(puntoInicial, index + 1),
                pendiente = mNuevo,
                intercepto = bNuevo
            )

            val corresponde = puntoCorresponde(
                xNuevo = nivel,
                yReal = litros,
                pendienteActual = mActual,
                interceptoActual = bActual,
                rmseActual = rmseActual,
                pendienteNueva = mNuevo,
                interceptoNuevo = bNuevo,
                rmseNuevo = rmseNuevo,
                factorTolerancia = FACTOR_TOLERANCIA,
                toleranciaMinima = TOLERANCIA_MINIMA,
                factorCrecimientoRmse = FACTOR_CRECIMIENTO_RMSE
            )

            if (corresponde) {
                regression.addData(nivel, litros)
            } else {
                val nuevoPunto = construirTendenciaDesdeRango(
                    puntos = puntos,
                    start = puntoInicial,
                    end = index - 1
                )
                if (nuevoPunto != null) {
                    tendencias.add(nuevoPunto)

                    puntoInicial = index
                    regression = SimpleRegression(true)
                    regression.addData(nivel, litros)
                }
            }
        }

        if (regression.n >= 2) {
            val nuevoPunto = construirTendenciaDesdeRango(
                puntos = puntos,
                start = puntoInicial,
                end = puntos.lastIndex
            )
            if (nuevoPunto != null) {
                tendencias.add(nuevoPunto)
            }
        }

        return tendencias
    }

    private fun refinarFronterasSegmentos(
        tendencias: MutableList<Tendencia>,
        puntos: List<Punto>,
        minPointsPerSegment: Int
    ) {
        if (tendencias.size < 2) return

        var huboCambio: Boolean

        do {
            huboCambio = false

            for (i in 0 until tendencias.lastIndex) {
                val izquierda = tendencias[i]
                val derecha = tendencias[i + 1]

                val mejor = mejorFronteraEntreSegmentos(
                    izquierda = izquierda,
                    derecha = derecha,
                    puntos = puntos,
                    minPointsPerSegment = minPointsPerSegment
                )

                if (mejor != null) {
                    val nuevaIzquierda = construirTendenciaDesdeRango(
                        puntos = puntos,
                        start = mejor.first,
                        end = mejor.second
                    )

                    val nuevaDerecha = construirTendenciaDesdeRango(
                        puntos = puntos,
                        start = mejor.third,
                        end = mejor.fourth
                    )

                    if (nuevaIzquierda == null || nuevaDerecha == null) continue

                    val cambioIzquierda = nuevaIzquierda.puntoInicial != izquierda.puntoInicial ||
                            nuevaIzquierda.puntoFinal != izquierda.puntoFinal

                    val cambioDerecha = nuevaDerecha.puntoInicial != derecha.puntoInicial ||
                            nuevaDerecha.puntoFinal != derecha.puntoFinal

                    if (cambioIzquierda || cambioDerecha) {
                        tendencias[i] = nuevaIzquierda
                        tendencias[i + 1] = nuevaDerecha
                        huboCambio = true
                    }
                }
            }

        } while (huboCambio)
    }

    private fun mejorFronteraEntreSegmentos(
        izquierda: Tendencia,
        derecha: Tendencia,
        puntos: List<Punto>,
        minPointsPerSegment: Int
    ): BoundaryCandidate? {
        val leftStart = izquierda.puntoInicial
        val leftEnd = izquierda.puntoFinal
        val rightStart = derecha.puntoInicial
        val rightEnd = derecha.puntoFinal

        val candidatos = mutableListOf<BoundaryCandidate>()

        // Opción actual
        if (segmentoValido(leftStart, leftEnd, minPointsPerSegment) &&
            segmentoValido(rightStart, rightEnd, minPointsPerSegment)
        ) {
            candidatos.add(
                BoundaryCandidate(
                    first = leftStart,
                    second = leftEnd,
                    third = rightStart,
                    fourth = rightEnd,
                    error = errorTotalDosSegmentos(leftStart, leftEnd, rightStart, rightEnd, puntos)
                )
            )
        }

        // Mover un punto del segmento izquierdo al derecho
        if (segmentoValido(leftStart, leftEnd - 1, minPointsPerSegment) &&
            segmentoValido(rightStart - 1, rightEnd, minPointsPerSegment) &&
            leftEnd == rightStart - 1
        ) {
            candidatos.add(
                BoundaryCandidate(
                    first = leftStart,
                    second = leftEnd - 1,
                    third = rightStart - 1,
                    fourth = rightEnd,
                    error = errorTotalDosSegmentos(
                        leftStart,
                        leftEnd - 1,
                        rightStart - 1,
                        rightEnd,
                        puntos
                    )
                )
            )
        }

        // Mover un punto del segmento derecho al izquierdo
        if (segmentoValido(leftStart, leftEnd + 1, minPointsPerSegment) &&
            segmentoValido(rightStart + 1, rightEnd, minPointsPerSegment) &&
            leftEnd == rightStart - 1
        ) {
            candidatos.add(
                BoundaryCandidate(
                    first = leftStart,
                    second = leftEnd + 1,
                    third = rightStart + 1,
                    fourth = rightEnd,
                    error = errorTotalDosSegmentos(
                        leftStart,
                        leftEnd + 1,
                        rightStart + 1,
                        rightEnd,
                        puntos
                    )
                )
            )
        }

        return candidatos.minByOrNull { it.error }
    }

    private fun errorTotalDosSegmentos(
        leftStart: Int,
        leftEnd: Int,
        rightStart: Int,
        rightEnd: Int,
        puntos: List<Punto>
    ): Double {
        val fitIzq = fitSegment(puntos, leftStart, leftEnd)
        val fitDer = fitSegment(puntos, rightStart, rightEnd)

        val rmseIzq = calcularRmse(
            puntos = puntos.subList(leftStart, leftEnd + 1),
            pendiente = fitIzq.first,
            intercepto = fitIzq.second
        )

        val rmseDer = calcularRmse(
            puntos = puntos.subList(rightStart, rightEnd + 1),
            pendiente = fitDer.first,
            intercepto = fitDer.second
        )

        return rmseIzq + rmseDer
    }

    private fun asegurarCoberturaUltimoPunto(
        tendencias: MutableList<Tendencia>,
        puntos: List<Punto>,
        minPointsPerSegment: Int
    ) {
        if (tendencias.isEmpty() || puntos.isEmpty()) return

        val ultimoIndiceReal = puntos.lastIndex
        val ultimaTendencia = tendencias.last()

        // Ya cubre todo
        if (ultimaTendencia.puntoFinal >= ultimoIndiceReal) return

        val inicioCola = ultimaTendencia.puntoFinal + 1
        val finCola = ultimoIndiceReal
        val puntosFaltantes = finCola - inicioCola + 1

        if (puntosFaltantes <= 0) return

        // Si la cola es muy corta, la absorbemos en la última tendencia
        if (puntosFaltantes < minPointsPerSegment) {
            val reconstruida = construirTendenciaDesdeRango(
                puntos = puntos,
                start = ultimaTendencia.puntoInicial,
                end = ultimoIndiceReal
            )
            if (reconstruida == null) return
            tendencias[tendencias.lastIndex] = reconstruida
            return
        }

        // Si la cola ya alcanza para formar un segmento válido, la creamos
        val nuevaFinal = construirTendenciaDesdeRango(
            puntos = puntos,
            start = inicioCola,
            end = finCola
        )
        if (nuevaFinal == null) return
        tendencias.add(nuevaFinal)
    }

    private fun ajustarUltimoSegmentoSiEsMuyCorto(
        tendencias: MutableList<Tendencia>,
        puntos: List<Punto>,
        minPointsPerSegment: Int
    ) {
        if (tendencias.size < 2) return

        val ultimo = tendencias.last()
        val puntosUltimo = ultimo.puntoFinal - ultimo.puntoInicial + 1

        if (puntosUltimo >= minPointsPerSegment) return

        val penultimo = tendencias[tendencias.lastIndex - 1]

        val fusionado = construirTendenciaDesdeRango(
            puntos = puntos,
            start = penultimo.puntoInicial,
            end = ultimo.puntoFinal
        )

        tendencias.removeAt(tendencias.lastIndex)
        tendencias.removeAt(tendencias.lastIndex)
        if (fusionado == null) return
        tendencias.add(fusionado)
    }

    private fun construirTendenciaDesdeRango(
        puntos: List<Punto>,
        start: Int,
        end: Int
    ): Tendencia? {
        if (start in puntos.indices) {
            println("start fuera de rango")
            return null
        }
        if (end in puntos.indices) {
            println("end fuera de rango")
            return null
        }
        if (start <= end) {
            println("start no puede ser mayor que end")
            return null
        }

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
    ): Pair<Double, Double> {
        val regression = SimpleRegression(true)
        for (i in start..end) {
            regression.addData(puntos[i].nivel, puntos[i].litros)
        }
        return regression.slope to regression.intercept
    }

    private fun puntoCorresponde(
        xNuevo: Double,
        yReal: Double,
        pendienteActual: Double,
        interceptoActual: Double,
        rmseActual: Double,
        pendienteNueva: Double,
        interceptoNuevo: Double,
        rmseNuevo: Double,
        factorTolerancia: Double = 1.8,
        toleranciaMinima: Double = 0.10,
        factorCrecimientoRmse: Double = 1.35
    ): Boolean {
        val yPredActual = predecir(xNuevo, pendienteActual, interceptoActual)
        val errorActual = abs(yReal - yPredActual)

        val yPredNuevo = predecir(xNuevo, pendienteNueva, interceptoNuevo)
        val errorNuevo = abs(yReal - yPredNuevo)

        val umbral = max(rmseActual * factorTolerancia, toleranciaMinima)
        val noExplotaRmse = rmseNuevo <= max(rmseActual * factorCrecimientoRmse, toleranciaMinima)

        return errorActual <= umbral &&
                errorNuevo <= umbral &&
                noExplotaRmse
    }

    private fun calcularRmse(
        puntos: List<Punto>,
        pendiente: Double,
        intercepto: Double
    ): Double {
        require(puntos.isNotEmpty()) { "La lista no puede estar vacía" }

        val mse = puntos.sumOf { punto ->
            val yPred = predecir(punto.nivel, pendiente, intercepto)
            val error = punto.litros - yPred
            error.pow(2)
        } / puntos.size

        return sqrt(mse)
    }

    private fun predecir(x: Double, pendiente: Double, intercepto: Double): Double {
        return intercepto + pendiente * x
    }

    private fun segmentoValido(
        start: Int,
        end: Int,
        minPointsPerSegment: Int
    ): Boolean {
        if (start > end) return false
        val size = end - start + 1
        return size >= minPointsPerSegment
    }

    data class Punto(
        val index: Int,
        val nivel: Double,
        val litros: Double
    )

    data class BoundaryCandidate(
        val first: Int,
        val second: Int,
        val third: Int,
        val fourth: Int,
        val error: Double
    )

    companion object {
        private const val MIN_POINTS_PER_SEGMENT = 3
        private const val FACTOR_TOLERANCIA = 1.8
        private const val TOLERANCIA_MINIMA = 0.10
        private const val FACTOR_CRECIMIENTO_RMSE = 1.35
    }
}