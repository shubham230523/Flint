package com.shubhamthorat.flint.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shubhamthorat.flint.presentation.theme.FlintTheme

@Composable
fun FlintAlertDialog(
    onDismissRequest: () -> Unit,
    title: String,
    text: String,
    isLoading: Boolean = false,
    loadingMessage: String = "Flint AI is generating...",
    confirmButtonText: String? = null,
    onConfirm: (() -> Unit)? = null,
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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                if (isLoading) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = FlintTheme.spacing.large),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(FlintTheme.spacing.medium)
                    ) {
                        FlintCircularProgressIndicator()
                        Text(
                            text = loadingMessage,
                            style = FlintTheme.typography.bodyLarge,
                            color = FlintTheme.colors.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    Text(
                        text = text,
                        style = FlintTheme.typography.bodyLarge,
                        color = FlintTheme.colors.onSurface
                    )
                }
            }
        },
        confirmButton = {
            if (confirmButtonText != null && onConfirm != null) {
                FlintButton(
                    onClick = onConfirm,
                    variant = FlintButtonVariant.PRIMARY,
                    text = confirmButtonText
                )
            }
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
