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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.shubhamthorat.flint.domain.model.CreatorDNA
import com.shubhamthorat.flint.domain.model.CreatorProfile
import com.shubhamthorat.flint.domain.repository.CreatorDnaRepository
import com.shubhamthorat.flint.presentation.component.FlintButton
import com.shubhamthorat.flint.presentation.component.FlintButtonVariant
import com.shubhamthorat.flint.presentation.component.FlintCard
import com.shubhamthorat.flint.presentation.component.FlintChip
import com.shubhamthorat.flint.presentation.component.FlintTextField
import com.shubhamthorat.flint.presentation.theme.FlintTheme
import kotlinx.coroutines.launch

@Composable
fun CreatorDnaScreen(
    creatorDnaRepository: CreatorDnaRepository,
    modifier: Modifier = Modifier
) {
    val activeProfile by creatorDnaRepository.observeProfile().collectAsState(initial = CreatorProfile("user_default", "@creator"))
    var dnaState by remember(activeProfile) { mutableStateOf(activeProfile?.dna ?: CreatorDNA()) }
    var isSavedMessageVisible by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

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
                    text = "🧬 Active DNA Profile (${activeProfile?.handle ?: "@creator"})",
                    style = FlintTheme.typography.titleLarge,
                    color = FlintTheme.colors.primary
                )
                Text(
                    text = "Tone: ${dnaState.preferredTone} | Style: ${dnaState.writingStyle} | Audience: ${dnaState.targetAudience}",
                    style = FlintTheme.typography.bodyMedium,
                    color = FlintTheme.colors.onSurface
                )
                Text(
                    text = "Niche: ${dnaState.niche} | CTA: ${dnaState.ctaStyle} | Humor Level: ${dnaState.humorLevel}/5",
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
                    value = dnaState.preferredTone,
                    onValueChange = { dnaState = dnaState.copy(preferredTone = it) },
                    label = "Preferred Tone",
                    placeholder = "Conversational, Authoritative, Witty..."
                )

                FlintTextField(
                    value = dnaState.writingStyle,
                    onValueChange = { dnaState = dnaState.copy(writingStyle = it) },
                    label = "Writing Style",
                    placeholder = "Story-driven, Data-backed, Punchy..."
                )

                FlintTextField(
                    value = dnaState.targetAudience,
                    onValueChange = { dnaState = dnaState.copy(targetAudience = it) },
                    label = "Target Audience",
                    placeholder = "Software Engineers, Tech Creators..."
                )

                FlintTextField(
                    value = dnaState.niche,
                    onValueChange = { dnaState = dnaState.copy(niche = it) },
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
                            selected = dnaState.humorLevel == level,
                            onClick = { dnaState = dnaState.copy(humorLevel = level) },
                            label = "Level $level"
                        )
                    }
                }
            }
        }

        FlintButton(
            onClick = {
                coroutineScope.launch {
                    val updated = (activeProfile ?: CreatorProfile("user_default", "@creator")).copy(dna = dnaState)
                    creatorDnaRepository.updateProfile(updated)
                    isSavedMessageVisible = true
                }
            },
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
