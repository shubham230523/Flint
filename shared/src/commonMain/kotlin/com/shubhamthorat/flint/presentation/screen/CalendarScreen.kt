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
import com.shubhamthorat.flint.presentation.component.FlintButton
import com.shubhamthorat.flint.presentation.component.FlintButtonVariant
import com.shubhamthorat.flint.presentation.component.FlintCard
import com.shubhamthorat.flint.presentation.component.FlintChip
import com.shubhamthorat.flint.presentation.navigation.FlintScreen
import com.shubhamthorat.flint.presentation.navigation.NavigationManager
import com.shubhamthorat.flint.presentation.theme.FlintTheme

data class ScheduledPost(
    val id: String,
    val title: String,
    val platform: String,
    val dateText: String,
    val timeText: String,
    val status: String = "SCHEDULED"
)

@Composable
fun CalendarScreen(
    navigationManager: NavigationManager,
    modifier: Modifier = Modifier
) {
    var selectedViewMode by remember { mutableStateOf("Month View") }

    val scheduledPosts = remember {
        listOf(
            ScheduledPost("1", "KMP 2.0 Production Guide", "LinkedIn", "Tomorrow", "10:00 AM"),
            ScheduledPost("2", "AI Task Router Failover Thread", "X (Twitter)", "Thu, Oct 1", "02:30 PM"),
            ScheduledPost("3", "Flint Weekly Substack Issue #43", "Substack", "Sat, Oct 3", "09:00 AM")
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
            text = "Content Calendar",
            style = FlintTheme.typography.displayMedium,
            color = FlintTheme.colors.primary,
            fontWeight = FontWeight.Bold
        )

        // View Mode Switcher
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(FlintTheme.spacing.small)) {
                listOf("Month View", "Week View", "List View").forEach { mode ->
                    FlintChip(
                        selected = selectedViewMode == mode,
                        onClick = { selectedViewMode = mode },
                        label = mode
                    )
                }
            }

            FlintButton(
                onClick = { navigationManager.navigateTo(FlintScreen.Create) },
                text = "+ Schedule Post",
                variant = FlintButtonVariant.PRIMARY
            )
        }

        // Calendar Overview Card
        FlintCard(
            modifier = Modifier.fillMaxWidth(),
            outlined = true
        ) {
            Column(
                modifier = Modifier.padding(FlintTheme.spacing.medium),
                verticalArrangement = Arrangement.spacedBy(FlintTheme.spacing.small)
            ) {
                Text(
                    text = "October 2026 Schedule",
                    style = FlintTheme.typography.titleLarge,
                    color = FlintTheme.colors.primary
                )
                Text(
                    text = "3 posts scheduled across LinkedIn, X Thread, and Substack this week.",
                    style = FlintTheme.typography.bodyMedium,
                    color = FlintTheme.colors.textSecondary
                )
            }
        }

        // Scheduled Posts Section
        Text(
            text = "Upcoming Publications",
            style = FlintTheme.typography.headlineMedium,
            color = FlintTheme.colors.onSurface
        )

        scheduledPosts.forEach { post ->
            FlintCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(FlintTheme.spacing.medium),
                    verticalArrangement = Arrangement.spacedBy(FlintTheme.spacing.extraSmall)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = post.title,
                            style = FlintTheme.typography.titleLarge,
                            color = FlintTheme.colors.onSurface
                        )
                        FlintChip(
                            selected = true,
                            onClick = {},
                            label = post.status
                        )
                    }

                    Text(
                        text = "📅 ${post.dateText} at ${post.timeText} • ${post.platform}",
                        style = FlintTheme.typography.bodyMedium,
                        color = FlintTheme.colors.textSecondary
                    )
                }
            }
        }
    }
}
