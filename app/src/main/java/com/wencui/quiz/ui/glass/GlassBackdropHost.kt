package com.wencui.quiz.ui.glass

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop

/**
 * 液态玻璃核心机制（用法参考 SleepDown-Schedule）：
 *
 * 1. 页面背景层挂 `Modifier.layerBackdrop(backdrop)`，
 *    它的绘制内容会被逐帧录制进这个 [LayerBackdrop]。
 * 2. 玻璃面板挂 [glassPanel]，内部通过 `drawBackdrop` 实时采样 backdrop，
 *    对背后的真实内容做模糊 + 折射 + 高光，形成液态玻璃质感。
 *
 * LayerBackdrop 是实时录制而非截图快照，因此背景滚动/动画时
 * 玻璃效果逐帧跟随，动画流畅不卡顿。
 */

/** 在背景内容上使用：把绘制内容录制成可供玻璃采样的 backdrop。 */
fun Modifier.appBackdrop(backdrop: LayerBackdrop): Modifier =
    layerBackdrop(backdrop)

/** 在 Composable 中创建共享的 backdrop 实例（放在层级最上方一次即可）。 */
@Composable
fun rememberAppBackdrop(): LayerBackdrop = rememberLayerBackdrop()