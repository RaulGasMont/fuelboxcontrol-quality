package com.gasmonsoft.fbccalidad.ui.detector.screen

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

enum class FuelType(
    val label: String,
    val typeId: Int,
    val topColor: Color,
    val bottomColor: Color,
    val waveColor: Color
) {
    DESCONOCIDO(
        label = "Desconocido",
        typeId = 0,
        topColor = Color(0xFF9CA3AF),
        bottomColor = Color(0xFF6B7280),
        waveColor = Color(0xFFD1D5DB)
    ),
    AIRE(
        label = "Aire",
        typeId = 1,
        topColor = Color(0xFFBEE3F8),
        bottomColor = Color(0xFF63B3ED),
        waveColor = Color(0xFFE0F2FE)
    ),
    DIESEL(
        label = "Diesel",
        typeId = 2,
        topColor = Color(0xFFFBBF24),
        bottomColor = Color(0xFFD97706),
        waveColor = Color(0xFFFCD34D)
    ),
    ACEITE(
        label = "Aceite",
        typeId = 3,
        topColor = Color(0xFF4B5563),
        bottomColor = Color(0xFF111827),
        waveColor = Color(0xFF9CA3AF)
    ),
    ALCOHOL(
        label = "Alcohol",
        typeId = 4,
        topColor = Color(0xFFF9A8D4),
        bottomColor = Color(0xFFEC4899),
        waveColor = Color(0xFFFBCFE8)
    ),
    AGUA(
        label = "Agua",
        typeId = 5,
        topColor = Color(0xFF60A5FA),
        bottomColor = Color(0xFF2563EB),
        waveColor = Color(0xFFBFDBFE)
    ),
    ADULTERADO(
        label = "Adulterado",
        typeId = 6,
        topColor = Color(0xFFEF4444),
        bottomColor = Color(0xFF991B1B),
        waveColor = Color(0xFFFCA5A5)
    )
}

