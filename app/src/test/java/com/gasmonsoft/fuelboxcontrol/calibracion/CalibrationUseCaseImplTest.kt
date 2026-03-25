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
            "33" to "2.36",
            "36" to "3.22",
            "39" to "4.12",
            "42" to "5.00",
            "45" to "5.91",
            "48" to "6.82",
            "51" to "7.70",
            "54" to "8.62",
            "57" to "9.49",
            "60" to "10.45",
            "63" to "11.38",
            "66" to "12.25",
            "69" to "13.11",
            "72" to "14.00",
            "75" to "14.88",
            "78" to "15.80",
            "81" to "16.71",
            "84" to "17.63",
            "87" to "18.55",
            "90" to "19.20"
        )

        val result = sut(rawMeasurements)

        assertNotNull(result)
        assertEquals("modelo_calculado", result.formula)
    }
}