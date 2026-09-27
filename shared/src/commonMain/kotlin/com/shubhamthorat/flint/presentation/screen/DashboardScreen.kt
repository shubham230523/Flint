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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.shubhamthorat.flint.domain.monetization.FreePlanLimits
import com.shubhamthorat.flint.domain.monetization.QuotaCalculator
import com.shubhamthorat.flint.domain.monetization.UserUsage
import com.shubhamthorat.flint.domain.repository.CampaignRepository
import com.shubhamthorat.flint.domain.repository.ContentRepository
import com.shubhamthorat.flint.presentation.component.FlintButton
import com.shubhamthorat.flint.presentation.component.FlintButtonVariant
import com.shubhamthorat.flint.presentation.component.FlintCard
import com.shubhamthorat.flint.presentation.component.FlintChip
import com.shubhamthorat.flint.presentation.component.FlintLinearProgressIndicator
import com.shubhamthorat.flint.presentation.navigation.FlintScreen
import com.shubhamthorat.flint.presentation.navigation.NavigationManager
import com.shubhamthorat.flint.presentation.theme.FlintTheme

@Composable
fun DashboardScreen(
    navigationManager: NavigationManager,
    campaignRepository: CampaignRepository,
    contentRepository: ContentRepository,
    modifier: Modifier = Modifier
) {
    val campaigns by campaignRepository.observeCampaigns().collectAsState(initial = emptyList())
    val contentAssets by contentRepository.observeContentAssets().collectAsState(initial = emptyList())

    val usage = remember(campaigns.size, contentAssets.size) {
        UserUsage(
            aiGenerationsCount = contentAssets.size,
            projectsCount = campaigns.size,
            campaignsCount = campaigns.size
        )
    }
    val quotaCalculator = remember(usage) { QuotaCalculator(usage = usage, isMember = false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(FlintTheme.spacing.medium),
        verticalArrangement = Arrangement.spacedBy(FlintTheme.spacing.medium)
    ) {
        // Hero Section
        FlintCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(FlintTheme.spacing.large),
                verticalArrangement = Arrangement.spacedBy(FlintTheme.spacing.small)
            ) {
                Text(
                    text = "What's your next spark?",
                    style = FlintTheme.typography.displayMedium,
                    color = FlintTheme.colors.primary,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Transform raw ideas, documents, audio, or videos into high-impact multi-channel campaigns.",
                    style = FlintTheme.typography.bodyLarge,
                    color = FlintTheme.colors.textSecondary
                )
                Spacer(modifier = Modifier.height(FlintTheme.spacing.small))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(FlintTheme.spacing.small)
                ) {
                    FlintButton(
                        onClick = { navigationManager.navigateTo(FlintScreen.Create) },
                        text = "✨ New Spark",
                        variant = FlintButtonVariant.PRIMARY,
                        modifier = Modifier.weight(1f)
                    )
                    FlintButton(
                        onClick = { navigationManager.navigateTo(FlintScreen.CreatorDNA) },
                        text = "🧬 Brand DNA",
                        variant = FlintButtonVariant.SECONDARY,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Quota & Usage Card
        FlintCard(
            modifier = Modifier.fillMaxWidth(),
            outlined = true
        ) {
            Column(
                modifier = Modifier.padding(FlintTheme.spacing.medium),
                verticalArrangement = Arrangement.spacedBy(FlintTheme.spacing.small)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Monthly AI Quota",
                        style = FlintTheme.typography.titleMedium,
                        color = FlintTheme.colors.onSurface,
                        modifier = Modifier.weight(1f, fill = false),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    FlintChip(
                        selected = false,
                        onClick = { navigationManager.navigateTo(FlintScreen.Membership) },
                        label = "Free Plan"
                    )
                }

                Text(
                    text = "${quotaCalculator.remainingAiGenerations()} of ${FreePlanLimits.MAX_AI_GENERATIONS} generations remaining",
                    style = FlintTheme.typography.bodyMedium,
                    color = FlintTheme.colors.textSecondary
                )

                FlintLinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    progress = quotaCalculator.aiQuotaUsedPercentage() / 100f
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    FlintButton(
                        onClick = { navigationManager.navigateTo(FlintScreen.Membership) },
                        text = "Upgrade Quota",
                        variant = FlintButtonVariant.TEXT
                    )
                }
            }
        }

        // Recent Sparks & Campaigns
        Text(
            text = "Recent Sparks & Campaigns",
            style = FlintTheme.typography.headlineMedium,
            color = FlintTheme.colors.onSurface
        )

        if (campaigns.isEmpty() && contentAssets.isEmpty()) {
            FlintCard(
                modifier = Modifier.fillMaxWidth(),
                outlined = true
            ) {
                Column(
                    modifier = Modifier.padding(FlintTheme.spacing.large),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(FlintTheme.spacing.small)
                ) {
                    Text(
                        text = "Every story starts somewhere.",
                        style = FlintTheme.typography.titleLarge,
                        color = FlintTheme.colors.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Your dashboard is ready. Click '✨ New Spark' to generate your first multi-channel campaign.",
                        style = FlintTheme.typography.bodyMedium,
                        color = FlintTheme.colors.textSecondary
                    )
                    FlintButton(
                        onClick = { navigationManager.navigateTo(FlintScreen.Create) },
                        text = "Create First Spark",
                        variant = FlintButtonVariant.PRIMARY
                    )
                }
            }
        } else {
            campaigns.forEach { campaign ->
                FlintCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { navigationManager.navigateTo(FlintScreen.Campaigns) }
                ) {
                    Column(
                        modifier = Modifier.padding(FlintTheme.spacing.medium),
                        verticalArrangement = Arrangement.spacedBy(FlintTheme.spacing.extraSmall)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = campaign.title,
                                style = FlintTheme.typography.titleLarge,
                                color = FlintTheme.colors.onSurface,
                                modifier = Modifier.weight(1f, fill = false),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            FlintChip(
                                selected = true,
                                onClick = {},
                                label = "${campaign.items.size} Assets"
                            )
                        }
                        Text(
                            text = "Source Idea: \"${campaign.ideaOrSource}\"",
                            style = FlintTheme.typography.bodyMedium,
                            color = FlintTheme.colors.textSecondary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
