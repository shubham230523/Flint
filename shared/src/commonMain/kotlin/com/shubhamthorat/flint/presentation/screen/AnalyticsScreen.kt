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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.shubhamthorat.flint.domain.repository.ContentRepository
import com.shubhamthorat.flint.domain.repository.ContentStatus
import com.shubhamthorat.flint.presentation.component.FlintButton
import com.shubhamthorat.flint.presentation.component.FlintButtonVariant
import com.shubhamthorat.flint.presentation.component.FlintCard
import com.shubhamthorat.flint.presentation.component.FlintChip
import com.shubhamthorat.flint.presentation.navigation.FlintScreen
import com.shubhamthorat.flint.presentation.navigation.NavigationManager
import com.shubhamthorat.flint.presentation.theme.FlintTheme

@Composable
fun AnalyticsScreen(
    navigationManager: NavigationManager,
    contentRepository: ContentRepository,
    modifier: Modifier = Modifier
) {
    var selectedTimeRange by remember { mutableStateOf("30 Days") }
    val assets by contentRepository.observeContentAssets().collectAsState(initial = emptyList())
    val publishedAssets = remember(assets) { assets.filter { it.status == ContentStatus.PUBLISHED } }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(FlintTheme.spacing.medium),
        verticalArrangement = Arrangement.spacedBy(FlintTheme.spacing.medium)
    ) {
        Text(
            text = "Analytics & Learning Engine",
            style = FlintTheme.typography.displayMedium,
            color = FlintTheme.colors.primary,
            fontWeight = FontWeight.Bold
        )

        // Time Range Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(FlintTheme.spacing.small)
        ) {
            listOf("7 Days", "30 Days", "90 Days", "All Time").forEach { range ->
                FlintChip(
                    selected = selectedTimeRange == range,
                    onClick = { selectedTimeRange = range },
                    label = range
                )
            }
        }

        if (assets.isEmpty()) {
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
                        text = "Publish something. We'll start learning.",
                        style = FlintTheme.typography.titleLarge,
                        color = FlintTheme.colors.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "No analytics history recorded yet. Publish your generated content to track impressions, engagement, and AI performance learning.",
                        style = FlintTheme.typography.bodyMedium,
                        color = FlintTheme.colors.textSecondary
                    )
                    FlintButton(
                        onClick = { navigationManager.navigateTo(FlintScreen.Create) },
                        text = "✨ Create & Publish Spark",
                        variant = FlintButtonVariant.PRIMARY
                    )
                }
            }
        } else {
            // Metrics Overview Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(FlintTheme.spacing.small)
            ) {
                FlintCard(modifier = Modifier.weight(1f)) {
                    Column(
                        modifier = Modifier.padding(FlintTheme.spacing.medium),
                        verticalArrangement = Arrangement.spacedBy(FlintTheme.spacing.extraSmall)
                    ) {
                        Text(text = "Total Assets Generated", style = FlintTheme.typography.labelSmall, color = FlintTheme.colors.textSecondary)
                        Text(text = "${assets.size}", style = FlintTheme.typography.displayMedium, color = FlintTheme.colors.primary, fontWeight = FontWeight.Bold)
                        Text(text = "Active in library", style = FlintTheme.typography.labelSmall, color = FlintTheme.colors.primary)
                    }
                }

                FlintCard(modifier = Modifier.weight(1f)) {
                    Column(
                        modifier = Modifier.padding(FlintTheme.spacing.medium),
                        verticalArrangement = Arrangement.spacedBy(FlintTheme.spacing.extraSmall)
                    ) {
                        Text(text = "Published Posts", style = FlintTheme.typography.labelSmall, color = FlintTheme.colors.textSecondary)
                        Text(text = "${publishedAssets.size}", style = FlintTheme.typography.displayMedium, color = FlintTheme.colors.secondary, fontWeight = FontWeight.Bold)
                        Text(text = "Live across channels", style = FlintTheme.typography.labelSmall, color = FlintTheme.colors.secondary)
                    }
                }
            }

            // AI Learning Insights
            FlintCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(FlintTheme.spacing.medium),
                    verticalArrangement = Arrangement.spacedBy(FlintTheme.spacing.small)
                ) {
                    Text(
                        text = "🤖 AI Performance Learning Insights",
                        style = FlintTheme.typography.titleLarge,
                        color = FlintTheme.colors.primary
                    )
                    Text(
                        text = "• ${assets.size} content assets generated using active Creator DNA brand voice.\n" +
                                "• Primary selected AI Provider is actively learning hook engagement patterns.\n" +
                                "• Creator DNA tone parameters automatically tuned for optimal audience resonance.",
                        style = FlintTheme.typography.bodyMedium,
                        color = FlintTheme.colors.onSurface
                    )
                }
            }
        }
    }
}
