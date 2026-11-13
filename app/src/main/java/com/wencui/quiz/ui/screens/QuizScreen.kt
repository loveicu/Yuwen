package com.wencui.quiz.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.wencui.quiz.QuizViewModel
import com.wencui.quiz.ui.components.PressableGlass
import com.wencui.quiz.ui.glass.GlassShapes
import com.wencui.quiz.ui.glass.GlassStyle
import com.wencui.quiz.ui.glass.glassPanel
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/**
 * 答题页：题目玻璃卡片 + 选项列表 + 提交按钮。
 * 切题时使用水平滑动 + 淡入淡出的组合动画，配合玻璃折射产生「液态翻页」感。
 */
@Composable
fun QuizScreen(
    backdrop: Backdrop,
    dark: Boolean,
    viewModel: QuizViewModel,
    modifier: Modifier = Modifier
) {
    val state = viewModel.uiState
    val question = state.currentQuestion ?: return
    val haptic = LocalHapticFeedback.current

    // 作答后触觉反馈：答对轻震、答错重震
    LaunchedEffect(state.chosenIndex) {
        if (state.chosenIndex != null) {
            val q = state.currentQuestion ?: return@LaunchedEffect
            val correct = state.chosenIndex == q.answerIndex
            haptic.performHapticFeedback(
                if (correct) HapticFeedbackType.LongPress else HapticFeedbackType.TextHandleMove
            )
        }
    }

    Column(modifier.fillMaxSize()) {
        // 顶部：返回 + 进度
        Row(
            Modifier
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PressableGlass(
                onClick = { viewModel.goHome() },
                shape = GlassShapes.button,
                backdrop = backdrop,
                dark = dark
            ) {
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "返回",
                    Modifier.padding(10.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                "${state.currentIndex + 1} / ${state.questions.size}" +
                        (state.sessionCategory?.let { " · $it" } ?: ""),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }

        // 答题进度条
        Box(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .height(4.dp)
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Color(0x14808C99),
                        RoundedCornerShape(2.dp)
                    )
            )
            Box(
                Modifier
                    .fillMaxWidth(fraction = (state.currentIndex + 1f) / state.questions.size)
                    .fillMaxHeight()
                    .background(
                        Brush.horizontalGradient(
                            listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)
                        ),
                        RoundedCornerShape(2.dp)
                    )
            )
        }

        // 题目区（带切题动画）
        AnimatedContent(
            targetState = state.currentIndex,
            transitionSpec = {
                if (targetState > initialState) {
                    (slideInHorizontally(tween(320)) { it / 3 } + fadeIn(tween(320)))
                        .togetherWith(slideOutHorizontally(tween(220)) { -it / 4 } + fadeOut(tween(220)))
                } else {
                    (slideInHorizontally(tween(320)) { -it / 3 } + fadeIn(tween(320)))
                        .togetherWith(slideOutHorizontally(tween(220)) { it / 4 } + fadeOut(tween(220)))
                }
            },
            label = "questionSwitch"
        ) { index ->
            val q = state.questions.getOrNull(index) ?: return@AnimatedContent
            val chosen = if (index == state.currentIndex) state.chosenIndex else null

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .glassPanel(backdrop, GlassShapes.card, GlassStyle.Card, dark)
                    ) {
                        Column(Modifier.padding(22.dp)) {
                            Text(
                                q.category,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(Modifier.height(10.dp))
                            Text(
                                q.stem,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                lineHeight = MaterialTheme.typography.headlineSmall.lineHeight
                            )
                        }
                    }
                }

                items(q.options.size) { optIndex ->
                    OptionCard(
                        backdrop = backdrop,
                        dark = dark,
                        label = "ABCD"[optIndex.coerceAtMost(3)].toString(),
                        text = q.options[optIndex],
                        chosen = chosen == optIndex,
                        revealed = chosen != null,
                        isAnswer = optIndex == q.answerIndex,
                        onClick = { viewModel.choose(optIndex) }
                    )
                }

                item {
                    Spacer(Modifier.height(4.dp))
                }

                item {
                    if (chosen != null) {
                        Column {
                            ExplanationCard(backdrop, dark, q.explanation, chosen == q.answerIndex)
                            Spacer(Modifier.height(12.dp))
                            PressableGlass(
                                onClick = { viewModel.next() },
                                shape = GlassShapes.bar,
                                backdrop = backdrop,
                                dark = dark,
                                style = GlassStyle.Button,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    if (state.isLastQuestion) "查看结算" else "下一题",
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 14.dp)
                                )
                            }
                        }
                    }
                }

                item { Spacer(Modifier.navigationBarsPadding()) }
            }
        }
    }
}

@Composable
private fun OptionCard(
    backdrop: Backdrop,
    dark: Boolean,
    label: String,
    text: String,
    chosen: Boolean,
    revealed: Boolean,
    isAnswer: Boolean,
    onClick: () -> Unit
) {
    val shape = GlassShapes.button

    PressableGlass(
        onClick = onClick,
        enabled = !revealed,
        shape = shape,
        backdrop = backdrop,
        dark = dark,
        style = GlassStyle.Card,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (revealed && isAnswer) Color(0xFF2E7D32)
                else if (revealed && chosen) Color(0xFFC62828)
                else MaterialTheme.colorScheme.primary,
                modifier = Modifier.width(28.dp)
            )
            Text(
                text,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            if (revealed && chosen) {
                Icon(
                    if (isAnswer) Icons.Rounded.Check else Icons.Rounded.Close,
                    contentDescription = null,
                    tint = if (isAnswer) Color(0xFF2E7D32) else Color(0xFFC62828)
                )
            } else if (revealed && isAnswer) {
                Icon(Icons.Rounded.Check, contentDescription = null, tint = Color(0xFF2E7D32))
            }
        }
    }
}

@Composable
private fun ExplanationCard(
    backdrop: Backdrop,
    dark: Boolean,
    explanation: String,
    correct: Boolean
) {
    if (explanation.isBlank()) return
    Box(
        Modifier
            .fillMaxWidth()
            .glassPanel(backdrop, GlassShapes.card, GlassStyle.Bar, dark)
    ) {
        Row(Modifier.padding(18.dp)) {
            Text(
                if (correct) "✓ 回答正确" else "✗ 回答错误",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (correct) Color(0xFF2E7D32) else Color(0xFFC62828)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                explanation,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}