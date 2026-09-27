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
import com.shubhamthorat.flint.domain.repository.ContentAsset
import com.shubhamthorat.flint.domain.repository.ContentStatus
import com.shubhamthorat.flint.domain.repository.ContentType
import com.shubhamthorat.flint.presentation.component.FlintAlertDialog
import com.shubhamthorat.flint.presentation.component.FlintButton
import com.shubhamthorat.flint.presentation.component.FlintButtonVariant
import com.shubhamthorat.flint.presentation.component.FlintCard
import com.shubhamthorat.flint.presentation.component.FlintChip
import com.shubhamthorat.flint.presentation.component.FlintTextField
import com.shubhamthorat.flint.presentation.theme.FlintTheme

@Composable
fun ContentLibraryScreen(
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") }
    var selectedAssetForDetail by remember { mutableStateOf<ContentAsset?>(null) }

    val sampleAssets = remember {
        listOf(
            ContentAsset(
                id = "1",
                sourceId = "s1",
                title = "10 Reasons KMP is Ready for Production",
                body = "Kotlin Multiplatform gives mobile & desktop teams shared code without compromising UI speed. Here are 10 key architectural wins...",
                type = ContentType.LINKEDIN_POST,
                status = ContentStatus.PUBLISHED,
                platform = "LinkedIn"
            ),
            ContentAsset(
                id = "2",
                sourceId = "s1",
                title = "Flint Architecture Breakdown",
                body = "1/ Thread on building a KMP AI Operating System with Clean Architecture, Ktor, and Coroutines Flow. 🧵",
                type = ContentType.X_THREAD,
                status = ContentStatus.DRAFT,
                platform = "X (Twitter)"
            ),
            ContentAsset(
                id = "3",
                sourceId = "s2",
                title = "Weekly Creator Spark #42",
                body = "Welcome to issue #42. Today we cover AI Task Routers and fallback strategies for 99.9% uptime...",
                type = ContentType.NEWSLETTER,
                status = ContentStatus.SCHEDULED,
                platform = "Substack"
            )
        )
    }

    val filteredAssets = remember(searchQuery, selectedFilter) {
        sampleAssets.filter { asset ->
            val matchesQuery = searchQuery.isBlank() || asset.title.contains(searchQuery, ignoreCase = true) || asset.body.contains(searchQuery, ignoreCase = true)
            val matchesFilter = when (selectedFilter) {
                "All" -> true
                "Drafts" -> asset.status == ContentStatus.DRAFT
                "Published" -> asset.status == ContentStatus.PUBLISHED
                else -> asset.platform.contains(selectedFilter, ignoreCase = true)
            }
            matchesQuery && matchesFilter
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(FlintTheme.spacing.medium),
        verticalArrangement = Arrangement.spacedBy(FlintTheme.spacing.medium)
    ) {
        Text(
            text = "Content Library",
            style = FlintTheme.typography.displayMedium,
            color = FlintTheme.colors.primary,
            fontWeight = FontWeight.Bold
        )

        // Search Bar
        FlintTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = "Search generated posts, threads, newsletters...",
            modifier = Modifier.fillMaxWidth()
        )

        // Category Filter Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(FlintTheme.spacing.small)
        ) {
            val filters = listOf("All", "Drafts", "Published", "LinkedIn", "X (Twitter)", "Substack")
            filters.forEach { filter ->
                FlintChip(
                    selected = selectedFilter == filter,
                    onClick = { selectedFilter = filter },
                    label = filter
                )
            }
        }

        // Asset Cards Grid
        filteredAssets.forEach { asset ->
            FlintCard(
                modifier = Modifier.fillMaxWidth(),
                onClick = { selectedAssetForDetail = asset }
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
                            text = asset.title,
                            style = FlintTheme.typography.titleLarge,
                            color = FlintTheme.colors.onSurface
                        )
                        FlintChip(
                            selected = asset.status == ContentStatus.PUBLISHED,
                            onClick = {},
                            label = asset.status.name
                        )
                    }

                    Text(
                        text = asset.body,
                        style = FlintTheme.typography.bodyMedium,
                        color = FlintTheme.colors.textSecondary,
                        maxLines = 2
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        FlintChip(
                            selected = false,
                            onClick = {},
                            label = asset.platform
                        )

                        FlintButton(
                            onClick = { selectedAssetForDetail = asset },
                            text = "View & Edit",
                            variant = FlintButtonVariant.TEXT
                        )
                    }
                }
            }
        }

        // Detail Dialog
        selectedAssetForDetail?.let { asset ->
            FlintAlertDialog(
                onDismissRequest = { selectedAssetForDetail = null },
                title = asset.title,
                text = asset.body,
                confirmButtonText = "Close",
                onConfirm = { selectedAssetForDetail = null }
            )
        }
    }
}
