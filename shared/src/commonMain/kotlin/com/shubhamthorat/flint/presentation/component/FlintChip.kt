package com.shubhamthorat.flint.presentation.component

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.shubhamthorat.flint.presentation.theme.FlintTheme

@Composable
fun FlintChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    leadingIcon: (@Composable () -> Unit)? = null
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text = label, style = FlintTheme.typography.labelLarge) },
        modifier = modifier,
        leadingIcon = leadingIcon,
        shape = RoundedCornerShape(FlintTheme.radius.round),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = FlintTheme.colors.primary,
            selectedLabelColor = FlintTheme.colors.surface,
            containerColor = FlintTheme.colors.surfaceVariant,
            labelColor = FlintTheme.colors.onSurface
        )
    )
}
