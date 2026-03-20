package com.gasmonsoft.fuelboxcontrol.ui.calibracion.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gasmonsoft.fuelboxcontrol.ui.common.ScreenHeaderCard

@Composable
fun CalibrationRoute(modifier: Modifier = Modifier) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CalibrationScreen(emptyList(), emptyList())
    }

}

@Composable
fun CalibrationScreen(
    points: List<ChartPoint>,
    coefficients: List<Float>,
    modifier: Modifier = Modifier
) {
    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
            MaterialTheme.colorScheme.surface,
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
        )
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundBrush)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ScreenHeaderCard(
                title = "Calibracion de grafica",
                subtitle = "Calibracion de sensores",
                icon = Icons.AutoMirrored.Filled.ShowChart,
            )
            Surface(
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
                ),
                shadowElevation = 10.dp
            ) {
                PolynomialRegressionChart(
                    points = points,
                    coefficients = coefficients,
                )
            }
        }
    }
}

@Composable
@Preview(showBackground = true, showSystemUi = true)
fun CalibrationRoutePreview(modifier: Modifier = Modifier) {
    val points = listOf(
        ChartPoint(1f, 2f),
        ChartPoint(2f, 3f),
        ChartPoint(3f, 5f),
        ChartPoint(4f, 8f),
        ChartPoint(5f, 12f)
    )

    // Ejemplo: y = 0.2 + 0.5x + 0.3x²
    val coefficients = listOf(0.2f, 0.5f, 0.3f)
    CalibrationScreen(
        points = points,
        coefficients = coefficients
    )
}