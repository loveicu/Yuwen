package com.wencui.quiz.ui.glass

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.Shadow

/**
 * 液态玻璃面板参数。
 *
 * @param blurRadius   背景模糊半径（dp），影响「磨砂」强度
 * @param tintAlpha    玻璃本色叠加透明度，亮色玻璃用白、暗色玻璃用黑
 * @param refraction   边缘折射高度（dp），产生玻璃厚度感的透镜效果（仅 Android 12+ 生效）
 */
data class GlassStyle(
    val blurRadius: Float = 24f,
    val tintAlpha: Float = 0.18f,
    val refraction: Float = 6f,
    val withVibrancy: Boolean = true,
    val withHighlight: Boolean = true,
    val withShadow: Boolean = true
) {
    companion object {
        /** 卡片：中等模糊，柔和折射。 */
        val Card = GlassStyle()

        /** 顶栏/底栏：强模糊弱折射，保证内容可读。 */
        val Bar = GlassStyle(blurRadius = 32f, tintAlpha = 0.22f, refraction = 0f)

        /** 按钮：轻模糊，折射明显，突出「玻璃珠」质感。 */
        val Button = GlassStyle(blurRadius = 16f, tintAlpha = 0.12f, refraction = 8f)
    }
}

/**
 * 液态玻璃面板修饰符。
 *
 * 实时采样 [backdrop]（背后内容的逐帧录制），叠加：
 * blur 磨砂 → vibrancy 饱和提升 → lens 边缘折射 → 高光描边 → 投影。
 * 全部在 GPU RenderEffect 管线完成，无逐帧像素回读，保证动画流畅。
 */
fun Modifier.glassPanel(
    backdrop: Backdrop,
    shape: Shape,
    style: GlassStyle = GlassStyle.Card,
    dark: Boolean
): Modifier = drawBackdrop(
    backdrop = backdrop,
    shape = { shape },
    effects = {
        blur(style.blurRadius.dp.toPx())
        if (style.withVibrancy) vibrancy()
        if (style.refraction > 0f) {
            lens(
                refractionHeight = style.refraction.dp.toPx(),
                refractionAmount = 24f.dp.toPx()
            )
        }
    },
    highlight = if (style.withHighlight) {
        { Highlight.Default }
    } else {
        null
    },
    shadow = if (style.withShadow) {
        { Shadow.Default }
    } else {
        null
    },
    onDrawSurface = {
        val tint = if (dark) Color.Black else Color.White
        drawRect(tint.copy(alpha = style.tintAlpha))
    }
)

/** 根据当前暗色模式取玻璃底色。 */
@Composable
fun rememberIsDarkGlass(): Boolean = isSystemInDarkTheme()

/** 常用玻璃形状。 */
@Composable
fun rememberGlassShapes(): GlassShapes = remember { GlassShapes }

object GlassShapes {
    val card = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)
    val bar = androidx.compose.foundation.shape.RoundedCornerShape(28.dp)
    val button = androidx.compose.foundation.shape.RoundedCornerShape(18.dp)
    val pill = androidx.compose.foundation.shape.CircleShape
}