package com.shubhamthorat.flint.presentation.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Flint Brand Color Tokens: "Spark -> Flame -> Stories"
 * Primary: Warm Amber / Orange
 * Secondary: Deep Violet
 * Accent: Coral
 */
object FlintColorTokens {
    val PrimaryAmber = Color(0xFFFF6B00)
    val PrimaryAmberVariant = Color(0xFFFF8800)
    val PrimaryAmberLight = Color(0xFFFFD4B3)

    val SecondaryViolet = Color(0xFF4A154B)
    val SecondaryVioletDark = Color(0xFF2C003E)
    val SecondaryVioletLight = Color(0xFF7A2B7C)

    val AccentCoral = Color(0xFFFF4F5A)
    val AccentCoralLight = Color(0xFFFFB3B8)

    // Light Theme Surfaces
    val BackgroundLight = Color(0xFFFAF8F5)
    val SurfaceLight = Color(0xFFFFFFFF)
    val SurfaceVariantLight = Color(0xFFF2EFEA)
    val OnBackgroundLight = Color(0xFF1C1B1F)
    val OnSurfaceLight = Color(0xFF1C1B1F)
    val TextSecondaryLight = Color(0xFF5F6368)

    // Dark Theme Surfaces
    val BackgroundDark = Color(0xFF121214)
    val SurfaceDark = Color(0xFF1E1E22)
    val SurfaceVariantDark = Color(0xFF2A2A30)
    val OnBackgroundDark = Color(0xFFE6E1E5)
    val OnSurfaceDark = Color(0xFFE6E1E5)
    val TextSecondaryDark = Color(0xFF9AA0A6)

    // Status Colors
    val Success = Color(0xFF00C853)
    val Warning = Color(0xFFFFAB00)
    val Error = Color(0xFFD32F2F)
}

@Immutable
data class FlintColorScheme(
    val primary: Color,
    val primaryVariant: Color,
    val secondary: Color,
    val accent: Color,
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val onBackground: Color,
    val onSurface: Color,
    val textSecondary: Color,
    val isDark: Boolean
)

val LightColorScheme = FlintColorScheme(
    primary = FlintColorTokens.PrimaryAmber,
    primaryVariant = FlintColorTokens.PrimaryAmberVariant,
    secondary = FlintColorTokens.SecondaryViolet,
    accent = FlintColorTokens.AccentCoral,
    background = FlintColorTokens.BackgroundLight,
    surface = FlintColorTokens.SurfaceLight,
    surfaceVariant = FlintColorTokens.SurfaceVariantLight,
    onBackground = FlintColorTokens.OnBackgroundLight,
    onSurface = FlintColorTokens.OnSurfaceLight,
    textSecondary = FlintColorTokens.TextSecondaryLight,
    isDark = false
)

val DarkColorScheme = FlintColorScheme(
    primary = FlintColorTokens.PrimaryAmberVariant,
    primaryVariant = FlintColorTokens.PrimaryAmber,
    secondary = FlintColorTokens.SecondaryVioletLight,
    accent = FlintColorTokens.AccentCoral,
    background = FlintColorTokens.BackgroundDark,
    surface = FlintColorTokens.SurfaceDark,
    surfaceVariant = FlintColorTokens.SurfaceVariantDark,
    onBackground = FlintColorTokens.OnBackgroundDark,
    onSurface = FlintColorTokens.OnSurfaceDark,
    textSecondary = FlintColorTokens.TextSecondaryDark,
    isDark = true
)

val LocalFlintColorScheme = staticCompositionLocalOf { LightColorScheme }
