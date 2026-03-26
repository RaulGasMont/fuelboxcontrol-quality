package com.gasmonsoft.fuelboxcontrol.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = FscPrimaryDark,
    onPrimary = FscOnPrimaryDark,
    primaryContainer = FscPrimaryContainerDark,
    onPrimaryContainer = FscOnPrimaryContainerDark,

    secondary = FscSecondaryDark,
    onSecondary = FscOnSecondaryDark,
    secondaryContainer = FscSecondaryContainerDark,
    onSecondaryContainer = FscOnSecondaryContainerDark,

    tertiary = FscTertiaryDark,
    onTertiary = FscOnTertiaryDark,
    tertiaryContainer = FscTertiaryContainerDark,
    onTertiaryContainer = FscOnTertiaryContainerDark,

    background = FscBackgroundDark,
    onBackground = FscOnBackgroundDark,
    surface = FscSurfaceDark,
    onSurface = FscOnSurfaceDark,

    outline = FscOutlineDark,
    error = FscErrorDark,
    onError = FscOnErrorDark
)

private val LightColorScheme = lightColorScheme(
    primary = FscPrimaryLight,
    onPrimary = FscOnPrimaryLight,
    primaryContainer = FscPrimaryContainerLight,
    onPrimaryContainer = FscOnPrimaryContainerLight,

    secondary = FscSecondaryLight,
    onSecondary = FscOnSecondaryLight,
    secondaryContainer = FscSecondaryContainerLight,
    onSecondaryContainer = FscOnSecondaryContainerLight,

    tertiary = FscTertiaryLight,
    onTertiary = FscOnTertiaryLight,
    tertiaryContainer = FscTertiaryContainerLight,
    onTertiaryContainer = FscOnTertiaryContainerLight,

    background = FscBackgroundLight,
    onBackground = FscOnBackgroundLight,
    surface = FscSurfaceLight,
    onSurface = FscOnSurfaceLight,

    outline = FscOutlineLight,
    error = FscErrorLight,
    onError = FscOnErrorLight
)

@Composable
fun FuelBoxControlTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}