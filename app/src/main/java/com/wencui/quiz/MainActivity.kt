package com.wencui.quiz

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.BackHandler
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.wencui.quiz.data.QuizRepository
import com.wencui.quiz.ui.components.DynamicAuroraBackground
import com.wencui.quiz.ui.components.UpdateDialog
import com.wencui.quiz.ui.components.UpdateState
import com.wencui.quiz.ui.glass.appBackdrop
import com.wencui.quiz.ui.screens.FavoritesScreen
import com.wencui.quiz.ui.screens.HomeScreen
import com.wencui.quiz.ui.screens.QuizScreen
import com.wencui.quiz.ui.screens.ResultScreen
import com.wencui.quiz.ui.screens.WrongBookScreen
import com.wencui.quiz.ui.theme.WenCuiTheme
import com.wencui.quiz.update.ApkDownloader
import com.wencui.quiz.update.ApkInstaller
import com.wencui.quiz.update.UpdateChecker
import com.wencui.quiz.update.isNewerVersion
import kotlinx.coroutines.delay

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

    // ===== 应用内更新系统 =====
    var updateState by remember { mutableStateOf<UpdateState>(UpdateState.Idle) }
    var downloadedApk by remember { mutableStateOf<java.io.File?>(null) }
    val localVersion = remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "0"
        }.getOrDefault("0")
    }

    // 启动 3 秒后静默检查更新（不阻塞首屏）
    LaunchedEffect(Unit) {
        delay(3000)
        val release = UpdateChecker.check() ?: return@LaunchedEffect
        // tag 恒为 "latest"，真实版本号需从 APK 文件名 / Release 名称解析
        val remote = release.version ?: release.tagName
        if (isNewerVersion(remote, localVersion) && release.apkAsset != null) {
            updateState = UpdateState.Available(release)
        }
    }

    fun startDownload(url: String) {
        updateState = UpdateState.Downloading(0f)
        // 后台线程下载，主线程回调 UI（无需引入额外协程依赖）
        Thread {
            val file = ApkDownloader.download(
                context.applicationContext, url
            ) { p ->
                context.mainExecutor.execute {
                    if (updateState is UpdateState.Downloading) {
                        updateState = UpdateState.Downloading(p)
                    }
                }
            }
            context.mainExecutor.execute {
                updateState = if (file != null) {
                    downloadedApk = file
                    UpdateState.Ready
                } else {
                    UpdateState.Failed
                }
            }
        }.start()
    }

    // 返回键路由：非主页时先回主页，主页则交给系统（退出）
    BackHandler(enabled = viewModel.uiState.screen != Screen.Home) {
        viewModel.goHome()
    }

    Box(Modifier.fillMaxSize()) {
        // 背景层：液态玻璃采样的源头。layerBackdrop 实时录制此层内容
        DynamicAuroraBackground(Modifier.appBackdrop(backdrop), dark)

        // 前景层：所有玻璃面板实时采样上面的背景
        AnimatedContent(
            targetState = viewModel.uiState.screen,
            transitionSpec = {
                (fadeIn(tween(260)) + slideInHorizontally(tween(260)) { it / 6 })
                    .togetherWith(
                        fadeOut(tween(200)) + slideOutHorizontally(tween(200)) { -it / 6 }
                    )
            },
            label = "screenSwitch"
        ) { screen ->
            when (screen) {
                Screen.Home -> HomeScreen(backdrop, dark, viewModel)
                Screen.Quiz -> QuizScreen(backdrop, dark, viewModel)
                Screen.Result -> ResultScreen(backdrop, dark, viewModel)
                Screen.WrongBook -> WrongBookScreen(backdrop, dark, viewModel)
                Screen.Favorites -> FavoritesScreen(backdrop, dark, viewModel)
            }
        }

        // 更新弹窗（覆盖在最上层）
        UpdateDialog(
            state = updateState,
            localVersion = localVersion,
            dark = dark,
            backdrop = backdrop,
            onDismiss = { updateState = UpdateState.Idle },
            onDownload = {
                val release = (updateState as? UpdateState.Available)?.release
                release?.apkAsset?.let { startDownload(it.browserDownloadUrl) }
            },
            onInstall = {
                downloadedApk?.let { ApkInstaller.install(context, it) }
            }
        )
    }
}