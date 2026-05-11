package com.gasmonsoft.fbccalidad.ui.detector.screen

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import kotlinx.coroutines.delay

@Composable
fun LoadingDotsText(
    maxDots: Int = 3,
    intervalMillis: Long = 500L
) {
    var dotCount by remember {
        mutableIntStateOf(0)
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(intervalMillis)
            dotCount = if (dotCount >= maxDots) {
                0
            } else {
                dotCount + 1
            }
        }
    }

    Text(
        text = ".".repeat(dotCount),
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = FontWeight.Medium,
        color = Color.White
    )
}