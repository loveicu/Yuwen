package com.wencui.quiz.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.wencui.quiz.QuizViewModel
import com.wencui.quiz.ui.components.PressableGlass
import com.wencui.quiz.ui.glass.GlassShapes
import com.wencui.quiz.ui.glass.GlassStyle
import com.wencui.quiz.ui.glass.glassPanel
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.LocalFireDepartment

/**
 * 主页：学习概览 + 随机练习入口 + 分类列表 + 错题本入口。
 * 全部卡片都是液态玻璃材质，背后光晕漂移时可看到流动折射。
 */
@Composable
fun HomeScreen(
    backdrop: Backdrop,
    dark: Boolean,
    viewModel: QuizViewModel,
    modifier: Modifier = Modifier
) {
    val state = viewModel.uiState

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(Modifier.statusBarsPadding()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "文萃刷题",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "语文文化常识 · 日积月累",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            StatsCard(
                backdrop = backdrop,
                dark = dark,
                totalAnswered = state.totalAnswered,
                totalCorrect = state.totalCorrect,
                mistakes = state.mistakeIds.size,
                bankSize = viewModel.totalCount
            )
        }

        item {
            DailyCard(
                backdrop = backdrop,
                dark = dark,
                dailyDone = state.dailyDone,
                streak = state.streak,
                bestStreak = state.bestStreak,
                onStart = { viewModel.startDaily() }
            )
        }

        item {
            PressableGlass(
                onClick = { viewModel.startRandom() },
                shape = GlassShapes.bar,
                backdrop = backdrop,
                dark = dark,
                style = GlassStyle.Button,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    Modifier.padding(horizontal = 22.dp, vertical = 18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Rounded.Edit,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            "开始随机练习",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "每次 ${QuizViewModel.DEFAULT_SESSION_SIZE} 题 · 混合全部分类",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item {
            Text(
                "分类练习",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 6.dp, start = 4.dp)
            )
        }

        items(viewModel.categories.size) { index ->
            val category = viewModel.categories[index]
            PressableGlass(
                onClick = { viewModel.startCategory(category) },
                shape = GlassShapes.card,
                backdrop = backdrop,
                dark = dark,
                style = GlassStyle.Card,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CategoryIcon(category)
                    Spacer(Modifier.width(14.dp))
                    Text(
                        category,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        "${viewModel.categoryCount(category)} 题",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            if (state.mistakeIds.isNotEmpty()) {
                PressableGlass(
                    onClick = { viewModel.openWrongBook() },
                    shape = GlassShapes.card,
                    backdrop = backdrop,
                    dark = dark,
                    style = GlassStyle.Card,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(Modifier.width(14.dp))
                        Text(
                            "错题本 · ${state.mistakeIds.size} 题",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            "点击重练",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }

        item { Spacer(Modifier.height(28.dp)) }
    }
}

@Composable
private fun DailyCard(
    backdrop: Backdrop,
    dark: Boolean,
    dailyDone: Boolean,
    streak: Int,
    bestStreak: Int,
    onStart: () -> Unit
) {
    PressableGlass(
        onClick = onStart,
        enabled = true,
        shape = GlassShapes.bar,
        backdrop = backdrop,
        dark = dark,
        style = GlassStyle.Button,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (dailyDone) Icons.Rounded.LocalFireDepartment else Icons.Rounded.CalendarToday,
                contentDescription = null,
                tint = if (dailyDone) MaterialTheme.colorScheme.secondary
                else MaterialTheme.colorScheme.tertiary
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    if (dailyDone) "今日已完成 · 明天再来" else "每日一练 · 10 题",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    if (dailyDone) "连续打卡 $streak 天 · 最长 $bestStreak 天"
                    else "每天一套固定卷 · 已连续 $streak 天",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (!dailyDone) {
                Text(
                    "GO",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun StatsCard(
    backdrop: Backdrop,
    dark: Boolean,
    totalAnswered: Int,
    totalCorrect: Int,
    mistakes: Int,
    bankSize: Int
) {
    val accuracy = if (totalAnswered == 0) "--" else "${(totalCorrect * 100 / totalAnswered)}%"
    Box(
        Modifier
            .fillMaxWidth()
            .glassPanel(backdrop, GlassShapes.card, GlassStyle.Card, dark)
    ) {
        Row(
            Modifier.padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            StatCell("已刷题数", "$totalAnswered", Modifier.weight(1f))
            StatCell("正确率", accuracy, Modifier.weight(1f))
            StatCell("错题", "$mistakes", Modifier.weight(1f))
            StatCell("题库", "$bankSize", Modifier.weight(1f))
        }
    }
}

@Composable
private fun StatCell(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CategoryIcon(category: String) {
    val icon: ImageVector = Icons.AutoMirrored.Rounded.MenuBook
    Icon(icon, contentDescription = category, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
}