package com.gasmonsoft.fuelboxcontrol.ui.calibracion.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gasmonsoft.fuelboxcontrol.ui.theme.FuelBoxControlTheme

data class ChartPoint(
    val x: Float,
    val y: Float
)

@Composable
fun PolynomialRegressionChart(
    points: List<ChartPoint>,
    coefficients: List<Float>, // Ej: [a0, a1, a2] => a0 + a1*x + a2*x^2
    modifier: Modifier = Modifier
) {
    if (points.isEmpty()) {
        Text("No hay datos para graficar")
        return
    }

    val colorSurface = MaterialTheme.colorScheme.surface
    val colorSurfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val colorOutline = MaterialTheme.colorScheme.outline
    val colorOnSurface = MaterialTheme.colorScheme.onSurface
    val colorPrimary = MaterialTheme.colorScheme.primary
    val colorTertiary = MaterialTheme.colorScheme.tertiary

    Canvas(
        modifier = modifier
            .height(320.dp)
            .fillMaxSize()
            .background(
                color = colorSurface,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(16.dp)
    ) {
        val paddingLeft = 70f
        val paddingBottom = 50f
        val paddingTop = 30f
        val paddingRight = 30f

        val chartWidth = size.width - paddingLeft - paddingRight
        val chartHeight = size.height - paddingTop - paddingBottom

        if (chartWidth <= 0f || chartHeight <= 0f) return@Canvas

        val xValues = points.map { it.x }
        val xMin = xValues.minOrNull() ?: 0f
        val xMax = xValues.maxOrNull() ?: 1f

        // Muestreamos la curva para conocer sus Y y ajustar el viewport
        val sampledCurve = samplePolynomialCurve(
            coefficients = coefficients,
            xMin = xMin,
            xMax = xMax,
            samples = 200
        )

        val yValues = points.map { it.y } + sampledCurve.map { it.y }
        var yMin = yValues.minOrNull() ?: 0f
        var yMax = yValues.maxOrNull() ?: 1f

        // Evitar división entre cero si todos los valores son iguales
        if (xMin == xMax || yMin == yMax) {
            yMin -= 1f
            yMax += 1f
        }

        fun mapX(x: Float): Float {
            return paddingLeft + ((x - xMin) / (xMax - xMin)) * chartWidth
        }

        fun mapY(y: Float): Float {
            return paddingTop + chartHeight - ((y - yMin) / (yMax - yMin)) * chartHeight
        }

        // Fondo del área de gráfico
        drawRect(
            color = colorSurfaceVariant.copy(alpha = 0.25f),
            topLeft = Offset(paddingLeft, paddingTop),
            size = Size(chartWidth, chartHeight)
        )

        // Grid horizontal
        val gridLines = 5
        repeat(gridLines + 1) { i ->
            val y = paddingTop + (chartHeight / gridLines) * i
            drawLine(
                color = colorOutline.copy(alpha = 0.3f),
                start = Offset(paddingLeft, y),
                end = Offset(paddingLeft + chartWidth, y),
                strokeWidth = 1f
            )
        }

        // Eje X
        drawLine(
            color = colorOnSurface,
            start = Offset(paddingLeft, paddingTop + chartHeight),
            end = Offset(paddingLeft + chartWidth, paddingTop + chartHeight),
            strokeWidth = 2f
        )

        // Eje Y
        drawLine(
            color = colorOnSurface,
            start = Offset(paddingLeft, paddingTop),
            end = Offset(paddingLeft, paddingTop + chartHeight),
            strokeWidth = 2f
        )

        // Dibujar curva polinómica
        val curvePath = Path()
        sampledCurve.forEachIndexed { index, point ->
            val px = mapX(point.x)
            val py = mapY(point.y)

            if (index == 0) {
                curvePath.moveTo(px, py)
            } else {
                curvePath.lineTo(px, py)
            }
        }

        drawPath(
            path = curvePath,
            color = colorPrimary,
            style = Stroke(width = 4f)
        )

        // Dibujar puntos reales
        points.forEach { point ->
            drawCircle(
                color = colorTertiary,
                radius = 6f,
                center = Offset(mapX(point.x), mapY(point.y))
            )
        }
    }
}

private fun samplePolynomialCurve(
    coefficients: List<Float>,
    xMin: Float,
    xMax: Float,
    samples: Int
): List<ChartPoint> {
    if (samples <= 1) return emptyList()

    val result = mutableListOf<ChartPoint>()
    val step = (xMax - xMin) / (samples - 1)

    for (i in 0 until samples) {
        val x = xMin + i * step
        val y = evaluatePolynomial(x, coefficients)
        result.add(ChartPoint(x, y))
    }

    return result
}

private fun evaluatePolynomial(x: Float, coefficients: List<Float>): Float {
    var y = 0f
    coefficients.forEachIndexed { power, coefficient ->
        y += coefficient * x.powInt(power)
    }
    return y
}

private fun Float.powInt(exp: Int): Float {
    var result = 1f
    repeat(exp) { result *= this }
    return result
}

@Preview(showBackground = true)
@Composable
fun PolynomialRegressionChartPreview() {
    val samplePoints = listOf(
        ChartPoint(10f, 100f),
        ChartPoint(20f, 150f),
        ChartPoint(30f, 220f),
        ChartPoint(40f, 280f),
        ChartPoint(50f, 350f),
        ChartPoint(60f, 430f),
        ChartPoint(70f, 520f),
        ChartPoint(80f, 630f)
    )
    val sampleCoefficients = listOf(80f, 1.2f, 0.05f) // a0 + a1*x + a2*x^2

    FuelBoxControlTheme {
        PolynomialRegressionChart(
            points = samplePoints,
            coefficients = sampleCoefficients,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        )
    }
}
