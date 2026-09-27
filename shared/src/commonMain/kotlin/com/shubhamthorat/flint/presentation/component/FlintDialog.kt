package com.shubhamthorat.flint.presentation.component

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.shubhamthorat.flint.presentation.theme.FlintTheme

@Composable
fun FlintAlertDialog(
    onDismissRequest: () -> Unit,
    title: String,
    text: String,
    confirmButtonText: String,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    dismissButtonText: String? = null,
    onDismiss: (() -> Unit)? = null
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        title = {
            Text(text = title, style = FlintTheme.typography.headlineMedium)
        },
        text = {
            Text(text = text, style = FlintTheme.typography.bodyLarge)
        },
        confirmButton = {
            FlintButton(
                onClick = onConfirm,
                variant = FlintButtonVariant.PRIMARY,
                text = confirmButtonText
            )
        },
        dismissButton = if (dismissButtonText != null && onDismiss != null) {
            {
                FlintButton(
                    onClick = onDismiss,
                    variant = FlintButtonVariant.OUTLINED,
                    text = dismissButtonText
                )
            }
        } else null,
        shape = RoundedCornerShape(FlintTheme.radius.large),
        containerColor = FlintTheme.colors.surface,
        titleContentColor = FlintTheme.colors.onSurface,
        textContentColor = FlintTheme.colors.onSurface
    )
}
