package com.local.linkpin.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 暖调"焦糖奶茶"配色：奶油底 + 陶土橙强调色，柔和阴影与暖白卡片，主打质感而非科技霓虹。
 *
 * 颜色层级约定：
 * - background 奶油米色，页面底色（配合 [LinkPinGradients.background] 渐变）；
 * - surface 暖白，卡片/输入框/弹窗容器；
 * - primary 陶土橙，主按钮、强调文字、选中态；
 * - secondary 焦糖棕，次级操作（编辑等）；
 * - tertiary 沙金，装饰性描边与光晕。
 */
private val WarmColors = lightColorScheme(
    primary = Color(0xFFC2653F),
    onPrimary = Color(0xFFFFF8F4),
    primaryContainer = Color(0xFFFBE4D8),
    onPrimaryContainer = Color(0xFF6E2F15),
    secondary = Color(0xFF96683F),
    onSecondary = Color(0xFFFFF8F1),
    secondaryContainer = Color(0xFFF6E7D2),
    onSecondaryContainer = Color(0xFF4A3320),
    tertiary = Color(0xFFA9805B),
    onTertiary = Color(0xFFFFF8F2),
    tertiaryContainer = Color(0xFFF2E3D3),
    onTertiaryContainer = Color(0xFF4C3A28),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
    background = Color(0xFFFAF5EF),
    onBackground = Color(0xFF33271E),
    surface = Color(0xFFFFFDFA),
    onSurface = Color(0xFF33271E),
    surfaceVariant = Color(0xFFF1E7DB),
    onSurfaceVariant = Color(0xFF7B6A5A),
    surfaceTint = Color(0xFFC2653F),
    outline = Color(0xFFCFBEA9),
    outlineVariant = Color(0xFFEDE1D3),
    inverseSurface = Color(0xFF3A3029),
    inverseOnSurface = Color(0xFFFAF3EC),
    inversePrimary = Color(0xFFF2B68C),
    scrim = Color(0xFF221812),
)

private val LinkPinTypography = Typography(
    displaySmall = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.4).sp,
    ),
    headlineSmall = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 25.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.2).sp,
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 21.sp,
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
        lineHeight = 17.sp,
        letterSpacing = 0.1.sp,
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.8.sp,
    ),
    labelMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp,
    ),
)

private val LinkPinShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(30.dp),
)

/** 全局渐变与光晕，供各页面复用，保证暖调质感一致。 */
object LinkPinGradients {
    /** 品牌强调渐变：暖沙金 → 陶土橙 → 琥珀深橙。 */
    val accent: Brush = Brush.linearGradient(
        colors = listOf(
            Color(0xFFEFA062),
            Color(0xFFDA7A50),
            Color(0xFFBF5A3B),
        ),
    )

    /** 页面背景渐变：奶油白 → 暖米色。 */
    val background: Brush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFFDFAF6),
            Color(0xFFF6EDE2),
        ),
    )

    /** 柔和暖光晕，用于页面顶部的氛围光。 */
    val glow: Brush = Brush.radialGradient(
        colors = listOf(
            Color(0x30E0915B),
            Color(0x00E0915B),
        ),
    )
}

/**
 * 全局主题：固定暖色调，不跟随系统深色。
 * 自用工具以品牌一致性优先，系统切深色时仍保持暖色界面。
 */
@Composable
fun LinkPinTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = WarmColors,
        typography = LinkPinTypography,
        shapes = LinkPinShapes,
        content = content,
    )
}
