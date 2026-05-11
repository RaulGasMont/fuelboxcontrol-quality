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

@Composable
fun AnimatedFuelTank(
    fuelType: QualityRange?,
    level: Float,
    modifier: Modifier = Modifier,
    showInfo: Boolean = true,
    isLoading: Boolean
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
        modifier = modifier.size(width = 150.dp, height = 310.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 6.dp.toPx()
            val neckWidth = size.width * 0.28f
            val neckHeight = size.height * 0.075f
            val tankTop = neckHeight + strokeWidth
            val tankBottom = size.height - strokeWidth
            val tankRight = size.width - strokeWidth
            val tankRect = Rect(
                left = strokeWidth,
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
                if (fuelType?.label == null) return@clipPath
                if (fuelType.label.equals("Aire", ignoreCase = true)) {
                    // Fondo degradado más notable (cielo)
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFFBAE6FD).copy(alpha = 0.45f),
                                Color(0xFFE0F2FE).copy(alpha = 0.20f)
                            ),
                            startY = tankRect.top,
                            endY = tankRect.bottom
                        ),
                        topLeft = Offset(tankRect.left, tankRect.top),
                        size = androidx.compose.ui.geometry.Size(tankRect.width, tankRect.height)
                    )

                    // Partículas flotando hacia arriba con más presencia
                    val airProgress = waveShift1 / (2f * PI).toFloat()
                    val particleData = listOf(
                        floatArrayOf(0.20f, 0.00f, 6f),
                        floatArrayOf(0.50f, 0.28f, 4.5f),
                        floatArrayOf(0.78f, 0.55f, 5.5f),
                        floatArrayOf(0.35f, 0.72f, 4f),
                        floatArrayOf(0.65f, 0.15f, 5f),
                        floatArrayOf(0.12f, 0.44f, 4f),
                        floatArrayOf(0.88f, 0.87f, 6f),
                        floatArrayOf(0.55f, 0.62f, 3.5f),
                        floatArrayOf(0.42f, 0.38f, 4.5f),
                        floatArrayOf(0.70f, 0.92f, 3.5f),
                    )

                    particleData.forEach { (xFrac, phaseOffset, radiusDp) ->
                        val p = (airProgress + phaseOffset) % 1.0f
                        val y = tankRect.bottom - p * tankRect.height
                        val sway =
                            sin((p * 4f * PI + phaseOffset * 10f).toFloat()) * tankRect.width * 0.07f
                        val x = (tankRect.left + xFrac * tankRect.width + sway)
                            .coerceIn(tankRect.left, tankRect.right)

                        val alpha = when {
                            p < 0.12f -> p / 0.12f * 0.85f
                            p > 0.88f -> (1f - p) / 0.12f * 0.85f
                            else -> 0.85f
                        }
                        val r = radiusDp.dp.toPx()

                        // Resplandor exterior (Glow)
                        drawCircle(
                            color = Color(0xFF7DD3FC).copy(alpha = alpha * 0.4f),
                            radius = r * 1.5f,
                            center = Offset(x, y)
                        )

                        drawCircle(
                            color = Color(0xFFBAE6FD).copy(alpha = alpha),
                            radius = r,
                            center = Offset(x, y)
                        )
                        // Reflejo interior más brillante
                        drawCircle(
                            color = Color.White.copy(alpha = alpha * 0.85f),
                            radius = r * 0.42f,
                            center = Offset(x - r * 0.22f, y - r * 0.22f)
                        )
                    }

                    // Líneas de viento horizontales animadas (más gruesas y visibles)
                    val windProgress = waveShift2 / (2f * PI).toFloat()
                    listOf(0.25f, 0.50f, 0.73f).forEachIndexed { i, yFrac ->
                        val offsetX =
                            cos((windProgress * 2f * PI + i * 2.1f).toFloat()) * tankRect.width * 0.15f
                        val lineY = tankRect.top + yFrac * tankRect.height
                        val lineAlpha =
                            0.35f + 0.15f * sin((windProgress * 2f * PI + i * 1.3f).toFloat())
                        drawLine(
                            color = Color(0xFF38BDF8).copy(alpha = lineAlpha),
                            start = Offset(tankRect.left + tankRect.width * 0.10f + offsetX, lineY),
                            end = Offset(tankRect.left + tankRect.width * 0.80f + offsetX, lineY),
                            strokeWidth = 3.dp.toPx(),
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
                    // Colores derivados para profundidad
                    val topColor = baseColor.copy(alpha = 0.75f)
                    val midColor = baseColor.copy(alpha = 0.92f)

                    // Cuerpo principal del líquido con gradiente de 3 niveles
                    drawPath(
                        path = wave1,
                        brush = Brush.verticalGradient(
                            colors = listOf(topColor, midColor, baseColor),
                            startY = liquidBaseY - amplitude,
                            endY = tankRect.bottom
                        )
                    )

                    // Segunda capa de onda para efecto de profundidad/transparencia
                    drawPath(
                        path = wave2,
                        color = baseColor.copy(alpha = 0.25f)
                    )

                    // Brillo en la superficie (Specular Highlight)
                    val surfacePath = buildWavePath(
                        left = tankRect.left,
                        right = tankRect.right,
                        bottom = liquidBaseY + 10f, // Solo una franja delgada
                        baseY = liquidBaseY,
                        amplitude = amplitude,
                        phase = waveShift1,
                        wavelength = wavelength
                    )
                    drawPath(
                        path = surfacePath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.35f),
                                Color.Transparent
                            ),
                            startY = liquidBaseY - amplitude,
                            endY = liquidBaseY + amplitude
                        )
                    )

                    // Reflejo lateral (Efecto cristal/vidrio del tanque)
                    drawLine(
                        color = Color.White.copy(alpha = 0.15f),
                        start = Offset(
                            x = tankRect.left + tankRect.width * 0.18f,
                            y = tankRect.top + 20f
                        ),
                        end = Offset(
                            x = tankRect.left + tankRect.width * 0.18f,
                            y = tankRect.bottom - 20f
                        ),
                        strokeWidth = tankRect.width * 0.06f,
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

        if (fuelType?.label != null) {
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
                }
            }
        }

        if (isLoading) {
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
                    LoadingDotsText()
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
            .padding(24.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(32.dp)
    ) {
        QualityRange.entries.forEach { range ->
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
                    modifier = Modifier.size(width = 120.dp, height = 260.dp),
                    isLoading = false
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
