package com.shubhamthorat.flint.presentation.screen

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.shubhamthorat.flint.domain.model.Campaign
import com.shubhamthorat.flint.domain.repository.CampaignRepository
import com.shubhamthorat.flint.presentation.component.FlintButton
import com.shubhamthorat.flint.presentation.component.FlintButtonVariant
import com.shubhamthorat.flint.presentation.component.FlintCard
import com.shubhamthorat.flint.presentation.component.FlintChip
import com.shubhamthorat.flint.presentation.navigation.FlintScreen
import com.shubhamthorat.flint.presentation.navigation.NavigationManager
import com.shubhamthorat.flint.presentation.theme.FlintTheme

@Composable
fun CampaignsScreen(
    navigationManager: NavigationManager,
    campaignRepository: CampaignRepository,
    modifier: Modifier = Modifier
) {
    var selectedCampaign by remember { mutableStateOf<Campaign?>(null) }
    val campaigns by campaignRepository.observeCampaigns().collectAsState(initial = emptyList())

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(FlintTheme.spacing.medium),
        verticalArrangement = Arrangement.spacedBy(FlintTheme.spacing.large)
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

        if (campaigns.isEmpty()) {
            FlintCard(
                modifier = Modifier.fillMaxWidth(),
                outlined = true
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = FlintTheme.spacing.extraLarge, horizontal = FlintTheme.spacing.large),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(FlintTheme.spacing.medium)
                ) {
                    Text(
                        text = "Turn one idea into a whole campaign.",
                        style = FlintTheme.typography.titleLarge,
                        color = FlintTheme.colors.primary,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "No campaigns generated yet. Enter a source idea in Spark Workspace to generate LinkedIn posts, X threads, and newsletters simultaneously.",
                        style = FlintTheme.typography.bodyMedium,
                        color = FlintTheme.colors.textSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    FlintButton(
                        onClick = { navigationManager.navigateTo(FlintScreen.Create) },
                        text = "✨ Create Campaign Spark",
                        variant = FlintButtonVariant.PRIMARY
                    )
                }
            }
        } else {
            campaigns.forEach { campaign ->
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
                                label = "${campaign.items.size} Channels"
                            )
                        }

                        Text(
                            text = "Source Spark: \"${campaign.ideaOrSource}\"",
                            style = FlintTheme.typography.bodyMedium,
                            color = FlintTheme.colors.textSecondary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(FlintTheme.spacing.small),
                            verticalAlignment = Alignment.CenterVertically
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
        }

        selectedCampaign?.let { campaign ->
            FlintCard(
                modifier = Modifier.fillMaxWidth(),
                outlined = true
            ) {
                Column(
                    modifier = Modifier.padding(FlintTheme.spacing.medium),
                    verticalArrangement = Arrangement.spacedBy(FlintTheme.spacing.medium)
                ) {
                    Text(
                        text = "Campaign Breakdown: ${campaign.title}",
                        style = FlintTheme.typography.titleLarge,
                        color = FlintTheme.colors.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    campaign.items.forEach { asset ->
                        FlintCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(FlintTheme.spacing.medium),
                                verticalArrangement = Arrangement.spacedBy(FlintTheme.spacing.small)
                            ) {
                                Text(
                                    text = "${asset.platform} — ${asset.title}",
                                    style = FlintTheme.typography.titleMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = asset.body,
                                    style = FlintTheme.typography.bodyMedium,
                                    color = FlintTheme.colors.textSecondary
                                )
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
