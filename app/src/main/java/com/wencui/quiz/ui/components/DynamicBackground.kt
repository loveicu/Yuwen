package com.wencui.quiz.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.delay
import java.util.Calendar
import kotlin.math.min
import kotlin.math.sin

/**
 * 动态渐变背景 v3 —— 「肉眼可见的流动」：
 *
 * 结构（自底向上）：
 * 1. 基底竖向渐变（保证整体亮度可读）
 * 2. 主色大色团：巨大 radialGradient，中心以 ~45 秒周期绕屏游移
 * 3. 辅色大色团：反向相位游移，与主色交融 → 整屏色调持续流动
 * 4. 六团李萨如光斑：小尺度灵动
 *
 * 昼夜色板拉大色差：清晨金橙 / 白天天蓝 / 黄昏橙紫 / 夜晚蓝紫，
 * 深色模式有专属暗色板。每分钟跟随真实时钟插值。
 */

/** 帧时钟：自首次组合起的秒数，每帧更新。 */
@Composable
private fun rememberFrameSeconds(): State<Float> = produceState(0f) {
    val start = withFrameNanos { it }
    while (true) {
        withFrameNanos { now -> value = (now - start) / 1_000_000_000f }
    }
}

/** 真实时钟的「当日小时浮点数」，每分钟刷新。 */
@Composable
private fun rememberHourOfDay(): State<Float> = produceState(0f) {
    while (true) {
        val cal = Calendar.getInstance()
        value = cal.get(Calendar.HOUR_OF_DAY) + cal.get(Calendar.MINUTE) / 60f
        delay(60_000L)
    }
}

/** 小光斑规格。 */
private data class Blob(
    val baseX: Float, val baseY: Float,
    val ampX: Float, val ampY: Float,
    val freqX: Float, val freqY: Float,
    val phase: Float,
    val radius: Float,
    val hue: Float,
    val hueDrift: Float,
    val alpha: Float,
    val breatheFreq: Float
)

private val BLOBS = listOf(
    Blob(0.20f, 0.15f, 0.10f, 0.08f, 0.31f, 0.23f, 0.0f, 0.40f, 210f, 28f, 0.42f, 0.21f),
    Blob(0.85f, 0.40f, 0.08f, 0.10f, 0.19f, 0.27f, 1.4f, 0.34f, 35f, 22f, 0.34f, 0.17f),
    Blob(0.35f, 0.85f, 0.12f, 0.06f, 0.26f, 0.34f, 2.8f, 0.37f, 350f, 30f, 0.30f, 0.24f),
    Blob(0.70f, 0.75f, 0.09f, 0.11f, 0.23f, 0.17f, 4.1f, 0.30f, 160f, 26f, 0.26f, 0.19f),
    Blob(0.50f, 0.50f, 0.15f, 0.12f, 0.13f, 0.29f, 5.2f, 0.44f, 270f, 24f, 0.20f, 0.13f),
    Blob(0.60f, 0.20f, 0.07f, 0.09f, 0.37f, 0.21f, 6.0f, 0.26f, 45f, 20f, 0.24f, 0.28f)
)

/** 昼夜色板：主色团 / 辅色团 / 基底顶 / 基底底。 */
private data class DayPalette(
    val hour: Float,
    val main: Color, val accent: Color,
    val baseTop: Color, val baseBottom: Color
)

// 浅色模式：色差拉满，每个时段有鲜明的色彩倾向
private val LIGHT_PALETTES = listOf(
    DayPalette(6f,  Color(0xFFFFC97A), Color(0xFFFFE3C2), Color(0xFFFFF6EA), Color(0xFFFDFBF6)), // 清晨 金橙
    DayPalette(11f, Color(0xFF8FC7FF), Color(0xFFCFE8FF), Color(0xFFF0F7FF), Color(0xFFFDFBF6)), // 白天 天蓝
    DayPalette(17f, Color(0xFFFFA184), Color(0xFFE2C4F5), Color(0xFFFFF0E9), Color(0xFFFDFBF6)), // 黄昏 橙×紫
    DayPalette(21f, Color(0xFF96ACE8), Color(0xFFC3BCEE), Color(0xFFE9EBF8), Color(0xFFF5F4FA)), // 夜晚 蓝紫
    DayPalette(29f, Color(0xFFFFC97A), Color(0xFFFFE3C2), Color(0xFFFFF6EA), Color(0xFFFDFBF6))  // 环回清晨
)

