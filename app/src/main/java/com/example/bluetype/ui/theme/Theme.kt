package com.example.bluetype.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * Theme — Defines the Material 3 design system for BlueType.
 *
 * Responsibility: Supplies color schemes, typography, and dynamic theming.
 * Depends on: [Color.kt], [Type.kt].
 * Notes: Provides high contrast and clean minimal aesthetics per design specification.
 */
private val DarkColorScheme = darkColorScheme(
    primary = SlatePrimaryDark,
    secondary = SlateSecondaryDark,
    tertiary = SlateTertiaryDark,
    background = SlateBackgroundDark,
    surface = SlateSurfaceDark,
    surfaceVariant = SlateSurfaceVariantDark
)

private val LightColorScheme = lightColorScheme(
    primary = SlatePrimaryLight,
    secondary = SlateSecondaryLight,
    tertiary = SlateTertiaryLight,
    background = SlateBackgroundLight,
    surface = SlateSurfaceLight,
    surfaceVariant = SlateSurfaceVariantLight
)

@Composable
fun BluetypeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
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