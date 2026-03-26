package com.gasmonsoft.fuelboxcontrol.calibracion

import com.gasmonsoft.fuelboxcontrol.domain.calibracion.CalibrationUseCase
import com.gasmonsoft.fuelboxcontrol.domain.calibracion.ModelSelectorUseCase
import com.gasmonsoft.fuelboxcontrol.domain.calibracion.PoliRegressionUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class CalibrationUseCaseImplTest {

    private val poliRegressionUseCase = PoliRegressionUseCase()
    private val modelSelectorUseCase = ModelSelectorUseCase()

    private val sut: CalibrationUseCase = CalibrationUseCaseImpl(
        modelSelectorUseCase = modelSelectorUseCase,
        polinomialRegression = poliRegressionUseCase
    )

    @Test
    fun calibrationTest(): Unit = runTest {
        val rawMeasurements = listOf(
            "10.25" to "0.7",
            "17.7" to "3.99",
            "25.2" to "7.47",
            "32.7" to "10.69",
            "40.2" to "13.39",
            "47.7" to "16.2",
            "55.2" to "18.68",
            "62.7" to "21.32",
            "70.2" to "24.18",
            "77.6" to "26.79",
            "85.1" to "29.29",
            "92.6" to "31.81",
            "100.1" to "34.52",
            "107.6" to "37.06",
            "115.1" to "39.71",
            "122.6" to "42.37",
            "130.1" to "45.21",
            "137.5" to "47.98",
            "145.0" to "51",
            "152.5" to "54.28",
            "158.0" to "56.68"
        )

        val result = sut(rawMeasurements)

        assertNotNull(result)
    }
}