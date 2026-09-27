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
import com.shubhamthorat.flint.domain.model.Campaign
import com.shubhamthorat.flint.domain.repository.ContentAsset
import com.shubhamthorat.flint.domain.repository.ContentStatus
import com.shubhamthorat.flint.domain.repository.ContentType
import com.shubhamthorat.flint.presentation.component.FlintButton
import com.shubhamthorat.flint.presentation.component.FlintButtonVariant
import com.shubhamthorat.flint.presentation.component.FlintCard
import com.shubhamthorat.flint.presentation.component.FlintChip
import com.shubhamthorat.flint.presentation.theme.FlintTheme

@Composable
fun CampaignsScreen(
    modifier: Modifier = Modifier
) {
    var selectedCampaign by remember { mutableStateOf<Campaign?>(null) }

    val sampleCampaigns = remember {
        listOf(
            Campaign(
                id = "c1",
                title = "Campaign: Flint KMP Launch Strategy",
                ideaOrSource = "One spark. Endless stories.",
                items = listOf(
                    ContentAsset("1", "c1", "LinkedIn Post", "Introducing Flint - the AI Content Operating System built on Kotlin Multiplatform...", ContentType.LINKEDIN_POST, ContentStatus.PUBLISHED, "LinkedIn"),
                    ContentAsset("2", "c1", "X Thread", "1/ Today we announce Flint. One spark -> Endless stories across mobile, desktop & web.", ContentType.X_THREAD, ContentStatus.PUBLISHED, "X (Twitter)"),
                    ContentAsset("3", "c1", "Newsletter Issue #1", "Inside Flint's Multiplatform Architecture & AI Task Router...", ContentType.NEWSLETTER, ContentStatus.DRAFT, "Substack")
                )
            ),
            Campaign(
                id = "c2",
                title = "Campaign: AI Content Automation Guide",
                ideaOrSource = "Automating social media without losing creator identity",
                items = listOf(
                    ContentAsset("4", "c2", "LinkedIn Post", "Why AI tools often sound generic and how Creator DNA fixes it...", ContentType.LINKEDIN_POST, ContentStatus.DRAFT, "LinkedIn"),
                    ContentAsset("5", "c2", "Carousel PDF", "5 Steps to train AI on your personal brand tone & vocabulary.", ContentType.CAROUSEL, ContentStatus.DRAFT, "Carousel")
                )
            )
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(FlintTheme.spacing.medium),
        verticalArrangement = Arrangement.spacedBy(FlintTheme.spacing.medium)
    ) {
        Text(
            text = "Campaign Engine",
            style = FlintTheme.typography.displayMedium,
            color = FlintTheme.colors.primary,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "One source spark automatically generated into coordinated multi-channel assets.",
            style = FlintTheme.typography.bodyLarge,
            color = FlintTheme.colors.textSecondary
        )

        sampleCampaigns.forEach { campaign ->
            FlintCard(
                modifier = Modifier.fillMaxWidth(),
                onClick = { selectedCampaign = campaign }
            ) {
                Column(
                    modifier = Modifier.padding(FlintTheme.spacing.medium),
                    verticalArrangement = Arrangement.spacedBy(FlintTheme.spacing.small)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = campaign.title,
                            style = FlintTheme.typography.titleLarge,
                            color = FlintTheme.colors.onSurface
                        )
                        FlintChip(
                            selected = true,
                            onClick = {},
                            label = "${campaign.items.size} Channels"
                        )
                    }

                    Text(
                        text = "Source Spark: \"${campaign.ideaOrSource}\"",
                        style = FlintTheme.typography.bodyMedium,
                        color = FlintTheme.colors.textSecondary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(FlintTheme.spacing.small)
                    ) {
                        campaign.items.forEach { item ->
                            FlintChip(
                                selected = false,
                                onClick = {},
                                label = item.platform
                            )
                        }
                    }
                }
            }
        }

        selectedCampaign?.let { campaign ->
            FlintCard(
                modifier = Modifier.fillMaxWidth(),
                outlined = true
            ) {
                Column(
                    modifier = Modifier.padding(FlintTheme.spacing.medium),
                    verticalArrangement = Arrangement.spacedBy(FlintTheme.spacing.small)
                ) {
                    Text(
                        text = "Campaign Breakdown: ${campaign.title}",
                        style = FlintTheme.typography.titleLarge,
                        color = FlintTheme.colors.primary
                    )

                    campaign.items.forEach { asset ->
                        FlintCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(FlintTheme.spacing.small),
                                verticalArrangement = Arrangement.spacedBy(FlintTheme.spacing.extraSmall)
                            ) {
                                Text(text = "${asset.platform} — ${asset.title}", style = FlintTheme.typography.titleMedium)
                                Text(text = asset.body, style = FlintTheme.typography.bodyMedium, color = FlintTheme.colors.textSecondary)
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        FlintButton(
                            onClick = { selectedCampaign = null },
                            text = "Collapse View",
                            variant = FlintButtonVariant.TEXT
                        )
                    }
                }
            }
        }
    }
}
