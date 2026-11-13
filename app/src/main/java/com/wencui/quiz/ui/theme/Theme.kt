package com.wencui.quiz.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// 配色：青花蓝为魂，宣纸米色为底，朱砂红为点缀 —— 呼应语文文化主题
val QingHua = Color(0xFF1E88E5)
val QingHuaDeep = Color(0xFF1565C0)
val XuanZhi = Color(0xFFF7F3EA)
val ZhuSha = Color(0xFFE53935)
val MoHei = Color(0xFF263238)
val JinShi = Color(0xFFFFB300)

private val LightColors = lightColorScheme(
    primary = QingHua,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6E7FF),
    onPrimaryContainer = Color(0xFF0D47A1),
    secondary = ZhuSha,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDAD6),
    onSecondaryContainer = Color(0xFF7F1D1D),
    tertiary = JinShi,
    background = XuanZhi,
    onBackground = MoHei,
    surface = Color.White,
    onSurface = MoHei,
    surfaceVariant = Color(0xFFEAE4D8),
    onSurfaceVariant = Color(0xFF5F6B73),
    outline = Color(0xFF8D9499)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9ECAFF),
    onPrimary = Color(0xFF00339B),
    primaryContainer = Color(0xFF0D47A1),
    onPrimaryContainer = Color(0xFFD6E7FF),
    secondary = Color(0xFFFFB4AB),
    onSecondary = Color(0xFF5C0F0F),
    secondaryContainer = Color(0xFF7F1D1D),
    onSecondaryContainer = Color(0xFFFFDAD6),
    tertiary = JinShi,
    background = Color(0xFF14161A),
    onBackground = Color(0xFFE4E2DE),
    surface = Color(0xFF1B1E23),
    onSurface = Color(0xFFE4E2DE),
    surfaceVariant = Color(0xFF3A3F45),
    onSurfaceVariant = Color(0xFFBFC8CE),
    outline = Color(0xFF8D9499)
)

@Composable
fun WenCuiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}