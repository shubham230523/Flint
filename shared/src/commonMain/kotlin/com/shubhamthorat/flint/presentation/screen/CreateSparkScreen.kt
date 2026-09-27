package com.shubhamthorat.flint.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shubhamthorat.flint.domain.ai.AiTaskRouter
import com.shubhamthorat.flint.domain.ai.FakeAiProvider
import com.shubhamthorat.flint.domain.model.Campaign
import com.shubhamthorat.flint.domain.model.CreatorDNA
import com.shubhamthorat.flint.domain.model.FlintResult
import com.shubhamthorat.flint.domain.repository.ContentType
import com.shubhamthorat.flint.domain.usecase.CreateSparkCampaignUseCase
import com.shubhamthorat.flint.domain.usecase.GenerateContentUseCase
import com.shubhamthorat.flint.presentation.component.FlintButton
import com.shubhamthorat.flint.presentation.component.FlintButtonVariant
import com.shubhamthorat.flint.presentation.component.FlintCard
import com.shubhamthorat.flint.presentation.component.FlintChip
import com.shubhamthorat.flint.presentation.component.FlintCircularProgressIndicator
import com.shubhamthorat.flint.presentation.component.FlintTextField
import com.shubhamthorat.flint.presentation.navigation.FlintScreen
import com.shubhamthorat.flint.presentation.navigation.NavigationManager
import com.shubhamthorat.flint.presentation.theme.FlintTheme
import kotlinx.coroutines.launch

@Composable
fun CreateSparkScreen(
    navigationManager: NavigationManager,
    modifier: Modifier = Modifier
) {
    var sourceText by remember { mutableStateOf("Kotlin Multiplatform allows sharing business logic across Android, iOS, Desktop, and Web while maintaining native UI performance.") }
    var selectedTone by remember { mutableStateOf("Conversational") }
    var isGenerating by remember { mutableStateOf(false) }
    var generatedCampaign by remember { mutableStateOf<Campaign?>(null) }
    var selectedAssetIndex by remember { mutableStateOf(0) }

    val selectedTypes = remember {
        mutableStateListOf(ContentType.LINKEDIN_POST, ContentType.X_THREAD, ContentType.NEWSLETTER)
    }

    val coroutineScope = rememberCoroutineScope()
    val campaignUseCase = remember {
        val router = AiTaskRouter(listOf(FakeAiProvider("Gemini")))
        CreateSparkCampaignUseCase(GenerateContentUseCase(router))
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(FlintTheme.spacing.medium),
        verticalArrangement = Arrangement.spacedBy(FlintTheme.spacing.medium)
    ) {
        Text(
            text = "Create Spark Workspace",
            style = FlintTheme.typography.displayMedium,
            color = FlintTheme.colors.primary,
            fontWeight = FontWeight.Bold
        )

        // Source Input Card
        FlintCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(FlintTheme.spacing.medium),
                verticalArrangement = Arrangement.spacedBy(FlintTheme.spacing.small)
            ) {
                Text(
                    text = "Source Idea / Content",
                    style = FlintTheme.typography.titleLarge,
                    color = FlintTheme.colors.onSurface
                )

                FlintTextField(
                    value = sourceText,
                    onValueChange = { sourceText = it },
                    placeholder = "Paste raw idea, blog URL, video transcript, or document text...",
                    singleLine = false,
                    modifier = Modifier.fillMaxWidth().height(120.dp)
                )
            }
        }

        // Target Channels
        FlintCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(FlintTheme.spacing.medium),
                verticalArrangement = Arrangement.spacedBy(FlintTheme.spacing.small)
            ) {
                Text(
                    text = "Target Platforms",
                    style = FlintTheme.typography.titleMedium,
                    color = FlintTheme.colors.onSurface
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(FlintTheme.spacing.small)
                ) {
                    val channels = listOf(
                        ContentType.LINKEDIN_POST to "LinkedIn",
                        ContentType.X_THREAD to "X Thread",
                        ContentType.NEWSLETTER to "Newsletter",
                        ContentType.YOUTUBE_SCRIPT to "YouTube",
                        ContentType.CAROUSEL to "Carousel"
                    )

                    channels.forEach { (type, label) ->
                        val isSelected = selectedTypes.contains(type)
                        FlintChip(
                            selected = isSelected,
                            onClick = {
                                if (isSelected) selectedTypes.remove(type) else selectedTypes.add(type)
                            },
                            label = label
                        )
                    }
                }
            }
        }

        // Brand Tone Selection
        FlintCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(FlintTheme.spacing.medium),
                verticalArrangement = Arrangement.spacedBy(FlintTheme.spacing.small)
            ) {
                Text(
                    text = "Creator DNA Tone Override",
                    style = FlintTheme.typography.titleMedium,
                    color = FlintTheme.colors.onSurface
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(FlintTheme.spacing.small)
                ) {
                    val tones = listOf("Conversational", "Punchy", "Authoritative", "Storytelling")
                    tones.forEach { tone ->
                        FlintChip(
                            selected = selectedTone == tone,
                            onClick = { selectedTone = tone },
                            label = tone
                        )
                    }
                }
            }
        }

        // Trigger Button
        FlintButton(
            onClick = {
                if (sourceText.isNotBlank() && selectedTypes.isNotEmpty()) {
                    isGenerating = true
                    coroutineScope.launch {
                        val dna = CreatorDNA(preferredTone = selectedTone)
                        val result = campaignUseCase.execute(sourceText, selectedTypes.toList(), dna)
                        if (result is FlintResult.Success) {
                            generatedCampaign = result.data
                        }
                        isGenerating = false
                    }
                }
            },
            text = if (isGenerating) "Generating Spark..." else "✨ Generate Spark Campaign",
            variant = FlintButtonVariant.PRIMARY,
            enabled = !isGenerating && sourceText.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        )

        if (isGenerating) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                FlintCircularProgressIndicator()
            }
        }

        // Generated Output Stage
        generatedCampaign?.let { campaign ->
            Spacer(modifier = Modifier.height(FlintTheme.spacing.small))
            Text(
                text = "Generated Campaign Assets (${campaign.items.size})",
                style = FlintTheme.typography.headlineMedium,
                color = FlintTheme.colors.primary
            )

            // Asset Tabs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(FlintTheme.spacing.small)
            ) {
                campaign.items.forEachIndexed { index, asset ->
                    FlintChip(
                        selected = selectedAssetIndex == index,
                        onClick = { selectedAssetIndex = index },
                        label = asset.platform
                    )
                }
            }

            // Asset Content Card
            if (campaign.items.isNotEmpty()) {
                val currentAsset = campaign.items[selectedAssetIndex.coerceIn(0, campaign.items.size - 1)]
                FlintCard(
                    modifier = Modifier.fillMaxWidth(),
                    outlined = true
                ) {
                    Column(
                        modifier = Modifier.padding(FlintTheme.spacing.medium),
                        verticalArrangement = Arrangement.spacedBy(FlintTheme.spacing.small)
                    ) {
                        Text(
                            text = currentAsset.title,
                            style = FlintTheme.typography.titleLarge,
                            color = FlintTheme.colors.onSurface
                        )
                        Text(
                            text = currentAsset.body,
                            style = FlintTheme.typography.bodyLarge,
                            color = FlintTheme.colors.onSurface
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            FlintButton(
                                onClick = { navigationManager.navigateTo(FlintScreen.ContentLibrary) },
                                text = "Save to Library",
                                variant = FlintButtonVariant.SECONDARY
                            )
                        }
                    }
                }
            }
        }
    }
}
