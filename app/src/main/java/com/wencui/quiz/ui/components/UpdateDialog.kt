package com.wencui.quiz.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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

/** 从 Release body 提取干净的更新说明：去掉 CI 模板行与 markdown 符号。 */
private fun sanitizeNotes(body: String?): String {
    if (body.isNullOrBlank()) return "修复体验问题，优化整体流畅度。"
    return body.lines()
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .filterNot { it.startsWith(">") }           // 引导行
        .filterNot { it.startsWith("自动构建于") }   // CI 模板行
        .filterNot { it.startsWith("<!--") }         // 注释
        .joinToString("\n")
        .replace(Regex("^#+\\s*"), "")               // 标题井号
        .take(300)
}

/**
 * 液态玻璃更新弹窗（v2）：
 * 全屏半透明遮罩 + 居中卡片 + 版本徽章 + 可滚动说明 + 实心主按钮。
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
    androidx.compose.animation.AnimatedVisibility(
        visible = visible,
        enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.scaleIn(initialScale = 0.92f),
        exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.scaleOut(targetScale = 0.95f)
    ) {
        // 全屏遮罩：点击空白处不关闭下载中，避免误触
        Box(
            Modifier
                .fillMaxSize()
                .background(Color(0x88000000))
                .clickable(enabled = state !is UpdateState.Downloading) { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier
                    .padding(24.dp)
                    .width(320.dp)
                    .glassPanel(backdrop, GlassShapes.card, GlassStyle.Card, dark)
                    .clickable { } // 空消费：吞掉卡片内点击，防止穿透到遮罩关闭
            ) {
                Column(Modifier.padding(24.dp)) {
                    val release = (state as? UpdateState.Available)?.release
                    val remote = release?.version ?: release?.tagName ?: ""

                    // 标题行：图标徽章 + 版本号
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFF1E88E5), Color(0xFF1565C0))
                                    ),
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                "v${remote.ifBlank { localVersion }}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "发现新版本",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "当前版本 v$localVersion",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(14.dp))

                    when (state) {
                        is UpdateState.Available -> {
                            // 更新说明：净化 + 可滚动，不再硬裁
                            val notes = sanitizeNotes(release?.body)
                            Text(
                                notes,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .heightIn(max = 160.dp)
                                    .verticalScroll(rememberScrollState())
                            )
                            Spacer(Modifier.height(20.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                GlassTextButton(
                                    "稍后再说", Modifier.weight(1f),
                                    filled = false, onClick = onDismiss
                                )
                                GlassTextButton(
                                    "立即更新", Modifier.weight(1f),
                                    filled = true, onClick = onDownload
                                )
                            }
                        }

                        is UpdateState.Downloading -> {
                            val p = state.progress
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                                    .background(Color(0x14808C99), RoundedCornerShape(5.dp))
                            ) {
                                Box(
                                    Modifier
                                        .fillMaxWidth(p.coerceIn(0.03f, 1f))
                                        .height(10.dp)
                                        .background(
                                            Brush.horizontalGradient(
                                                listOf(Color(0xFF1E88E5), Color(0xFF7E57C2))
                                            ),
                                            RoundedCornerShape(5.dp)
                                        )
                                )
                            }
                            Spacer(Modifier.height(10.dp))
                            Text(
                                "下载中 ${(p * 100).toInt()}% · 请保持网络畅通",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        is UpdateState.Ready -> {
                            Text(
                                "下载完成，已就绪",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(20.dp))
                            GlassTextButton("安装更新", Modifier.fillMaxWidth(), filled = true, onClick = onInstall)
                        }

                        is UpdateState.Failed -> {
                            Text(
                                "下载失败，请检查网络后重试",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(Modifier.height(20.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                GlassTextButton("取消", Modifier.weight(1f), filled = false, onClick = onDismiss)
                                GlassTextButton("重试", Modifier.weight(1f), filled = true, onClick = onDownload)
                            }
                        }

                        else -> Unit
                    }
                }
            }
        }
    }
}

/** 弹窗按钮：主操作实心渐变，次操作浅玻璃。 */
@Composable
private fun GlassTextButton(
    text: String,
    modifier: Modifier = Modifier,
    filled: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier
            .then(
                if (filled) Modifier.background(
                    Brush.linearGradient(listOf(Color(0xFF1E88E5), Color(0xFF1565C0))),
                    RoundedCornerShape(14.dp)
                )
                else Modifier.background(
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                    RoundedCornerShape(14.dp)
                )
            )
            .clickable { onClick() }
            .padding(vertical = 13.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = if (filled) Color.White else MaterialTheme.colorScheme.onSurface
        )
    }
}