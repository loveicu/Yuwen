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
 * 真正的动态背景（v2 修复版）：
 *
 * 1. 【帧时钟驱动】withFrameNanos 逐帧推进，光斑沿李萨如曲线运动，
 *    频率互质 → 轨迹永不重复；
 * 2. 【呼吸与色相漂移】半径起伏 + 色相环游移；
 * 3. 【昼夜色调】真实时钟插值，0-6 点正确落在「深夜」区间；
 * 4. 【深色模式】暗色主题使用专属低亮度配色，不再发白。
 *
 * 移除了背景层的手势检测：背景处于事件分发链末端，
 * 挂手势既收不到被卡片覆盖区域的事件，也会干扰列表滚动，得不偿失。
 */

/** 帧时钟：返回自首次组合起的秒数，每帧更新。 */
@Composable
private fun rememberFrameSeconds(): State<Float> = produceState(0f) {
    val start = withFrameNanos { it }
    while (true) {
        withFrameNanos { now -> value = (now - start) / 1_000_000_000f }
    }
}

/** 真实时钟的「当日小时浮点数」，每分钟刷新一次。 */
@Composable
private fun rememberHourOfDay(): State<Float> = produceState(0f) {
    while (true) {
        val cal = Calendar.getInstance()
        value = cal.get(Calendar.HOUR_OF_DAY) + cal.get(Calendar.MINUTE) / 60f
        delay(60_000L)  // 60 秒后刷新
    }
}

/** 光斑规格：基点/振幅为画布比例，频率为弧度每秒。 */
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

// 六团光斑：频率互质 → 李萨如轨迹不重复；相位错开 → 永不排成一列
private val BLOBS = listOf(
    Blob(0.20f, 0.15f, 0.10f, 0.08f, 0.31f, 0.23f, 0.0f, 0.45f, 210f, 28f, 0.50f, 0.21f),
    Blob(0.85f, 0.40f, 0.08f, 0.10f, 0.19f, 0.27f, 1.4f, 0.38f, 35f, 22f, 0.36f, 0.17f),
    Blob(0.35f, 0.85f, 0.12f, 0.06f, 0.26f, 0.34f, 2.8f, 0.42f, 350f, 30f, 0.32f, 0.24f),
    Blob(0.70f, 0.75f, 0.09f, 0.11f, 0.23f, 0.17f, 4.1f, 0.34f, 160f, 26f, 0.28f, 0.19f),
    Blob(0.50f, 0.50f, 0.15f, 0.12f, 0.13f, 0.29f, 5.2f, 0.50f, 270f, 24f, 0.20f, 0.13f),
    Blob(0.60f, 0.20f, 0.07f, 0.09f, 0.37f, 0.21f, 6.0f, 0.28f, 45f, 20f, 0.26f, 0.28f)
)

/** 昼夜底色关键帧：小时 + 三段渐变色（顶/中/底）。 */
private data class DayFrame(val hour: Float, val top: Color, val mid: Color, val bottom: Color)

private val LIGHT_KEYFRAMES = listOf(
    DayFrame(6f,  Color(0xFFFFF3E0), Color(0xFFFFF8EE), Color(0xFFFDFBF6)),  // 清晨 金粉
    DayFrame(11f, Color(0xFFEAF3FF), Color(0xFFF7F3EA), Color(0xFFFDFBF6)),  // 白天 蓝宣
    DayFrame(17f, Color(0xFFFFE9D6), Color(0xFFFBEDE3), Color(0xFFFDFBF6)),  // 黄昏 暖橙
    DayFrame(21f, Color(0xFFC9D2EE), Color(0xFFD8DCEF), Color(0xFFE8E6EF)),  // 夜晚 淡蓝紫（加深）
    DayFrame(29f, Color(0xFFFFF3E0), Color(0xFFFFF8EE), Color(0xFFFDFBF6))   // 环回清晨(=6+24)
)

/** 暗色模式专属关键帧：全时段低亮度，深夜更深。 */
private val DARK_KEYFRAMES = listOf(
    DayFrame(6f,  Color(0xFF1A2130), Color(0xFF161B26), Color(0xFF12151D)),  // 清晨 冷蓝
    DayFrame(11f, Color(0xFF1C2433), Color(0xFF171C28), Color(0xFF12151D)),  // 白天 蓝灰
    DayFrame(17f, Color(0xFF241F2E), Color(0xFF1B1826), Color(0xFF141218)),  // 黄昏 暖紫
    DayFrame(21f, Color(0xFF131722), Color(0xFF10131C), Color(0xFF0C0E15)),  // 夜晚 深蓝
    DayFrame(29f, Color(0xFF1A2130), Color(0xFF161B26), Color(0xFF12151D))
)

/** 按当前小时在关键帧间线性插值底色。0-6 点环回到 24-30 区间（深夜→清晨）。 */
private fun timeOfDayColors(hour: Float, frames: List<DayFrame>): List<Color> {
    val h = if (hour < frames.first().hour) hour + 24f else hour
    var i = 0
    while (i < frames.lastIndex && frames[i + 1].hour <= h) i++
    val a = frames[i]
    val b = frames[min(i + 1, frames.lastIndex)]
    val span = (b.hour - a.hour).coerceAtLeast(1f)
    val f = ((h - a.hour) / span).coerceIn(0f, 1f)
    return listOf(
        lerpColor(a.top, b.top, f),
        lerpColor(a.mid, b.mid, f),
        lerpColor(a.bottom, b.bottom, f)
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

    // 真实时钟驱动：每分钟刷新；深浅色各有一套关键帧
    val baseColors = remember(hour, dark) {
        timeOfDayColors(hour, if (dark) DARK_KEYFRAMES else LIGHT_KEYFRAMES)
    }

    // 光斑在暗色下降低不透明度，避免夜里过于刺眼
    val blobAlphaScale = if (dark) 0.62f else 1f

    Canvas(modifier) {
        val w = size.width
        val h = size.height

        // 1. 昼夜底色
        drawRect(Brush.verticalGradient(baseColors))

        // 2. 李萨如流体光斑
        for (b in BLOBS) {
            val t = time
            val cx = (b.baseX + b.ampX * sin(t * b.freqX + b.phase)) * w
            val cy = (b.baseY + b.ampY * sin(t * b.freqY + b.phase * 1.73f)) * h
            val breathe = 1f + 0.14f * sin(t * b.breatheFreq + b.phase)
            val r = b.radius * min(w, h) * breathe
            val hue = ((b.hue + b.hueDrift * sin(t * 0.05f + b.phase)) % 360f + 360f) % 360f
            val color = Color.hsv(hue, 0.62f, 1f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(color.copy(alpha = b.alpha * blobAlphaScale), Color.Transparent),
                    center = Offset(cx, cy),
                    radius = r
                ),
                radius = r,
                center = Offset(cx, cy)
            )
        }
    }
}