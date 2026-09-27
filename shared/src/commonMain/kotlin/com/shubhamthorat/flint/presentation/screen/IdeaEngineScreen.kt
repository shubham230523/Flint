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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.shubhamthorat.flint.presentation.component.FlintButton
import com.shubhamthorat.flint.presentation.component.FlintButtonVariant
import com.shubhamthorat.flint.presentation.component.FlintCard
import com.shubhamthorat.flint.presentation.component.FlintChip
import com.shubhamthorat.flint.presentation.component.FlintTextField
import com.shubhamthorat.flint.presentation.navigation.FlintScreen
import com.shubhamthorat.flint.presentation.navigation.NavigationManager
import com.shubhamthorat.flint.presentation.theme.FlintTheme

@Composable
fun IdeaEngineScreen(
    navigationManager: NavigationManager,
    modifier: Modifier = Modifier
) {
    var newIdeaInput by remember { mutableStateOf("") }
    val ideasList = remember {
        mutableStateListOf(
            "Why Kotlin Multiplatform is replacing React Native for enterprise KMP apps.",
            "How to structure AI Provider Failover in production KMP apps.",
            "5 mistakes creators make when scheduling content across 4 platforms."
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
                            ideasList.add(0, newIdeaInput)
                            newIdeaInput = ""
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
            text = "Saved Ideas (${ideasList.size})",
            style = FlintTheme.typography.headlineMedium,
            color = FlintTheme.colors.onSurface
        )

        ideasList.forEach { idea ->
            FlintCard(
                modifier = Modifier.fillMaxWidth(),
                outlined = true
            ) {
                Column(
                    modifier = Modifier.padding(FlintTheme.spacing.medium),
                    verticalArrangement = Arrangement.spacedBy(FlintTheme.spacing.small)
                ) {
                    Text(text = idea, style = FlintTheme.typography.bodyLarge, color = FlintTheme.colors.onSurface)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        FlintChip(selected = false, onClick = {}, label = "Raw Idea")

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
