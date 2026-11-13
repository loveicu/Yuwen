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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.wencui.quiz.QuizViewModel
import com.wencui.quiz.ui.components.PressableGlass
import com.wencui.quiz.ui.glass.GlassShapes
import com.wencui.quiz.ui.glass.GlassStyle
import com.wencui.quiz.ui.glass.glassPanel

/** 结算页：本轮正确率环形展示 + 再来一轮 / 回主页。 */
@Composable
fun ResultScreen(
    backdrop: Backdrop,
    dark: Boolean,
    viewModel: QuizViewModel,
    modifier: Modifier = Modifier
) {
    val state = viewModel.uiState
    val accuracy = (state.sessionAccuracy * 100).toInt()

    Column(modifier.fillMaxSize()) {
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
                "练习结算",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(Modifier.height(16.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .glassPanel(backdrop, GlassShapes.card, GlassStyle.Card, dark)
                ) {
                    Column(
                        Modifier
                            .padding(vertical = 36.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val praise = when {
                            accuracy == 100 -> "满腹经纶！"
                            accuracy >= 80 -> "学有所成"
                            accuracy >= 60 -> "渐入佳境"
                            else -> "温故知新"
                        }
                        Text(
                            "$accuracy%",
                            style = MaterialTheme.typography.displayLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            praise,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(Modifier.height(18.dp))
                        Text(
                            "本轮 ${state.records.size} 题 · 正确 ${state.sessionCorrect} 题",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PressableGlass(
                        onClick = { viewModel.startRandom() },
                        shape = GlassShapes.card,
                        backdrop = backdrop,
                        dark = dark,
                        style = GlassStyle.Button,
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            Modifier.padding(vertical = 16.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Rounded.Refresh,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("再来一轮", fontWeight = FontWeight.SemiBold)
                        }
                    }
                    PressableGlass(
                        onClick = { viewModel.goHome() },
                        shape = GlassShapes.card,
                        backdrop = backdrop,
                        dark = dark,
                        style = GlassStyle.Button,
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            Modifier.padding(vertical = 16.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Rounded.Home,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("回主页", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            item { Spacer(Modifier.navigationBarsPadding()) }
        }
    }
}