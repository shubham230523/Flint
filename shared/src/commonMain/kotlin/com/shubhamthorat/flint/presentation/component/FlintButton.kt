package com.shubhamthorat.flint.presentation.component

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.shubhamthorat.flint.presentation.theme.FlintTheme

enum class FlintButtonVariant {
    PRIMARY, SECONDARY, OUTLINED, TEXT
}

@Composable
fun FlintButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: FlintButtonVariant = FlintButtonVariant.PRIMARY,
    enabled: Boolean = true,
    text: String? = null,
    content: (@Composable RowScope.() -> Unit)? = null
) {
    val shape = RoundedCornerShape(FlintTheme.radius.medium)

    when (variant) {
        FlintButtonVariant.PRIMARY -> {
            Button(
                onClick = onClick,
                modifier = modifier.height(48.dp),
                enabled = enabled,
                shape = shape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = FlintTheme.colors.primary,
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(horizontal = FlintTheme.spacing.medium)
            ) {
                if (text != null) {
                    Text(text = text, style = FlintTheme.typography.labelLarge)
                } else {
                    content?.invoke(this)
                }
            }
        }
        FlintButtonVariant.SECONDARY -> {
            Button(
                onClick = onClick,
                modifier = modifier.height(48.dp),
                enabled = enabled,
                shape = shape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = FlintTheme.colors.secondary,
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(horizontal = FlintTheme.spacing.medium)
            ) {
                if (text != null) {
                    Text(text = text, style = FlintTheme.typography.labelLarge)
                } else {
                    content?.invoke(this)
                }
            }
        }
        FlintButtonVariant.OUTLINED -> {
            OutlinedButton(
                onClick = onClick,
                modifier = modifier.height(48.dp),
                enabled = enabled,
                shape = shape,
                contentPadding = PaddingValues(horizontal = FlintTheme.spacing.medium)
            ) {
                if (text != null) {
                    Text(
                        text = text,
                        style = FlintTheme.typography.labelLarge,
                        color = FlintTheme.colors.primary
                    )
                } else {
                    content?.invoke(this)
                }
            }
        }
        FlintButtonVariant.TEXT -> {
            TextButton(
                onClick = onClick,
                modifier = modifier.height(48.dp),
                enabled = enabled,
                shape = shape,
                contentPadding = PaddingValues(horizontal = FlintTheme.spacing.medium)
            ) {
                if (text != null) {
                    Text(
                        text = text,
                        style = FlintTheme.typography.labelLarge,
                        color = FlintTheme.colors.primary
                    )
                } else {
                    content?.invoke(this)
                }
            }
        }
    }
}
