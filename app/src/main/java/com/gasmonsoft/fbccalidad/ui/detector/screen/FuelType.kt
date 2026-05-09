package com.gasmonsoft.fbccalidad.ui.detector.screen

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gasmonsoft.fbccalidad.domain.model.QualityRange
import com.gasmonsoft.fbccalidad.domain.model.toColor
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

data class FuelType(
    val label: String,
    val typeId: Int,
    val topColor: Color,
    val bottomColor: Color,
    val waveColor: Color
) {
    companion object {
        val DESCONOCIDO = FuelType(
            label = "Desconocido",
            typeId = 0,
            topColor = Color(0xFF9CA3AF),
            bottomColor = Color(0xFF6B7280),
            waveColor = Color(0xFFD1D5DB)
        )
        val AIRE = FuelType(
            label = "Aire",
            typeId = 1,
            topColor = Color(0xFFBEE3F8),
            bottomColor = Color(0xFF63B3ED),
            waveColor = Color(0xFFE0F2FE)
        )
        val DIESEL = FuelType(
            label = "Diesel",
            typeId = 2,
            topColor = Color(0xFFFBBF24),
            bottomColor = Color(0xFFD97706),
            waveColor = Color(0xFFFCD34D)
        )
        val ACEITE = FuelType(
            label = "Aceite",
            typeId = 3,
            topColor = Color(0xFF4B5563),
            bottomColor = Color(0xFF111827),
            waveColor = Color(0xFF9CA3AF)
        )
        val ALCOHOL = FuelType(
            label = "Alcohol",
            typeId = 4,
            topColor = Color(0xFFF9A8D4),
            bottomColor = Color(0xFFEC4899),
            waveColor = Color(0xFFFBCFE8)
        )
        val AGUA = FuelType(
            label = "Agua",
            typeId = 5,
            topColor = Color(0xFF60A5FA),
            bottomColor = Color(0xFF2563EB),
            waveColor = Color(0xFFBFDBFE)
        )
        val ADULTERADO = FuelType(
            label = "Adulterado",
            typeId = 6,
            topColor = Color(0xFFEF4444),
            bottomColor = Color(0xFF991B1B),
            waveColor = Color(0xFFFCA5A5)
        )

        val entries = listOf(
            DESCONOCIDO, AIRE, DIESEL, ACEITE, ALCOHOL, AGUA, ADULTERADO
        )
    }
}

@Composable
fun AnimatedFuelTank(
    fuelType: QualityRange,
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
                if (fuelType.label.equals("Aire", ignoreCase = true)) {
                    // Fondo degradado sutil (cielo)
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFFBAE6FD).copy(alpha = 0.22f),
                                Color(0xFFE0F2FE).copy(alpha = 0.08f)
                            ),
                            startY = tankRect.top,
                            endY = tankRect.bottom
                        ),
                        topLeft = Offset(tankRect.left, tankRect.top),
                        size = androidx.compose.ui.geometry.Size(tankRect.width, tankRect.height)
                    )

                    // Partículas flotando hacia arriba
                    val airProgress = waveShift1 / (2f * PI).toFloat()
                    val particleData = listOf(
                        floatArrayOf(0.20f, 0.00f, 5f),
                        floatArrayOf(0.50f, 0.28f, 3.5f),
                        floatArrayOf(0.78f, 0.55f, 4.5f),
                        floatArrayOf(0.35f, 0.72f, 3f),
                        floatArrayOf(0.65f, 0.15f, 4f),
                        floatArrayOf(0.12f, 0.44f, 3f),
                        floatArrayOf(0.88f, 0.87f, 5f),
                        floatArrayOf(0.55f, 0.62f, 2.5f),
                        floatArrayOf(0.42f, 0.38f, 3.5f),
                        floatArrayOf(0.70f, 0.92f, 2.5f),
                    )

                    particleData.forEach { (xFrac, phaseOffset, radiusDp) ->
                        val p = (airProgress + phaseOffset) % 1.0f
                        val y = tankRect.bottom - p * tankRect.height
                        val sway =
                            sin((p * 4f * PI + phaseOffset * 10f).toFloat()) * tankRect.width * 0.07f
                        val x = (tankRect.left + xFrac * tankRect.width + sway)
                            .coerceIn(tankRect.left, tankRect.right)

                        val alpha = when {
                            p < 0.08f -> p / 0.08f * 0.55f
                            p > 0.92f -> (1f - p) / 0.08f * 0.55f
                            else -> 0.55f
                        }
                        val r = radiusDp.dp.toPx()
                        drawCircle(
                            color = Color(0xFFBAE6FD).copy(alpha = alpha),
                            radius = r,
                            center = Offset(x, y)
                        )
                        // Reflejo interior
                        drawCircle(
                            color = Color.White.copy(alpha = alpha * 0.55f),
                            radius = r * 0.38f,
                            center = Offset(x - r * 0.22f, y - r * 0.22f)
                        )
                    }

                    // Líneas de viento horizontales animadas
                    val windProgress = waveShift2 / (2f * PI).toFloat()
                    listOf(0.25f, 0.50f, 0.73f).forEachIndexed { i, yFrac ->
                        val offsetX =
                            cos((windProgress * 2f * PI + i * 2.1f).toFloat()) * tankRect.width * 0.12f
                        val lineY = tankRect.top + yFrac * tankRect.height
                        val lineAlpha =
                            0.18f + 0.10f * sin((windProgress * 2f * PI + i * 1.3f).toFloat())
                        drawLine(
                            color = Color(0xFF7DD3FC).copy(alpha = lineAlpha),
                            start = Offset(tankRect.left + tankRect.width * 0.15f + offsetX, lineY),
                            end = Offset(tankRect.left + tankRect.width * 0.75f + offsetX, lineY),
                            strokeWidth = 2.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                } else {
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

                    val baseColor = fuelType.color.toColor()
                    
                    drawPath(
                        path = wave1,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                baseColor.copy(alpha = 0.8f),
                                baseColor
                            ),
                            startY = liquidBaseY - amplitude * 2f,
                            endY = tankRect.bottom
                        )
                    )

                    drawPath(
                        path = wave2,
                        color = baseColor.copy(alpha = 0.35f)
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
    val mockRanges = listOf(
        QualityRange("Diesel", 2.0, 2.8, "0xFFFBBF24", "Normal"),
        QualityRange("Agua", 20.0, null, "0xFF60A5FA", "Peligro"),
        QualityRange("Aire", 0.0, 1.9, "0xFFBEE3F8", "Vacio"),
        QualityRange("Alcohol", 6.0, 18.0, "0xFFF9A8D4", "Alerta"),
        QualityRange("Adulterado", null, 0.0, "0xFFEF4444", "Critico")
    )

    Row(
        modifier = Modifier
            .background(Color(0xFFF8FAFC))
            .padding(24.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(32.dp)
    ) {
        mockRanges.forEach { range ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = range.status,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                AnimatedFuelTank(
                    fuelType = range,
                    level = if (range.label == "Aire") 1.0f else 0.6f,
                    modifier = Modifier.size(width = 120.dp, height = 260.dp)
                )

                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    tonalElevation = 2.dp
                ) {
                    Text(
                        text = range.label,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
