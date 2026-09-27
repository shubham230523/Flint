package com.shubhamthorat.flint

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shubhamthorat.flint.presentation.component.FlintButton
import com.shubhamthorat.flint.presentation.component.FlintButtonVariant
import com.shubhamthorat.flint.presentation.component.FlintCard
import com.shubhamthorat.flint.presentation.component.FlintChip
import com.shubhamthorat.flint.presentation.navigation.FlintAppScaffold
import com.shubhamthorat.flint.presentation.navigation.FlintScreen
import com.shubhamthorat.flint.presentation.navigation.NavigationManager
import com.shubhamthorat.flint.presentation.theme.FlintTheme

@Composable
fun App() {
    var isDarkTheme by remember { mutableStateOf(false) }
    val navigationManager = remember { NavigationManager(initialScreen = FlintScreen.Dashboard) }

    FlintTheme(darkTheme = isDarkTheme) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = FlintTheme.colors.background
        ) {
            FlintAppScaffold(
                navigationManager = navigationManager,
                isWideScreen = false
            ) { activeScreen ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(FlintTheme.spacing.medium),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(FlintTheme.spacing.medium)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "FLINT",
                                style = FlintTheme.typography.displayMedium,
                                color = FlintTheme.colors.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "One spark. Endless stories.",
                                style = FlintTheme.typography.bodyMedium,
                                color = FlintTheme.colors.textSecondary
                            )
                        }

                        FlintChip(
                            selected = isDarkTheme,
                            onClick = { isDarkTheme = !isDarkTheme },
                            label = if (isDarkTheme) "🌙 Dark" else "☀️ Light"
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    FlintCard(
                        modifier = Modifier.fillMaxWidth(),
                        outlined = true
                    ) {
                        Column(
                            modifier = Modifier.padding(FlintTheme.spacing.medium),
                            verticalArrangement = Arrangement.spacedBy(FlintTheme.spacing.small)
                        ) {
                            Text(
                                text = activeScreen.title,
                                style = FlintTheme.typography.titleLarge,
                                color = FlintTheme.colors.onSurface
                            )
                            Text(
                                text = when (activeScreen) {
                                    FlintScreen.Dashboard -> "Welcome to Flint Dashboard. Select 'Create Spark' to turn an idea into multi-platform content."
                                    FlintScreen.Create -> "Spark Workspace: Input a video, audio, document, or raw idea to generate platform-adapted campaigns."
                                    FlintScreen.ContentLibrary -> "Content Library: Access your generated scripts, posts, threads, newsletters, and carousels."
                                    FlintScreen.Campaigns -> "Campaign Engine: View multi-channel campaigns generated from a single spark."
                                    FlintScreen.Settings -> "Settings: AI Providers, Preferences, Account, and Membership Management."
                                    else -> "Active section: ${activeScreen.title}"
                                },
                                style = FlintTheme.typography.bodyLarge,
                                color = FlintTheme.colors.textSecondary
                            )
                        }
                    }

                    if (activeScreen == FlintScreen.Dashboard) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(FlintTheme.spacing.small)
                        ) {
                            FlintButton(
                                onClick = { navigationManager.navigateTo(FlintScreen.Create) },
                                text = "New Spark",
                                variant = FlintButtonVariant.PRIMARY,
                                modifier = Modifier.weight(1f)
                            )
                            FlintButton(
                                onClick = { navigationManager.navigateTo(FlintScreen.CreatorDNA) },
                                text = "Creator DNA",
                                variant = FlintButtonVariant.SECONDARY,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}
