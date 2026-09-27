package com.shubhamthorat.flint.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shubhamthorat.flint.presentation.theme.FlintTheme

@Composable
fun FlintAppScaffold(
    navigationManager: NavigationManager,
    content: @Composable (FlintScreen) -> Unit
) {
    val currentScreen by navigationManager.currentScreen.collectAsState()
    val navItems = listOf(
        FlintScreen.Dashboard,
        FlintScreen.Create,
        FlintScreen.ContentLibrary,
        FlintScreen.Campaigns,
        FlintScreen.Settings
    )

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 720.dp

        if (isWideScreen) {
            Row(modifier = Modifier.fillMaxSize()) {
                NavigationRail(
                    modifier = Modifier.fillMaxHeight(),
                    containerColor = FlintTheme.colors.surface,
                    contentColor = FlintTheme.colors.onSurface
                ) {
                    navItems.forEach { screen ->
                        val selected = currentScreen == screen
                        NavigationRailItem(
                            selected = selected,
                            onClick = { navigationManager.navigateTo(screen, clearBackstack = true) },
                            icon = {},
                            label = {
                                Text(
                                    text = screen.title,
                                    style = FlintTheme.typography.labelSmall
                                )
                            },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = FlintTheme.colors.primary,
                                selectedTextColor = FlintTheme.colors.primary,
                                indicatorColor = FlintTheme.colors.surfaceVariant
                            )
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = FlintTheme.spacing.medium),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 1200.dp)
                    ) {
                        content(currentScreen)
                    }
                }
            }
        } else {
            Scaffold(
                bottomBar = {
                    NavigationBar(
                        containerColor = FlintTheme.colors.surface,
                        contentColor = FlintTheme.colors.onSurface
                    ) {
                        navItems.forEach { screen ->
                            val selected = currentScreen == screen
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigationManager.navigateTo(screen, clearBackstack = true) },
                                icon = {},
                                label = {
                                    Text(
                                        text = screen.title,
                                        style = FlintTheme.typography.labelSmall
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = FlintTheme.colors.primary,
                                    selectedTextColor = FlintTheme.colors.primary,
                                    indicatorColor = FlintTheme.colors.surfaceVariant
                                )
                            )
                        }
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    content(currentScreen)
                }
            }
        }
    }
}
