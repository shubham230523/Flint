package com.shubhamthorat.flint.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

@Composable
fun FlintTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    com.shubhamthorat.flint.adjustSystemBars(darkTheme)
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val typography = FlintTypography()
    val spacing = FlintSpacing()
    val radius = FlintRadius()
    val elevation = FlintElevation()

    val m3ColorScheme = if (darkTheme) {
        darkColorScheme(
            primary = colorScheme.primary,
            secondary = colorScheme.secondary,
            background = colorScheme.background,
            surface = colorScheme.surface,
            onPrimary = colorScheme.surface,
            onSecondary = colorScheme.surface,
            onBackground = colorScheme.onBackground,
            onSurface = colorScheme.onSurface
        )
    } else {
        lightColorScheme(
            primary = colorScheme.primary,
            secondary = colorScheme.secondary,
            background = colorScheme.background,
            surface = colorScheme.surface,
            onPrimary = colorScheme.surface,
            onSecondary = colorScheme.surface,
            onBackground = colorScheme.onBackground,
            onSurface = colorScheme.onSurface
        )
    }

    CompositionLocalProvider(
        LocalFlintColorScheme provides colorScheme,
        LocalFlintTypography provides typography,
        LocalFlintSpacing provides spacing,
        LocalFlintRadius provides radius,
        LocalFlintElevation provides elevation
    ) {
        MaterialTheme(
            colorScheme = m3ColorScheme,
            content = content
        )
    }
}

object FlintTheme {
    val colors: FlintColorScheme
        @Composable
        @ReadOnlyComposable
        get() = LocalFlintColorScheme.current

    val typography: FlintTypography
        @Composable
        @ReadOnlyComposable
        get() = LocalFlintTypography.current

    val spacing: FlintSpacing
        @Composable
        @ReadOnlyComposable
        get() = LocalFlintSpacing.current

    val radius: FlintRadius
        @Composable
        @ReadOnlyComposable
        get() = LocalFlintRadius.current

    val elevation: FlintElevation
        @Composable
        @ReadOnlyComposable
        get() = LocalFlintElevation.current
}
