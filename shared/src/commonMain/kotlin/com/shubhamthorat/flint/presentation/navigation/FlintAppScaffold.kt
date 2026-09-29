package com.shubhamthorat.flint.presentation.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
        FlintScreen.YouTubeWorkspace,
        FlintScreen.ContentLibrary,
        FlintScreen.Campaigns,
        FlintScreen.CreatorDNA,
        FlintScreen.Settings
    )

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 720.dp

        if (isWideScreen) {
            Row(modifier = Modifier.fillMaxSize()) {
                // Desktop / Tablet Navigation Sidebar
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(220.dp)
                        .background(FlintTheme.colors.surface)
                        .padding(vertical = FlintTheme.spacing.medium, horizontal = FlintTheme.spacing.small),
                    verticalArrangement = Arrangement.spacedBy(FlintTheme.spacing.extraSmall)
                ) {
                    navItems.forEach { screen ->
                        val selected = currentScreen == screen
                        Surface(
                            onClick = { navigationManager.navigateTo(screen, clearBackstack = true) },
                            shape = RoundedCornerShape(FlintTheme.radius.medium),
                            color = if (selected) FlintTheme.colors.surfaceVariant else Color.Transparent,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = FlintTheme.spacing.medium),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = screen.title,
                                    style = FlintTheme.typography.labelLarge,
                                    color = if (selected) FlintTheme.colors.primary else FlintTheme.colors.onSurface,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    textAlign = TextAlign.Start,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
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
                    Surface(
                        color = FlintTheme.colors.surface,
                        tonalElevation = 3.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                                .padding(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            navItems.forEach { screen ->
                                val selected = currentScreen == screen
                                Surface(
                                    onClick = { navigationManager.navigateTo(screen, clearBackstack = true) },
                                    shape = RoundedCornerShape(FlintTheme.radius.medium),
                                    color = if (selected) FlintTheme.colors.primary.copy(alpha = 0.12f) else Color.Transparent,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .padding(horizontal = 2.dp)
                                ) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        val mobileTitle = when (screen) {
                                            FlintScreen.Create -> "Create"
                                            FlintScreen.ContentLibrary -> "Library"
                                            FlintScreen.YouTubeWorkspace -> "YouTube"
                                            FlintScreen.CreatorDNA -> "Brand DNA"
                                            else -> screen.title
                                        }
                                        Text(
                                            text = mobileTitle,
                                            style = FlintTheme.typography.labelSmall,
                                            color = if (selected) FlintTheme.colors.primary else FlintTheme.colors.onSurface,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                            textAlign = TextAlign.Center,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
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
