package com.example.finfit.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
    darkColorScheme(
        primary = DarkPrimary,
        onPrimary = Color.White,
        primaryContainer = DarkPrimary,
        secondary = DarkSecondary,
        tertiary = DarkTertiary,
        background = DarkBackground,
        surface = DarkSurface,
        onBackground = DarkOnSurface,
        onSurface = DarkOnSurface,
        surfaceVariant = DarkSurfaceVariant,
        onSurfaceVariant = DarkOnSurfaceVariant
    )

private val LightColorScheme =
    lightColorScheme(
        primary = LightPrimary,
        onPrimary = Color.White,
        primaryContainer = LightPrimaryContainer,
        secondary = LightSecondary,
        secondaryContainer = LightSecondaryContainer,
        tertiary = LightTertiary,
        tertiaryContainer = LightTertiaryContainer,
        background = LightBackground,
        surface = LightSurface,
        onBackground = LightOnSurface,
        onSurface = LightOnSurface,
        surfaceVariant = LightSurfaceContainerLow,
        onSurfaceVariant = LightOnSurfaceVariant
    )

@Composable
fun FinFitTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Set dynamicColor to false by default as the user wants specific brand colors
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

        MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
