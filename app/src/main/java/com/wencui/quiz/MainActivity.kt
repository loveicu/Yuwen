package com.wencui.quiz

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.wencui.quiz.data.QuizRepository
import com.wencui.quiz.ui.components.AuroraBackground
import com.wencui.quiz.ui.glass.appBackdrop
import com.wencui.quiz.ui.screens.HomeScreen
import com.wencui.quiz.ui.screens.QuizScreen
import com.wencui.quiz.ui.screens.ResultScreen
import com.wencui.quiz.ui.screens.WrongBookScreen
import com.wencui.quiz.ui.theme.WenCuiTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WenCuiTheme {
                WenCuiApp()
            }
        }
    }
}

@Composable
fun WenCuiApp() {
    val dark = isSystemInDarkTheme()
    val backdrop: LayerBackdrop = rememberLayerBackdrop()
    val context = LocalContext.current
    val repo = remember(context) { QuizRepository.get(context) }
    val viewModel: QuizViewModel =
        androidx.lifecycle.viewmodel.compose.viewModel(factory = QuizViewModel.factory(repo))

    Box(Modifier.fillMaxSize()) {
        // 背景层：液态玻璃采样的源头。layerBackdrop 实时录制此层内容
        AuroraBackground(Modifier.appBackdrop(backdrop))

        // 前景层：所有玻璃面板实时采样上面的背景
        AnimatedContent(
            targetState = viewModel.uiState.screen,
            transitionSpec = {
                (fadeIn(tween(260)) + androidx.compose.animation.slideInHorizontally(tween(260)) { it / 6 })
                    .togetherWith(
                        fadeOut(tween(200)) + androidx.compose.animation.slideOutHorizontally(tween(200)) { -it / 6 }
                    )
            },
            label = "screenSwitch"
        ) { screen ->
            when (screen) {
                Screen.Home -> HomeScreen(backdrop, dark, viewModel)
                Screen.Quiz -> QuizScreen(backdrop, dark, viewModel)
                Screen.Result -> ResultScreen(backdrop, dark, viewModel)
                Screen.WrongBook -> WrongBookScreen(backdrop, dark, viewModel)
            }
        }
    }
}