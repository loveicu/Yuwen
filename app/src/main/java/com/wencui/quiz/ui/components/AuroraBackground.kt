package com.wencui.quiz.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlin.math.sin

/**
 * 液态玻璃的「画布」：多层柔和渐变背景。
 *
 * 玻璃面板的模糊/折射全部来自对这层背景的实时采样，
 * 三团彩色光晕以不同周期缓慢漂移，玻璃里的倒影也会跟着流动，
 * 让整个界面有「活水」般的通透感。
 */
@Composable
fun AuroraBackground(modifier: Modifier = Modifier) {

    Box(
        modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFFEAF3FF), Color(0xFFF7F3EA), Color(0xFFFDFBF6))
                )
            )
    ) {
        // 一个极慢的全局相位（0..2π 循环），三团光晕用不同相位差错开
        val transition = rememberInfiniteTransition(label = "aurora")
        val phase by transition.animateFloat(
            initialValue = 0f,
            targetValue = (2 * Math.PI).toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 24_000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "auroraPhase"
        )

        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            fun glow(
                color: Color,
                radius: Float,
                cx: Float, cy: Float,
                ax: Float, ay: Float,
                ph: Float,
                alpha: Float
            ) {
                val center = Offset(
                    x = cx + ax * sin(phase + ph),
                    y = cy + ay * sin(phase * 0.83f + ph * 1.37f)
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(color.copy(alpha = alpha), Color.Transparent),
                        center = center,
                        radius = radius
                    ),
                    radius = radius,
                    center = center
                )
            }

            glow(
                color = Color(0xFF64B5F6), radius = w * 0.42f,
                cx = w * 0.22f, cy = h * 0.18f, ax = w * 0.06f, ay = h * 0.05f,
                ph = 0f, alpha = 0.50f
            )
            glow(
                color = Color(0xFFFFB74D), radius = w * 0.36f,
                cx = w * 0.82f, cy = h * 0.42f, ax = w * 0.05f, ay = h * 0.06f,
                ph = 2.1f, alpha = 0.34f
            )
            glow(
                color = Color(0xFFEF9A9A), radius = w * 0.40f,
                cx = w * 0.38f, cy = h * 0.86f, ax = w * 0.07f, ay = h * 0.04f,
                ph = 4.2f, alpha = 0.30f
            )
        }
    }
}