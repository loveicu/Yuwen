package com.wencui.quiz.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import java.util.Calendar
import kotlin.math.min
import kotlin.math.sin

/**
 * 真正的动态背景：
 *
 * 1. 【帧时钟驱动】以 withFrameNanos 逐帧推进（60fps 连续动画），
 *    每个光斑沿李萨如曲线（Lissajous）运动——频率互质，轨迹永不重复；
 * 2. 【呼吸与色相漂移】光斑半径随时间起伏，颜色在色环上缓慢游移；
 * 3. 【触摸交互】手指按下时，指尖处升起一团光晕，松手后缓缓消散；
 * 4. 【昼夜色调】基于设备真实时间（清晨/白天/黄昏/夜晚）对底色做柔和插值。
 *
 * 性能说明：时间值只被 Canvas 的 draw lambda 读取，
 * 动画不触发测量/布局，仅重绘，GPU 负担极小。
 */

/** 帧时钟：返回自首次组合起的秒数，每帧更新。 */
@Composable
private fun rememberFrameSeconds(): State<Float> = produceState(0f) {
    val start = withFrameNanos { it }
    while (true) {
        withFrameNanos { now -> value = (now - start) / 1_000_000_000f }
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

private val DAY_KEYFRAMES = listOf(
    DayFrame(6f,  Color(0xFFFFF3E0), Color(0xFFFFF8EE), Color(0xFFFDFBF6)),  // 清晨 金粉
    DayFrame(11f, Color(0xFFEAF3FF), Color(0xFFF7F3EA), Color(0xFFFDFBF6)),  // 白天 蓝宣
    DayFrame(17f, Color(0xFFFFE9D6), Color(0xFFFBEDE3), Color(0xFFFDFBF6)),  // 黄昏 暖橙
    DayFrame(21f, Color(0xFFE3E8F7), Color(0xFFF0EEF5), Color(0xFFF7F6FA)),  // 夜晚 淡蓝紫
    DayFrame(29f, Color(0xFFFFF3E0), Color(0xFFFFF8EE), Color(0xFFFDFBF6))   // 环回清晨(=6+24)
)

/** 按当前小时在关键帧间线性插值底色。 */
private fun timeOfDayColors(hour: Float): List<Color> {
    var i = 0
    while (i < DAY_KEYFRAMES.lastIndex && DAY_KEYFRAMES[i + 1].hour <= hour) i++
    val a = DAY_KEYFRAMES[i]
    val b = DAY_KEYFRAMES[min(i + 1, DAY_KEYFRAMES.lastIndex)]
    val span = (b.hour - a.hour).coerceAtLeast(1f)
    val f = ((hour - a.hour) / span).coerceIn(0f, 1f)
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

    // 触摸光晕：按下点亮，松手渐散
    var pressPoint by remember { mutableStateOf(Offset.Zero) }
    val pressGlow = remember { Animatable(0f) }

    // 底色每分钟重算一次（真实时间昼夜渐变）
    val baseColors = remember(time.toInt() / 60) {
        val cal = Calendar.getInstance()
        val hour = cal.get(Calendar.HOUR_OF_DAY) + cal.get(Calendar.MINUTE) / 60f
        timeOfDayColors(hour)
    }

    Canvas(
        modifier
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { pos ->
                        pressPoint = pos
                        pressGlow.animateTo(1f, tween(180))
                        tryAwaitRelease()
                        pressGlow.animateTo(0f, tween(900))
                    }
                )
            }
    ) {
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
            // 色相缓慢漂移
            val hue = ((b.hue + b.hueDrift * sin(t * 0.05f + b.phase)) % 360f + 360f) % 360f
            val color = Color.hsv(hue, 0.62f, 1f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(color.copy(alpha = b.alpha), Color.Transparent),
                    center = Offset(cx, cy),
                    radius = r
                ),
                radius = r,
                center = Offset(cx, cy)
            )
        }

        // 3. 触摸光晕（指尖升起，松手消散）
        if (pressGlow.value > 0.01f) {
            val a = pressGlow.value
            val r = w * 0.38f * (0.6f + 0.4f * a)
            val glow = if (dark) Color.hsv(215f, 0.55f, 1f, 0.45f * a)
            else Color.hsv(210f, 0.60f, 1f, 0.40f * a)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(glow, Color.Transparent),
                    center = pressPoint,
                    radius = r
                ),
                radius = r,
                center = pressPoint
            )
        }
    }
}