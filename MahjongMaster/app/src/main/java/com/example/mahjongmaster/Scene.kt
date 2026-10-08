package com.example.mahjongmaster

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Oyun sahnesi: tema gradyanı + seigaiha (wave) deseni + yavaş hareket eden
 * ambiyans ışıkları. Tüm ekranların ortak zeminidir.
 */
@Composable
fun SceneBackdrop(
    theme: BoardTheme,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val drift = rememberInfiniteTransition(label = "sceneDrift")
    val phase by drift.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(18_000, easing = LinearEasing), RepeatMode.Restart),
        label = "phase"
    )
    val bob by drift.animateFloat(
        0f, (2f * PI).toFloat(),
        infiniteRepeatable(tween(11_000, easing = LinearEasing), RepeatMode.Restart),
        label = "bob"
    )
    Box(
        modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(theme.sceneTop, theme.sceneMid, theme.sceneBottom)))
            .seigaiha(theme)
    ) {
        AmbientGlow(
            color = theme.glowA,
            sizeFrac = 0.72f,
            xFrac = 0.08f + 0.04f * sin(bob),
            yFrac = 0.04f + 0.03f * cos(bob),
            alpha = 0.28f
        )
        AmbientGlow(
            color = theme.glowB,
            sizeFrac = 0.68f,
            xFrac = 0.78f + 0.05f * cos(phase * 2f * PI.toFloat()),
            yFrac = 0.72f + 0.04f * sin(phase * 2f * PI.toFloat()),
            alpha = 0.22f
        )
        AmbientGlow(
            color = theme.glowC,
            sizeFrac = 0.5f,
            xFrac = 0.92f,
            yFrac = 0.38f + 0.05f * sin(bob + 1.2f),
            alpha = 0.14f
        )
        content()
    }
}

@Composable
private fun BoxScope.AmbientGlow(
    color: Color,
    sizeFrac: Float,
    xFrac: Float,
    yFrac: Float,
    alpha: Float
) {
    Canvas(Modifier.fillMaxSize()) {
        val r = size.minDimension * sizeFrac / 2f
        val c = Offset(size.width * xFrac, size.height * yFrac)
        drawCircle(
            Brush.radialGradient(listOf(color.copy(alpha = alpha), Color.Transparent), c, r),
            radius = r,
            center = c
        )
    }
}

/** Klasik Japon seigaiha (üst üste binen dalga yayları) deseni. */
private fun Modifier.seigaiha(theme: BoardTheme): Modifier = drawWithCache {
    val spacing = size.minDimension * 0.085f
    val radius = spacing * 0.92f
    val stroke = Stroke(width = spacing * 0.045f)
    val color = Color.White.copy(alpha = 0.045f)
    val pathCache = Path()
    onDrawBehind {
        val cols = (size.width / spacing).toInt() + 3
        val rows = (size.height / (spacing * 0.52f)).toInt() + 3
        for (row in 0 until rows) {
            val y = row * spacing * 0.52f
            val odd = row % 2 != 0
            for (col in 0 until cols) {
                val x = col * spacing + if (odd) spacing / 2f else 0f
                translate(x, y) {
                    for (k in 1..3) {
                        val r = radius * k / 3f
                        pathCache.reset()
                        pathCache.addArc(
                            androidx.compose.ui.geometry.Rect(-r, -r, r, r),
                            200f, 140f
                        )
                        drawPath(pathCache, color, style = stroke)
                    }
                }
            }
        }
        // Üst/alt vignette — tahtayı merkeze çeker
        drawRect(
            Brush.verticalGradient(
                0f to theme.sceneTop.copy(alpha = 0.35f),
                0.18f to Color.Transparent,
                0.82f to Color.Transparent,
                1f to theme.sceneBottom.copy(alpha = 0.55f)
            )
        )
    }
}