@Composable
fun AnimatedFuelTank(
    fuelType: FuelType,
    level: Float,
    modifier: Modifier = Modifier.size(width = 150.dp, height = 310.dp),
    showInfo: Boolean = true
) {
    val safeLevel = level.coerceIn(0f, 1f)

    val animatedLevel by animateFloatAsState(
        targetValue = safeLevel,
        animationSpec = tween(durationMillis = 900),
        label = "tank-level"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "tank-wave")

    val waveShift1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing)
        ),
        label = "wave-shift-1"
    )

    val waveShift2 by infiniteTransition.animateFloat(
        initialValue = PI.toFloat(),
        targetValue = (3f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing)
        ),
        label = "wave-shift-2"
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 6.dp.toPx()
            val neckWidth = size.width * 0.28f
            val neckHeight = size.height * 0.075f
            val tankTop = neckHeight + strokeWidth
            val tankBottom = size.height - strokeWidth
            val tankLeft = strokeWidth
            val tankRight = size.width - strokeWidth
            val tankRect = Rect(
                left = tankLeft,
                top = tankTop,
                right = tankRight,
                bottom = tankBottom
            )
            val corner = CornerRadius(x = size.width * 0.22f, y = size.width * 0.22f)

            val tankPath = Path().apply {
                addRoundRect(
                    RoundRect(
                        rect = tankRect,
                        topLeft = corner,
                        topRight = corner,
                        bottomLeft = corner,
                        bottomRight = corner
                    )
                )
            }

            val neckLeft = (size.width - neckWidth) / 2f
            val neckTop = strokeWidth / 2f

            drawRoundRect(
                color = Color(0xFFCBD5E1).copy(alpha = 0.30f),
                topLeft = Offset(neckLeft, neckTop),
                size = androidx.compose.ui.geometry.Size(neckWidth, neckHeight),
                cornerRadius = CornerRadius(neckWidth * 0.25f, neckWidth * 0.25f)
            )

            drawRoundRect(
                color = Color(0xFF94A3B8),
                topLeft = Offset(neckLeft, neckTop),
                size = androidx.compose.ui.geometry.Size(neckWidth, neckHeight),
                cornerRadius = CornerRadius(neckWidth * 0.25f, neckWidth * 0.25f),
                style = Stroke(width = strokeWidth)
            )

            drawPath(
                path = tankPath,
                color = Color(0xFF0F172A).copy(alpha = 0.08f)
            )

            val liquidBaseY = tankRect.bottom - (tankRect.height * animatedLevel)
            val amplitude = size.height * 0.018f
            val wavelength = tankRect.width * 0.9f

            clipPath(tankPath) {
                val wave1 = buildWavePath(
                    left = tankRect.left,
                    right = tankRect.right,
                    bottom = tankRect.bottom,
                    baseY = liquidBaseY,
                    amplitude = amplitude,
                    phase = waveShift1,
                    wavelength = wavelength
                )

                val wave2 = buildWavePath(
                    left = tankRect.left,
                    right = tankRect.right,
                    bottom = tankRect.bottom,
                    baseY = liquidBaseY + amplitude * 0.35f,
                    amplitude = amplitude * 0.75f,
                    phase = waveShift2,
                    wavelength = wavelength * 0.8f
                )

                drawPath(
                    path = wave1,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            fuelType.waveColor,
                            fuelType.topColor,
                            fuelType.bottomColor
                        ),
                        startY = liquidBaseY - amplitude * 2f,
                        endY = tankRect.bottom
                    )
                )

                drawPath(
                    path = wave2,
                    color = fuelType.waveColor.copy(alpha = 0.35f)
                )

                drawLine(
                    color = Color.White.copy(alpha = 0.12f),
                    start = Offset(
                        x = tankRect.left + tankRect.width * 0.20f,
                        y = tankRect.top + 16f
                    ),
                    end = Offset(
                        x = tankRect.left + tankRect.width * 0.20f,
                        y = tankRect.bottom - 16f
                    ),
                    strokeWidth = tankRect.width * 0.08f,
                    cap = StrokeCap.Round
                )
            }

            drawPath(
                path = tankPath,
                color = Color(0xFF94A3B8),
                style = Stroke(width = strokeWidth)
            )
        }

        if (showInfo) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp),
                shape = CircleShape,
                color = Color(0xFF0F172A).copy(alpha = 0.72f)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = fuelType.label,
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White
                    )
//                    Text(
//                        text = "${(animatedLevel * 100f).roundToInt()}%",
//                        style = MaterialTheme.typography.bodyMedium,
//                        color = Color.White.copy(alpha = 0.90f)
//                    )
                }
            }
        }
    }
}

private fun buildWavePath(
    left: Float,
    right: Float,
    bottom: Float,
    baseY: Float,
    amplitude: Float,
    phase: Float,
    wavelength: Float
): Path {
    val path = Path().apply {
        moveTo(left, bottom)
        lineTo(left, baseY)

        var x = left
        while (x <= right) {
            val angle = (((x - left) / wavelength) * 2f * PI).toFloat() + phase
            val y = baseY + sin(angle) * amplitude
            lineTo(x, y)
            x += 4f
        }

        lineTo(right, bottom)
        close()
    }
    return path
}

@Preview(showBackground = true, backgroundColor = 0xFFF8FAFC)
@Composable
private fun AnimatedFuelTankPreview() {
    Row(
        modifier = Modifier
            .background(Color(0xFFF8FAFC))
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        AnimatedFuelTank(
            fuelType = FuelType.DIESEL,
            level = 0.32f,
            modifier = Modifier.size(120.dp, 250.dp)
        )

        AnimatedFuelTank(
            fuelType = FuelType.ALCOHOL,
            level = 0.68f,
            modifier = Modifier.size(120.dp, 250.dp)
        )

        AnimatedFuelTank(
            fuelType = FuelType.ACEITE,
            level = 0.91f,
            modifier = Modifier.size(120.dp, 250.dp)
        )
    }
}