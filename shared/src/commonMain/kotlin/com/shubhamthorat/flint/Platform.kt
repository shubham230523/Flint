package com.shubhamthorat.flint

import androidx.compose.runtime.Composable

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform

@Composable
expect fun adjustSystemBars(darkTheme: Boolean)
