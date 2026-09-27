package com.shubhamthorat.flint.presentation.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedCard
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shubhamthorat.flint.presentation.theme.FlintTheme

@Composable
fun FlintCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    outlined: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(FlintTheme.radius.large)

    if (outlined) {
        if (onClick != null) {
            OutlinedCard(
                onClick = onClick,
                modifier = modifier,
                shape = shape,
                border = BorderStroke(1.dp, FlintTheme.colors.surfaceVariant),
                colors = CardDefaults.outlinedCardColors(
                    containerColor = FlintTheme.colors.surface
                ),
                content = content
            )
        } else {
            OutlinedCard(
                modifier = modifier,
                shape = shape,
                border = BorderStroke(1.dp, FlintTheme.colors.surfaceVariant),
                colors = CardDefaults.outlinedCardColors(
                    containerColor = FlintTheme.colors.surface
                ),
                content = content
            )
        }
    } else {
        if (onClick != null) {
            Card(
                onClick = onClick,
                modifier = modifier,
                shape = shape,
                colors = CardDefaults.cardColors(
                    containerColor = FlintTheme.colors.surface
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = FlintTheme.elevation.low
                ),
                content = content
            )
        } else {
            Card(
                modifier = modifier,
                shape = shape,
                colors = CardDefaults.cardColors(
                    containerColor = FlintTheme.colors.surface
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = FlintTheme.elevation.low
                ),
                content = content
            )
        }
    }
}