// 深色模式：暗基底上用较高饱和的深色团
private val DARK_PALETTES = listOf(
    DayPalette(6f,  Color(0xFF31456E), Color(0xFF463B66), Color(0xFF161B26), Color(0xFF101318)),
    DayPalette(11f, Color(0xFF2E4E80), Color(0xFF35446E), Color(0xFF171C28), Color(0xFF12151D)),
    DayPalette(17f, Color(0xFF583566), Color(0xFF663A4E), Color(0xFF1B1622), Color(0xFF131017)),
    DayPalette(21f, Color(0xFF26375F), Color(0xFF2B2750), Color(0xFF12151F), Color(0xFF0C0E15)),
    DayPalette(29f, Color(0xFF31456E), Color(0xFF463B66), Color(0xFF161B26), Color(0xFF101318))
)

/** 小时插值取色板。0-6 点环回 24-30 区间（深夜→黎明）。 */
private fun paletteAt(hour: Float, frames: List<DayPalette>): DayPalette {
    val h = if (hour < frames.first().hour) hour + 24f else hour
    var i = 0
    while (i < frames.lastIndex && frames[i + 1].hour <= h) i++
    val a = frames[i]
    val b = frames[min(i + 1, frames.lastIndex)]
    val span = (b.hour - a.hour).coerceAtLeast(1f)
    val f = ((h - a.hour) / span).coerceIn(0f, 1f)
    return DayPalette(
        hour = h,
        main = lerpColor(a.main, b.main, f),
        accent = lerpColor(a.accent, b.accent, f),
        baseTop = lerpColor(a.baseTop, b.baseTop, f),
        baseBottom = lerpColor(a.baseBottom, b.baseBottom, f)
    )
}

private fun lerpColor(a: Color, b: Color, f: Float): Color = Color(
    red = a.red + (b.red - a.red) * f,
    green = a.green + (b.green - a.green) * f,
    blue = a.blue + (b.blue - a.blue) * f,
    alpha = 1f
)

@Composable
fun DynamicAuroraBackground(
    modifier: Modifier = Modifier,
    dark: Boolean = false
) {
    val time by rememberFrameSeconds()
    val hour by rememberHourOfDay()

    val palette = remember(hour, dark) {
        paletteAt(hour, if (dark) DARK_PALETTES else LIGHT_PALETTES)
    }

    // 色团透明度：深色下略高（暗底需要更多色才可感知）
    val mainAlpha = if (dark) 0.62f else 0.48f
    val accentAlpha = if (dark) 0.50f else 0.38f
    val blobScale = if (dark) 0.72f else 1f

    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val t = time

        // 1. 基底竖向渐变
        drawRect(
            Brush.verticalGradient(
                listOf(palette.baseTop, palette.baseBottom)
            )
        )

        // 2. 主色大色团：~45 秒周期在屏幕上游移
        val ax = (0.30f + 0.34f * sin(t * 0.14f)) * w
        val ay = (0.28f + 0.30f * sin(t * 0.11f + 1.2f)) * h
        val aR = 1.05f * min(w, h) * (1f + 0.08f * sin(t * 0.09f))
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(palette.main.copy(alpha = mainAlpha), Color.Transparent),
                center = Offset(ax, ay),
                radius = aR
            ),
            radius = aR,
            center = Offset(ax, ay)
        )

        // 3. 辅色大色团：反向游移，与主色交融
        val bx = (0.72f + 0.30f * sin(t * 0.12f + 3.1f)) * w
        val by = (0.74f + 0.28f * sin(t * 0.15f + 0.6f)) * h
        val bR = 0.95f * min(w, h) * (1f + 0.10f * sin(t * 0.11f + 2f))
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(palette.accent.copy(alpha = accentAlpha), Color.Transparent),
                center = Offset(bx, by),
                radius = bR
            ),
            radius = bR,
            center = Offset(bx, by)
        )

        // 4. 六团李萨如小光斑
        for (b in BLOBS) {
            val cx = (b.baseX + b.ampX * sin(t * b.freqX + b.phase)) * w
            val cy = (b.baseY + b.ampY * sin(t * b.freqY + b.phase * 1.73f)) * h
            val breathe = 1f + 0.14f * sin(t * b.breatheFreq + b.phase)
            val r = b.radius * min(w, h) * breathe
            val hue = ((b.hue + b.hueDrift * sin(t * 0.05f + b.phase)) % 360f + 360f) % 360f
            val color = Color.hsv(hue, 0.62f, 1f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(color.copy(alpha = b.alpha * blobScale), Color.Transparent),
                    center = Offset(cx, cy),
                    radius = r
                ),
                radius = r,
                center = Offset(cx, cy)
            )
        }
    }
}