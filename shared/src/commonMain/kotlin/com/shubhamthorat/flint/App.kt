package com.shubhamthorat.flint

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.shubhamthorat.flint.presentation.component.FlintChip
import com.shubhamthorat.flint.presentation.navigation.FlintAppScaffold
import com.shubhamthorat.flint.presentation.navigation.FlintScreen
import com.shubhamthorat.flint.presentation.navigation.NavigationManager
import com.shubhamthorat.flint.presentation.screen.CampaignsScreen
import com.shubhamthorat.flint.presentation.screen.ContentLibraryScreen
import com.shubhamthorat.flint.presentation.screen.CreateSparkScreen
import com.shubhamthorat.flint.presentation.screen.CreatorDnaScreen
import com.shubhamthorat.flint.presentation.screen.DashboardScreen
import com.shubhamthorat.flint.presentation.screen.SettingsScreen
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
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Brand Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = FlintTheme.spacing.medium,
                            vertical = FlintTheme.spacing.small
                        ),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "FLINT",
                            style = FlintTheme.typography.headlineLarge,
                            color = FlintTheme.colors.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "One spark. Endless stories.",
                            style = FlintTheme.typography.labelSmall,
                            color = FlintTheme.colors.textSecondary
                        )
                    }

                    FlintChip(
                        selected = isDarkTheme,
                        onClick = { isDarkTheme = !isDarkTheme },
                        label = if (isDarkTheme) "🌙 Dark" else "☀️ Light"
                    )
                }

                // Main App Navigation Scaffold & Screen Renderer
                FlintAppScaffold(
                    navigationManager = navigationManager,
                    isWideScreen = false
                ) { activeScreen ->
                    when (activeScreen) {
                        FlintScreen.Dashboard -> DashboardScreen(navigationManager = navigationManager)
                        FlintScreen.Create -> CreateSparkScreen(navigationManager = navigationManager)
                        FlintScreen.ContentLibrary -> ContentLibraryScreen()
                        FlintScreen.Campaigns -> CampaignsScreen()
                        FlintScreen.CreatorDNA -> CreatorDnaScreen()
                        FlintScreen.Settings, FlintScreen.Membership -> SettingsScreen(
                            navigationManager = navigationManager,
                            isDarkTheme = isDarkTheme,
                            onToggleDarkTheme = { isDarkTheme = !isDarkTheme }
                        )
                        else -> DashboardScreen(navigationManager = navigationManager)
                    }
                }
            }
        }
    }
}
