package com.shubhamthorat.flint.presentation.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.VisualTransformation
import com.shubhamthorat.flint.presentation.theme.FlintTheme

@Composable
fun FlintTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    errorText: String? = null,
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null
) {
    val shape = RoundedCornerShape(FlintTheme.radius.medium)

    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = if (singleLine) Modifier.fillMaxWidth() else Modifier.fillMaxSize(),
            label = label?.let { { Text(it, style = FlintTheme.typography.bodyMedium) } },
            placeholder = placeholder?.let { { Text(it, style = FlintTheme.typography.bodyMedium) } },
            isError = errorText != null,
            singleLine = singleLine,
            minLines = minLines,
            maxLines = maxLines,
            visualTransformation = visualTransformation,
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            shape = shape,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = FlintTheme.colors.primary,
                unfocusedBorderColor = FlintTheme.colors.surfaceVariant,
                focusedLabelColor = FlintTheme.colors.primary,
                cursorColor = FlintTheme.colors.primary
            )
        )
        if (errorText != null) {
            Text(
                text = errorText,
                style = FlintTheme.typography.labelSmall,
                color = FlintTheme.colors.accent,
                modifier = Modifier.padding(
                    start = FlintTheme.spacing.small,
                    top = FlintTheme.spacing.extraSmall
                )
            )
        }
    }
}
