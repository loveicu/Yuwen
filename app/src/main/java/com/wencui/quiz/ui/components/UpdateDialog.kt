package com.wencui.quiz.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.wencui.quiz.update.ReleaseInfo
import com.wencui.quiz.ui.glass.GlassShapes
import com.wencui.quiz.ui.glass.GlassStyle
import com.wencui.quiz.ui.glass.glassPanel

/** 更新弹窗状态机。 */
sealed interface UpdateState {
    data object Idle : UpdateState
    data class Available(val release: ReleaseInfo) : UpdateState
    data class Downloading(val progress: Float) : UpdateState
    data object Ready : UpdateState
    data object Failed : UpdateState
}

/**
 * 液态玻璃更新弹窗。
 * 显示新版本号、更新说明、下载进度；下载完成后按钮变为「安装」。
 */
@Composable
fun UpdateDialog(
    state: UpdateState,
    localVersion: String,
    dark: Boolean,
    backdrop: Backdrop,
    onDismiss: () -> Unit,
    onDownload: () -> Unit,
    onInstall: () -> Unit
) {
    val visible = state !is UpdateState.Idle
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + scaleIn(initialScale = 0.9f),
        exit = fadeOut() + scaleOut(targetScale = 0.95f)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            // 半透明遮罩
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(360.dp)
                    .background(Color(0x66000000), RoundedCornerShape(28.dp))
            )
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp)
                    .glassPanel(backdrop, GlassShapes.card, GlassStyle.Bar, dark)
            ) {
                Column(Modifier.padding(22.dp)) {
                    Text(
                        "发现新版本",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(6.dp))
                    val release = (state as? UpdateState.Available)?.release
                    if (release != null) {
                        Text(
                            "v${release.version ?: release.tagName}  (当前 v$localVersion)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(Modifier.height(12.dp))

                    when (state) {
                        is UpdateState.Available -> {
                            release?.body?.takeIf { it.isNotBlank() }?.let { notes ->
                                Text(
                                    notes.take(400),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.height(120.dp)
                                )
                            }
                            Spacer(Modifier.height(16.dp))
                            Row {
                                GlassTextButton("稍后再说", Modifier.weight(1f), onDismiss)
                                Spacer(Modifier.width(10.dp))
                                GlassTextButton(
                                    "立即更新",
                                    Modifier.weight(1f),
                                    onDownload,
                                    emphasize = true
                                )
                            }
                        }

                        is UpdateState.Downloading -> {
                            val p = state.progress
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .background(Color(0x14808C99), RoundedCornerShape(4.dp))
                            ) {
                                Box(
                                    Modifier
                                        .fillMaxWidth(p.coerceIn(0.02f, 1f))
                                        .height(8.dp)
                                        .background(
                                            Brush.horizontalGradient(
                                                listOf(
                                                    MaterialTheme.colorScheme.primary,
                                                    MaterialTheme.colorScheme.tertiary
                                                )
                                            ),
                                            RoundedCornerShape(4.dp)
                                        )
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "下载中 ${(p * 100).toInt()}%",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                        }

                        is UpdateState.Ready -> {
                            Text(
                                "下载完成，点击安装",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(Modifier.height(16.dp))
                            GlassTextButton("安装", Modifier.fillMaxWidth(), onInstall, emphasize = true)
                        }

                        is UpdateState.Failed -> {
                            Text(
                                "下载失败，请检查网络后重试",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(Modifier.height(16.dp))
                            Row {
                                GlassTextButton("取消", Modifier.weight(1f), onDismiss)
                                Spacer(Modifier.width(10.dp))
                                GlassTextButton("重试", Modifier.weight(1f), onDownload, emphasize = true)
                            }
                        }

                        else -> Unit
                    }
                }
            }
        }
    }
}

@Composable
private fun GlassTextButton(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    emphasize: Boolean = false
) {
    Box(
        modifier
            .background(
                if (emphasize) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
                RoundedCornerShape(14.dp)
            )
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = if (emphasize) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurface
        )
    }
}