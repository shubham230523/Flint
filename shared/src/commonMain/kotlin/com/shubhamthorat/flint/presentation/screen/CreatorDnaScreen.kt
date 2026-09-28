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
import androidx.compose.runtime.LaunchedEffect
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
import com.shubhamthorat.flint.domain.model.FlintResult
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
    val activeProfileState = creatorDnaRepository.observeProfile().collectAsState(initial = null)
    val activeProfile = activeProfileState.value

    var dnaState by remember { mutableStateOf(CreatorDNA()) }
    var hasInitialPreFillDone by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSavedMessageVisible by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(activeProfile) {
        if (!hasInitialPreFillDone && activeProfile?.dna != null) {
            val saved = activeProfile.dna
            if (saved.preferredTone.isNotBlank() || saved.writingStyle.isNotBlank() || saved.targetAudience.isNotBlank() || saved.niche.isNotBlank()) {
                dnaState = saved
                hasInitialPreFillDone = true
            }
        }
    }

    val savedDna = activeProfile?.dna
    val hasSavedDna = savedDna != null && (
        savedDna.preferredTone.isNotBlank() ||
        savedDna.writingStyle.isNotBlank() ||
        savedDna.targetAudience.isNotBlank() ||
        savedDna.niche.isNotBlank()
    )

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
                if (hasSavedDna) {
                    Text(
                        text = "🧬 Active DNA Profile (${activeProfile.handle})",
                        style = FlintTheme.typography.titleLarge,
                        color = FlintTheme.colors.primary
                    )
                    Text(
                        text = "Tone: ${savedDna.preferredTone.ifBlank { "Not set" }} | Style: ${savedDna.writingStyle.ifBlank { "Not set" }} | Audience: ${savedDna.targetAudience.ifBlank { "Not set" }}",
                        style = FlintTheme.typography.bodyMedium,
                        color = FlintTheme.colors.onSurface
                    )
                    Text(
                        text = "Niche: ${savedDna.niche.ifBlank { "Not set" }} | CTA: ${savedDna.ctaStyle.ifBlank { "Not set" }} | Humor Level: ${savedDna.humorLevel}/5",
                        style = FlintTheme.typography.bodyMedium,
                        color = FlintTheme.colors.textSecondary
                    )
                } else {
                    Text(
                        text = "🧬 No Active DNA Profile Saved",
                        style = FlintTheme.typography.titleLarge,
                        color = FlintTheme.colors.primary
                    )
                    Text(
                        text = "Fill in your brand voice controls below and click 'Save Creator DNA' to teach Flint AI your personal tone and style.",
                        style = FlintTheme.typography.bodyMedium,
                        color = FlintTheme.colors.textSecondary
                    )
                }
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
                    placeholder = "e.g., Conversational, Authoritative, Punchy, Witty..."
                )

                FlintTextField(
                    value = dnaState.writingStyle,
                    onValueChange = { dnaState = dnaState.copy(writingStyle = it) },
                    label = "Writing Style",
                    placeholder = "e.g., Story-driven, Data-backed, Direct & Minimalist..."
                )

                FlintTextField(
                    value = dnaState.targetAudience,
                    onValueChange = { dnaState = dnaState.copy(targetAudience = it) },
                    label = "Target Audience",
                    placeholder = "e.g., Software Engineers, Founders, Marketers..."
                )

                FlintTextField(
                    value = dnaState.niche,
                    onValueChange = { dnaState = dnaState.copy(niche = it) },
                    label = "Content Niche",
                    placeholder = "e.g., Kotlin Multiplatform, AI Tools, SaaS..."
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
                    errorMessage = null
                    isSavedMessageVisible = false
                    val currentProfile = activeProfile ?: CreatorProfile("user_default", "@creator")
                    val updated = currentProfile.copy(dna = dnaState)
                    val result = creatorDnaRepository.updateProfile(updated)
                    if (result is FlintResult.Success) {
                        isSavedMessageVisible = true
                        dnaState = CreatorDNA() // Form gets empty again upon submitting!
                    } else if (result is FlintResult.Error) {
                        errorMessage = result.error.message
                    }
                }
            },
            text = "Save Creator DNA",
            variant = FlintButtonVariant.PRIMARY,
            modifier = Modifier.fillMaxWidth()
        )

        errorMessage?.let { err ->
            FlintCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = err,
                    style = FlintTheme.typography.bodyMedium,
                    color = com.shubhamthorat.flint.presentation.theme.FlintColorTokens.Error,
                    modifier = Modifier.padding(FlintTheme.spacing.medium)
                )
            }
        }

        if (isSavedMessageVisible) {
            FlintCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "✓ Creator DNA successfully saved to Cloud Firestore! Future AI generations will apply these voice tokens.",
                    style = FlintTheme.typography.bodyMedium,
                    color = FlintTheme.colors.primary,
                    modifier = Modifier.padding(FlintTheme.spacing.medium)
                )
            }
        }
    }
}
