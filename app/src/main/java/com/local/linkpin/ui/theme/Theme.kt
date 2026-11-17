package com.local.linkpin.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 深空霓虹配色：深蓝黑背景 + 青/蓝紫/紫霓虹强调色，主打科技感。
 */
private val DarkColors = darkColorScheme(
    primary = Color(0xFF22D3EE),
    onPrimary = Color(0xFF00323F),
    primaryContainer = Color(0xFF0C3B48),
    onPrimaryContainer = Color(0xFFA5F1FF),
    secondary = Color(0xFF818CF8),
    onSecondary = Color(0xFF11154A),
    secondaryContainer = Color(0xFF252B63),
    onSecondaryContainer = Color(0xFFE0E2FF),
    tertiary = Color(0xFFC084FC),
    onTertiary = Color(0xFF2E1155),
    tertiaryContainer = Color(0xFF45286D),
    onTertiaryContainer = Color(0xFFF0DBFF),
    error = Color(0xFFFF7D87),
    onError = Color(0xFF430016),
    errorContainer = Color(0xFF5E1024),
    onErrorContainer = Color(0xFFFFD9DD),
    background = Color(0xFF090F1E),
    onBackground = Color(0xFFE4EAF7),
    surface = Color(0xFF111827),
    onSurface = Color(0xFFE4EAF7),
    surfaceVariant = Color(0xFF1B2740),
    onSurfaceVariant = Color(0xFF94A6C8),
    outline = Color(0xFF2D3C5C),
    outlineVariant = Color(0xFF1E2A47),
    inverseSurface = Color(0xFFE4EAF7),
    inverseOnSurface = Color(0xFF151F36),
    inversePrimary = Color(0xFF07889F),
    scrim = Color(0xFF000000),
)

/**
 * 浅色主题：冷调灰蓝背景 + 青蓝强调色（保留给跟随系统的场景）。
 */
private val LightColors = lightColorScheme(
    primary = Color(0xFF0284C7),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6F1FF),
    onPrimaryContainer = Color(0xFF003952),
    secondary = Color(0xFF4F46E5),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE1E1FF),
    onSecondaryContainer = Color(0xFF1B1B6B),
    tertiary = Color(0xFF7C3AED),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFEDE2FF),
    onTertiaryContainer = Color(0xFF32035F),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    background = Color(0xFFF4F7FD),
    onBackground = Color(0xFF101A2C),
    surface = Color.White,
    onSurface = Color(0xFF101A2C),
    surfaceVariant = Color(0xFFE6ECF8),
    onSurfaceVariant = Color(0xFF4C5B78),
    outline = Color(0xFFB6C2DA),
    outlineVariant = Color(0xFFD6DFEF),
)

private val LinkPinTypography = Typography(
    headlineSmall = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 26.sp,
        lineHeight = 32.sp,
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 28.sp,
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.1.sp,
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodySmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.3.sp,
    ),
)

private val LinkPinShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

/** 全局渐变与光晕，供各页面复用，保证科技感风格一致。 */
object LinkPinGradients {
    /** 品牌强调渐变：青 → 蓝紫 → 紫。 */
    val accent: Brush = Brush.linearGradient(
        colors = listOf(
            Color(0xFF22D3EE),
            Color(0xFF6366F1),
            Color(0xFFA855F7),
        ),
    )

    /** 页面背景渐变：上部深蓝 → 底部近黑。 */
    val background: Brush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF111B36),
            Color(0xFF090F1E),
        ),
    )
}

@Composable
fun LinkPinTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = LinkPinTypography,
        shapes = LinkPinShapes,
        content = content,
    )
}
