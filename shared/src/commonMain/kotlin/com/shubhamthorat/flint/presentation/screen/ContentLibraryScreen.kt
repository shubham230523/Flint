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
import androidx.compose.ui.text.style.TextOverflow
import com.shubhamthorat.flint.domain.repository.ContentAsset
import com.shubhamthorat.flint.domain.repository.ContentRepository
import com.shubhamthorat.flint.domain.repository.ContentStatus
import com.shubhamthorat.flint.presentation.component.FlintAlertDialog
import com.shubhamthorat.flint.presentation.component.FlintButton
import com.shubhamthorat.flint.presentation.component.FlintButtonVariant
import com.shubhamthorat.flint.presentation.component.FlintCard
import com.shubhamthorat.flint.presentation.component.FlintChip
import com.shubhamthorat.flint.presentation.component.FlintTextField
import com.shubhamthorat.flint.presentation.navigation.FlintScreen
import com.shubhamthorat.flint.presentation.navigation.NavigationManager
import com.shubhamthorat.flint.presentation.theme.FlintTheme

@Composable
fun ContentLibraryScreen(
    navigationManager: NavigationManager,
    contentRepository: ContentRepository,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") }
    var selectedAssetForDetail by remember { mutableStateOf<ContentAsset?>(null) }

    val assets by contentRepository.observeContentAssets().collectAsState(initial = emptyList())

    val filteredAssets = remember(assets, searchQuery, selectedFilter) {
        assets.filter { asset ->
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
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(FlintTheme.spacing.small),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val filters = listOf("All", "Drafts", "Published", "LinkedIn", "X Thread", "Newsletter")
            filters.forEach { filter ->
                FlintChip(
                    selected = selectedFilter == filter,
                    onClick = { selectedFilter = filter },
                    label = filter
                )
            }
        }

        if (filteredAssets.isEmpty()) {
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
                        text = "Your content library is empty.",
                        style = FlintTheme.typography.titleLarge,
                        color = FlintTheme.colors.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Generate your first campaign in the Spark workspace to populate your library.",
                        style = FlintTheme.typography.bodyMedium,
                        color = FlintTheme.colors.textSecondary
                    )
                    FlintButton(
                        onClick = { navigationManager.navigateTo(FlintScreen.Create) },
                        text = "✨ Create New Spark",
                        variant = FlintButtonVariant.PRIMARY
                    )
                }
            }
        } else {
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
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = asset.title,
                                style = FlintTheme.typography.titleLarge,
                                color = FlintTheme.colors.onSurface,
                                modifier = Modifier.weight(1f, fill = false),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
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
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
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
