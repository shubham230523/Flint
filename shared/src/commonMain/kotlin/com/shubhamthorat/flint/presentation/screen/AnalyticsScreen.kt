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
import com.shubhamthorat.flint.presentation.component.FlintCard
import com.shubhamthorat.flint.presentation.component.FlintChip
import com.shubhamthorat.flint.presentation.theme.FlintTheme

@Composable
fun AnalyticsScreen(
    modifier: Modifier = Modifier
) {
    var selectedTimeRange by remember { mutableStateOf("30 Days") }

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
                    Text(text = "Total Impressions", style = FlintTheme.typography.labelSmall, color = FlintTheme.colors.textSecondary)
                    Text(text = "128,450", style = FlintTheme.typography.displayMedium, color = FlintTheme.colors.primary, fontWeight = FontWeight.Bold)
                    Text(text = "↑ +24% vs last period", style = FlintTheme.typography.labelSmall, color = FlintTheme.colors.primary)
                }
            }

            FlintCard(modifier = Modifier.weight(1f)) {
                Column(
                    modifier = Modifier.padding(FlintTheme.spacing.medium),
                    verticalArrangement = Arrangement.spacedBy(FlintTheme.spacing.extraSmall)
                ) {
                    Text(text = "Avg. Engagement", style = FlintTheme.typography.labelSmall, color = FlintTheme.colors.textSecondary)
                    Text(text = "6.8%", style = FlintTheme.typography.displayMedium, color = FlintTheme.colors.secondary, fontWeight = FontWeight.Bold)
                    Text(text = "↑ +1.2% higher", style = FlintTheme.typography.labelSmall, color = FlintTheme.colors.secondary)
                }
            }
        }

        // Platform Breakdown
        Text(
            text = "Channel Performance Breakdown",
            style = FlintTheme.typography.headlineMedium,
            color = FlintTheme.colors.onSurface
        )

        val channels = listOf(
            Triple("LinkedIn", "54,200 Impressions", "8.2% Engagement"),
            Triple("X (Twitter)", "48,100 Impressions", "5.4% Engagement"),
            Triple("Substack Newsletter", "26,150 Opens", "42.5% Open Rate")
        )

        channels.forEach { (platform, views, engagement) ->
            FlintCard(
                modifier = Modifier.fillMaxWidth(),
                outlined = true
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(FlintTheme.spacing.medium),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(text = platform, style = FlintTheme.typography.titleLarge, color = FlintTheme.colors.onSurface)
                        Text(text = views, style = FlintTheme.typography.bodyMedium, color = FlintTheme.colors.textSecondary)
                    }
                    FlintChip(selected = true, onClick = {}, label = engagement)
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
                    text = "🤖 AI Performance Insights",
                    style = FlintTheme.typography.titleLarge,
                    color = FlintTheme.colors.primary
                )
                Text(
                    text = "• Posts starting with a 'Question Hook' received 38% more comments on LinkedIn.\n" +
                            "• 'Conversational' tone generated 2.4x more newsletter clicks than 'Authoritative'.\n" +
                            "• Creator DNA tone tokens have been automatically adjusted to favor high-engagement hooks.",
                    style = FlintTheme.typography.bodyMedium,
                    color = FlintTheme.colors.onSurface
                )
            }
        }
    }
}
