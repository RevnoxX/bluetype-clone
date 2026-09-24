package com.example.bluetype.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * rememberShimmerBrush — Creates an animated shimmer gradient brush for highlighting active elements.
 *
 * Responsibility: Supplies a linear gradient brush that continuously sweeps across components.
 * Depends on: Compose Animation library.
 * Notes: Used to add a shiny shimmer animation to the connected device card per UI requirements.
 */
@Composable
fun rememberShimmerBrush(
    shimmerColor: Color = Color.White.copy(alpha = 0.35f),
    targetOffset: Float = 1400f,
    durationMs: Int = 2000
): Brush {
    val transition = rememberInfiniteTransition(label = "shimmerTransition")
    val translateAnim by transition.animateFloat(
        initialValue = -400f,
        targetValue = targetOffset,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMs, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslation"
    )

    return Brush.linearGradient(
        colors = listOf(
            Color.Transparent,
            shimmerColor.copy(alpha = 0.1f),
            shimmerColor,
            shimmerColor.copy(alpha = 0.1f),
            Color.Transparent
        ),
        start = Offset(translateAnim - 300f, translateAnim - 300f),
        end = Offset(translateAnim, translateAnim)
    )
}
