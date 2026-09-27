package com.shubhamthorat.flint.presentation.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class FlintSpacing(
    val extraSmall: Dp = 4.dp,
    val small: Dp = 8.dp,
    val medium: Dp = 16.dp,
    val large: Dp = 24.dp,
    val extraLarge: Dp = 32.dp,
    val huge: Dp = 48.dp
)

@Immutable
data class FlintRadius(
    val small: Dp = 8.dp,
    val medium: Dp = 12.dp,
    val large: Dp = 16.dp,
    val extraLarge: Dp = 24.dp,
    val round: Dp = 100.dp
)

@Immutable
data class FlintElevation(
    val none: Dp = 0.dp,
    val low: Dp = 2.dp,
    val medium: Dp = 4.dp,
    val high: Dp = 8.dp
)

val LocalFlintSpacing = staticCompositionLocalOf { FlintSpacing() }
val LocalFlintRadius = staticCompositionLocalOf { FlintRadius() }
val LocalFlintElevation = staticCompositionLocalOf { FlintElevation() }
