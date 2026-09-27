package com.shubhamthorat.flint.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.shubhamthorat.flint.domain.model.CreatorDNA
import com.shubhamthorat.flint.presentation.component.FlintButton
import com.shubhamthorat.flint.presentation.component.FlintButtonVariant
import com.shubhamthorat.flint.presentation.component.FlintCard
import com.shubhamthorat.flint.presentation.component.FlintChip
import com.shubhamthorat.flint.presentation.component.FlintTextField
import com.shubhamthorat.flint.presentation.theme.FlintTheme

@Composable
fun CreatorDnaScreen(
    modifier: Modifier = Modifier
) {
    var dna by remember { mutableStateOf(CreatorDNA()) }
    var isSavedMessageVisible by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(FlintTheme.spacing.medium),
        verticalArrangement = Arrangement.spacedBy(FlintTheme.spacing.medium)
    ) {
        Text(
            text = "Creator DNA Studio",
            style = FlintTheme.typography.displayMedium,
            color = FlintTheme.colors.primary,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Teach Flint your unique brand voice, audience, niche, and content preferences so every generated post sounds authentically like you.",
            style = FlintTheme.typography.bodyLarge,
            color = FlintTheme.colors.textSecondary
        )

        // Live DNA Summary Card
        FlintCard(
            modifier = Modifier.fillMaxWidth(),
            outlined = true
        ) {
            Column(
                modifier = Modifier.padding(FlintTheme.spacing.medium),
                verticalArrangement = Arrangement.spacedBy(FlintTheme.spacing.small)
            ) {
                Text(
                    text = "🧬 Active DNA Profile",
                    style = FlintTheme.typography.titleLarge,
                    color = FlintTheme.colors.primary
                )
                Text(
                    text = "Tone: ${dna.preferredTone} | Style: ${dna.writingStyle} | Audience: ${dna.targetAudience}",
                    style = FlintTheme.typography.bodyMedium,
                    color = FlintTheme.colors.onSurface
                )
                Text(
                    text = "Niche: ${dna.niche} | CTA: ${dna.ctaStyle} | Humor Level: ${dna.humorLevel}/5",
                    style = FlintTheme.typography.bodyMedium,
                    color = FlintTheme.colors.textSecondary
                )
            }
        }

        // Form Fields
        FlintCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(FlintTheme.spacing.medium),
                verticalArrangement = Arrangement.spacedBy(FlintTheme.spacing.medium)
            ) {
                Text(text = "Brand Voice Controls", style = FlintTheme.typography.titleLarge)

                FlintTextField(
                    value = dna.preferredTone,
                    onValueChange = { dna = dna.copy(preferredTone = it) },
                    label = "Preferred Tone",
                    placeholder = "Conversational, Authoritative, Witty..."
                )

                FlintTextField(
                    value = dna.writingStyle,
                    onValueChange = { dna = dna.copy(writingStyle = it) },
                    label = "Writing Style",
                    placeholder = "Story-driven, Data-backed, Punchy..."
                )

                FlintTextField(
                    value = dna.targetAudience,
                    onValueChange = { dna = dna.copy(targetAudience = it) },
                    label = "Target Audience",
                    placeholder = "Software Engineers, Tech Creators..."
                )

                FlintTextField(
                    value = dna.niche,
                    onValueChange = { dna = dna.copy(niche = it) },
                    label = "Content Niche",
                    placeholder = "Kotlin Multiplatform, AI Operating Systems..."
                )

                Text(text = "Humor Level", style = FlintTheme.typography.titleMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(FlintTheme.spacing.small)
                ) {
                    (1..5).forEach { level ->
                        FlintChip(
                            selected = dna.humorLevel == level,
                            onClick = { dna = dna.copy(humorLevel = level) },
                            label = "Level $level"
                        )
                    }
                }
            }
        }

        FlintButton(
            onClick = { isSavedMessageVisible = true },
            text = "Save Creator DNA",
            variant = FlintButtonVariant.PRIMARY,
            modifier = Modifier.fillMaxWidth()
        )

        if (isSavedMessageVisible) {
            FlintCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "✓ Creator DNA successfully saved! Future AI generations will apply these voice tokens.",
                    style = FlintTheme.typography.bodyMedium,
                    color = FlintTheme.colors.primary,
                    modifier = Modifier.padding(FlintTheme.spacing.medium)
                )
            }
        }
    }
}
