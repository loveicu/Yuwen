package com.wencui.quiz.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Shape
import com.kyant.backdrop.Backdrop
import com.wencui.quiz.ui.glass.GlassStyle
import com.wencui.quiz.ui.glass.glassPanel

/**
 * 通用玻璃按压容器：按压缩放弹回 + 液态玻璃面板。
 *
 * 动画说明：spring 弹性曲线驱动 scale，只触发 graphicsLayer 属性变化，
 * 不触发重组测量，保证按反馈帧率稳定。
 */
@Composable
fun PressableGlass(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape,
    backdrop: Backdrop,
    dark: Boolean,
    style: GlassStyle = GlassStyle.Button,
    enabled: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "glassPressScale"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .glassPanel(backdrop, shape, style, dark)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = onClick
            ),
        content = content
    )
}