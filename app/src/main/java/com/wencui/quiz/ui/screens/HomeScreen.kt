package com.wencui.quiz.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.wencui.quiz.QuizViewModel
import com.wencui.quiz.SessionMode
import com.wencui.quiz.ui.components.PressableGlass
import com.wencui.quiz.ui.glass.GlassShapes
import com.wencui.quiz.ui.glass.GlassStyle
import com.wencui.quiz.ui.glass.glassPanel

/**
 * 主页：统计 + 每日打卡 + 搜索 + 背题/收藏 + 分类（含掌握度）+ 题库更新。
 */
@Composable
fun HomeScreen(
    backdrop: Backdrop,
    dark: Boolean,
    viewModel: QuizViewModel,
    modifier: Modifier = Modifier
) {
    val state = viewModel.uiState
    val categories = viewModel.categories()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(Modifier.statusBarsPadding()) {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "文萃刷题",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    // 连击火焰（combo >= 3 才显示）
                    if (state.bestCombo >= 3) {
                        Icon(
                            Icons.Rounded.LocalFireDepartment,
                            contentDescription = "最佳连击",
                            tint = Color(0xFFFF7043)
                        )
                        Text(
                            "${state.bestCombo}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF7043)
                        )
                    }
                }
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

        // ===== 搜索框 =====
        item {
            SearchField(
                backdrop = backdrop,
                dark = dark,
                onSearch = { query ->
                    val results = viewModel.searchResults(query)
                    if (results.isNotEmpty()) {
                        // 直接开练第一题所在分类
                        viewModel.startFromSearch(results.first())
                    }
                }
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
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PressableGlass(
                    onClick = { viewModel.startRandom(mode = SessionMode.Memorize) },
                    shape = GlassShapes.card,
                    backdrop = backdrop,
                    dark = dark,
                    style = GlassStyle.Card,
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        Modifier.padding(vertical = 14.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Rounded.Lightbulb,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("背题模式", fontWeight = FontWeight.Medium)
                    }
                }
                PressableGlass(
                    onClick = { viewModel.openFavorites() },
                    shape = GlassShapes.card,
                    backdrop = backdrop,
                    dark = dark,
                    style = GlassStyle.Card,
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        Modifier.padding(vertical = 14.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Rounded.Star,
                            contentDescription = null,
                            tint = Color(0xFFFFB300)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("收藏夹", fontWeight = FontWeight.Medium)
                    }
                }
            }
        }

        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp, start = 4.dp)) {
                Text(
                    "分类练习",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 分类列表 + 掌握度徽章
        items(categories.size) { index ->
            val category = categories[index]
            val accuracy = viewModel.categoryAccuracy(category)
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
                    Icon(
                        Icons.AutoMirrored.Rounded.MenuBook,
                        contentDescription = category,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            category,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium
                        )
                        // 掌握度条
                        if (accuracy >= 0f) {
                            Spacer(Modifier.height(5.dp))
                            Box(
                                Modifier
                                    .fillMaxWidth(0.6f)
                                    .height(4.dp)
                                    .background(
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f),
                                        RoundedCornerShape(2.dp)
                                    )
                            ) {
                                Box(
                                    Modifier
                                        .fillMaxWidth(accuracy.coerceIn(0.04f, 1f))
                                        .height(4.dp)
                                        .background(
                                            when {
                                                accuracy >= 0.8f -> Color(0xFF4CAF50)
                                                accuracy >= 0.6f -> Color(0xFFFFB300)
                                                else -> Color(0xFFEF5350)
                                            },
                                            RoundedCornerShape(2.dp)
                                        )
                                )
                            }
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "${viewModel.categoryCount(category)} 题",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (accuracy >= 0f) {
                            Text(
                                "${(accuracy * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    accuracy >= 0.8f -> Color(0xFF4CAF50)
                                    accuracy >= 0.6f -> Color(0xFFFFB300)
                                    else -> Color(0xFFEF5350)
                                }
                            )
                        }
                    }
                }
            }
        }

        // 错题本入口
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

        // ===== 题库云端更新 =====
        item {
            PressableGlass(
                onClick = { viewModel.updateBank() },
                enabled = !state.bankUpdating,
                shape = GlassShapes.card,
                backdrop = backdrop,
                dark = dark,
                style = GlassStyle.Card,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Rounded.CloudDownload,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            if (state.bankUpdating) "正在检查题库更新…"
                            else "检查题库更新",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            state.bankMessage ?: "题库版本 v${state.bankVersion}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item { Spacer(Modifier.height(28.dp)) }
    }
}

/** 搜索输入框（玻璃材质）。 */
@Composable
private fun SearchField(
    backdrop: Backdrop,
    dark: Boolean,
    onSearch: (String) -> Unit
) {
    var query by remember { mutableStateOf("") }
    Box(
        Modifier
            .fillMaxWidth()
            .glassPanel(backdrop, GlassShapes.bar, GlassStyle.Bar, dark)
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Rounded.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(10.dp))
            Box(Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text(
                        "搜索题目 / 分类 / 关键词，回车开练",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        // 提交按钮（点 GO 搜索）
        if (query.isNotEmpty()) {
            Box(
                Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 8.dp)
                    .clickable { onSearch(query) }
            ) {
                Text(
                    "GO",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(8.dp)
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