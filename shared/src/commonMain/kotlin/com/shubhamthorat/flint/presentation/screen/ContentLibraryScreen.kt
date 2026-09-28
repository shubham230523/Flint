package com.shubhamthorat.flint.presentation.screen

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.shubhamthorat.flint.domain.repository.ContentAsset
import com.shubhamthorat.flint.domain.repository.ContentRepository
import com.shubhamthorat.flint.domain.repository.ContentStatus
import com.shubhamthorat.flint.domain.repository.ContentType
import com.shubhamthorat.flint.presentation.component.FlintButton
import com.shubhamthorat.flint.presentation.component.FlintButtonVariant
import com.shubhamthorat.flint.presentation.component.FlintCard
import com.shubhamthorat.flint.presentation.component.FlintChip
import com.shubhamthorat.flint.presentation.component.FlintTextField
import com.shubhamthorat.flint.presentation.navigation.FlintScreen
import com.shubhamthorat.flint.presentation.navigation.NavigationManager
import com.shubhamthorat.flint.presentation.theme.FlintTheme
import kotlinx.coroutines.launch

@Composable
fun ContentLibraryScreen(
    navigationManager: NavigationManager,
    contentRepository: ContentRepository,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") }
    var selectedAssetForEdit by remember { mutableStateOf<ContentAsset?>(null) }

    val assets by contentRepository.observeContentAssets().collectAsState(initial = emptyList())
    val coroutineScope = rememberCoroutineScope()

    val filteredAssets = remember(assets, searchQuery, selectedFilter) {
        assets.filter { asset ->
            val matchesQuery = searchQuery.isBlank() ||
                    asset.title.contains(searchQuery, ignoreCase = true) ||
                    asset.body.contains(searchQuery, ignoreCase = true)

            val matchesFilter = when (selectedFilter) {
                "All" -> true
                else -> asset.platform.contains(selectedFilter, ignoreCase = true) ||
                        selectedFilter.contains(asset.platform, ignoreCase = true) ||
                        (selectedFilter.contains("X", ignoreCase = true) && asset.type == ContentType.X_THREAD) ||
                        (selectedFilter.contains("Newsletter", ignoreCase = true) && (asset.type == ContentType.NEWSLETTER || asset.type == ContentType.EMAIL))
            }
            matchesQuery && matchesFilter
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(FlintTheme.spacing.medium),
        verticalArrangement = Arrangement.spacedBy(FlintTheme.spacing.large)
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
            val filters = listOf("All", "LinkedIn", "X (Twitter)", "Newsletter", "YouTube", "Carousel")
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = FlintTheme.spacing.extraLarge, horizontal = FlintTheme.spacing.large),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(FlintTheme.spacing.medium)
                ) {
                    Text(
                        text = if (assets.isEmpty()) "Your content library is empty." else "No content found for '$selectedFilter'.",
                        style = FlintTheme.typography.titleLarge,
                        color = FlintTheme.colors.primary,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = if (assets.isEmpty()) "Generate your first campaign in the Spark workspace to populate your library." else "Try selecting 'All' or generating a new campaign for this category.",
                        style = FlintTheme.typography.bodyMedium,
                        color = FlintTheme.colors.textSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
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
                    onClick = { selectedAssetForEdit = asset }
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
                                onClick = { selectedAssetForEdit = asset },
                                text = "View & Edit",
                                variant = FlintButtonVariant.TEXT
                            )
                        }
                    }
                }
            }
        }

        // View & Edit Modal Dialog
        selectedAssetForEdit?.let { asset ->
            ContentEditModalDialog(
                asset = asset,
                onDismiss = { selectedAssetForEdit = null },
                onSave = { updatedAsset ->
                    coroutineScope.launch {
                        contentRepository.saveContent(updatedAsset)
                        selectedAssetForEdit = null
                    }
                }
            )
        }
    }
}

@Composable
private fun ContentEditModalDialog(
    asset: ContentAsset,
    onDismiss: () -> Unit,
    onSave: (ContentAsset) -> Unit
) {
    var editedTitle by remember(asset) { mutableStateOf(asset.title) }
    var editedBody by remember(asset) { mutableStateOf(asset.body) }
    var editedStatus by remember(asset) { mutableStateOf(asset.status) }
    var isCopiedFeedbackVisible by remember { mutableStateOf(false) }

    val clipboardManager = LocalClipboardManager.current

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(FlintTheme.spacing.medium),
            shape = RoundedCornerShape(FlintTheme.radius.large),
            colors = CardDefaults.cardColors(containerColor = FlintTheme.colors.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(FlintTheme.spacing.large)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(FlintTheme.spacing.medium)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Edit Content Asset",
                        style = FlintTheme.typography.headlineMedium,
                        color = FlintTheme.colors.primary,
                        fontWeight = FontWeight.Bold
                    )
                    FlintChip(selected = true, onClick = {}, label = asset.platform)
                }

                // Status Toggle Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(FlintTheme.spacing.small),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Status:",
                        style = FlintTheme.typography.titleMedium,
                        color = FlintTheme.colors.onSurface
                    )
                    listOf(ContentStatus.DRAFT, ContentStatus.PUBLISHED, ContentStatus.SCHEDULED).forEach { status ->
                        FlintChip(
                            selected = editedStatus == status,
                            onClick = { editedStatus = status },
                            label = status.name
                        )
                    }
                }

                FlintTextField(
                    value = editedTitle,
                    onValueChange = { editedTitle = it },
                    label = "Title",
                    modifier = Modifier.fillMaxWidth()
                )

                FlintTextField(
                    value = editedBody,
                    onValueChange = { editedBody = it },
                    label = "Content Body",
                    singleLine = false,
                    minLines = 8,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(FlintTheme.spacing.small)
                ) {
                    FlintButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(editedBody))
                            isCopiedFeedbackVisible = true
                        },
                        text = if (isCopiedFeedbackVisible) "✓ Copied!" else "📋 Copy Text",
                        variant = FlintButtonVariant.OUTLINED,
                        modifier = Modifier.weight(1f)
                    )

                    FlintButton(
                        onClick = {
                            val updated = asset.copy(title = editedTitle, body = editedBody, status = editedStatus)
                            onSave(updated)
                        },
                        text = "💾 Save Changes",
                        variant = FlintButtonVariant.PRIMARY,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    FlintButton(
                        onClick = onDismiss,
                        text = "Close",
                        variant = FlintButtonVariant.TEXT
                    )
                }
            }
        }
    }
}
