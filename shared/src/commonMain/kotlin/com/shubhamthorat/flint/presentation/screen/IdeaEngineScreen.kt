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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.shubhamthorat.flint.domain.repository.SourceItem
import com.shubhamthorat.flint.domain.repository.SourceRepository
import com.shubhamthorat.flint.domain.repository.SourceType
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
fun IdeaEngineScreen(
    navigationManager: NavigationManager,
    sourceRepository: SourceRepository,
    modifier: Modifier = Modifier
) {
    var newIdeaInput by remember { mutableStateOf("") }
    val sources by sourceRepository.observeSources().collectAsState(initial = emptyList())
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(FlintTheme.spacing.medium),
        verticalArrangement = Arrangement.spacedBy(FlintTheme.spacing.medium)
    ) {
        Text(
            text = "Content Idea Inbox & Topic Gap Finder",
            style = FlintTheme.typography.displayMedium,
            color = FlintTheme.colors.primary,
            fontWeight = FontWeight.Bold
        )

        // Quick Idea Capture Card
        FlintCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(FlintTheme.spacing.medium),
                verticalArrangement = Arrangement.spacedBy(FlintTheme.spacing.small)
            ) {
                Text(text = "💡 Quick Idea Capture", style = FlintTheme.typography.titleLarge)

                FlintTextField(
                    value = newIdeaInput,
                    onValueChange = { newIdeaInput = it },
                    placeholder = "Capture a raw spark, shower thought, or article link...",
                    modifier = Modifier.fillMaxWidth()
                )

                FlintButton(
                    onClick = {
                        if (newIdeaInput.isNotBlank()) {
                            coroutineScope.launch {
                                val item = SourceItem(
                                    id = "source_${sources.size + 1}",
                                    title = newIdeaInput.take(30),
                                    type = SourceType.IDEA,
                                    contentOrUrl = newIdeaInput
                                )
                                sourceRepository.addSource(item)
                                newIdeaInput = ""
                            }
                        }
                    },
                    text = "+ Save to Idea Inbox",
                    variant = FlintButtonVariant.PRIMARY,
                    enabled = newIdeaInput.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Ideas List
        Text(
            text = "Saved Ideas (${sources.size})",
            style = FlintTheme.typography.headlineMedium,
            color = FlintTheme.colors.onSurface
        )

        if (sources.isEmpty()) {
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
                        text = "Your idea inbox is empty.",
                        style = FlintTheme.typography.titleLarge,
                        color = FlintTheme.colors.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Type a shower thought, link, or topic above to save it for your next spark campaign.",
                        style = FlintTheme.typography.bodyMedium,
                        color = FlintTheme.colors.textSecondary
                    )
                }
            }
        } else {
            sources.forEach { source ->
                FlintCard(
                    modifier = Modifier.fillMaxWidth(),
                    outlined = true
                ) {
                    Column(
                        modifier = Modifier.padding(FlintTheme.spacing.medium),
                        verticalArrangement = Arrangement.spacedBy(FlintTheme.spacing.small)
                    ) {
                        Text(
                            text = source.contentOrUrl,
                            style = FlintTheme.typography.bodyLarge,
                            color = FlintTheme.colors.onSurface
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FlintChip(selected = false, onClick = {}, label = source.type.name)

                            FlintButton(
                                onClick = { navigationManager.navigateTo(FlintScreen.Create) },
                                text = "✨ Turn into Spark Campaign",
                                variant = FlintButtonVariant.SECONDARY
                            )
                        }
                    }
                }
            }
        }
    }
}
