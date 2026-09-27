package com.shubhamthorat.flint.presentation.component

import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.shubhamthorat.flint.presentation.theme.FlintTheme

@Composable
fun FlintCircularProgressIndicator(
    modifier: Modifier = Modifier,
    progress: Float? = null
) {
    if (progress != null) {
        CircularProgressIndicator(
            progress = { progress },
            modifier = modifier,
            color = FlintTheme.colors.primary,
            trackColor = FlintTheme.colors.surfaceVariant
        )
    } else {
        CircularProgressIndicator(
            modifier = modifier,
            color = FlintTheme.colors.primary,
            trackColor = FlintTheme.colors.surfaceVariant
        )
    }
}

@Composable
fun FlintLinearProgressIndicator(
    modifier: Modifier = Modifier,
    progress: Float? = null
) {
    if (progress != null) {
        LinearProgressIndicator(
            progress = { progress },
            modifier = modifier,
            color = FlintTheme.colors.primary,
            trackColor = FlintTheme.colors.surfaceVariant
        )
    } else {
        LinearProgressIndicator(
            modifier = modifier,
            color = FlintTheme.colors.primary,
            trackColor = FlintTheme.colors.surfaceVariant
        )
    }
}
