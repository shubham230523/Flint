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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.shubhamthorat.flint.data.repository.FirebaseAuthRepository
import com.shubhamthorat.flint.data.repository.FirestoreCampaignRepository
import com.shubhamthorat.flint.data.repository.FirestoreContentRepository
import com.shubhamthorat.flint.data.repository.FirestoreCreatorDnaRepository
import com.shubhamthorat.flint.data.repository.FirestoreSourceRepository
import com.shubhamthorat.flint.data.repository.InMemoryCampaignRepository
import com.shubhamthorat.flint.data.repository.InMemoryContentRepository
import com.shubhamthorat.flint.data.repository.InMemoryCreatorDnaRepository
import com.shubhamthorat.flint.data.repository.InMemorySourceRepository
import com.shubhamthorat.flint.domain.repository.AuthRepository
import com.shubhamthorat.flint.domain.repository.CampaignRepository
import com.shubhamthorat.flint.domain.repository.ContentRepository
import com.shubhamthorat.flint.domain.repository.CreatorDnaRepository
import com.shubhamthorat.flint.domain.repository.SourceRepository
import com.shubhamthorat.flint.presentation.component.FlintButton
import com.shubhamthorat.flint.presentation.component.FlintButtonVariant
import com.shubhamthorat.flint.presentation.component.FlintChip
import com.shubhamthorat.flint.presentation.navigation.FlintAppScaffold
import com.shubhamthorat.flint.presentation.navigation.FlintScreen
import com.shubhamthorat.flint.presentation.navigation.NavigationManager
import com.shubhamthorat.flint.presentation.screen.AnalyticsScreen
import com.shubhamthorat.flint.presentation.screen.AuthScreen
import com.shubhamthorat.flint.presentation.screen.CalendarScreen
import com.shubhamthorat.flint.presentation.screen.CampaignsScreen
import com.shubhamthorat.flint.presentation.screen.ContentLibraryScreen
import com.shubhamthorat.flint.presentation.screen.CreateSparkScreen
import com.shubhamthorat.flint.presentation.screen.CreatorDnaScreen
import com.shubhamthorat.flint.presentation.screen.DashboardScreen
import com.shubhamthorat.flint.presentation.screen.IdeaEngineScreen
import com.shubhamthorat.flint.presentation.screen.OnboardingScreen
import com.shubhamthorat.flint.presentation.screen.SettingsScreen
import com.shubhamthorat.flint.presentation.theme.FlintTheme
import kotlinx.coroutines.launch

@Composable
fun App() {
    var isDarkTheme by remember { mutableStateOf(false) }
    val navigationManager = remember { NavigationManager(initialScreen = FlintScreen.Dashboard) }
    val coroutineScope = rememberCoroutineScope()

    // Real Firebase Repositories (with fallback for Firestore if uninitialized)
    val authRepository: AuthRepository = remember { FirebaseAuthRepository() }
    val currentUser by authRepository.currentUserFlow.collectAsState(initial = null)

    val campaignRepository: CampaignRepository = remember {
        try {
            FirestoreCampaignRepository(authRepository)
        } catch (_: Throwable) {
            InMemoryCampaignRepository()
        }
    }

    val contentRepository: ContentRepository = remember {
        try {
            FirestoreContentRepository(authRepository)
        } catch (_: Throwable) {
            InMemoryContentRepository()
        }
    }

    val sourceRepository: SourceRepository = remember {
        try {
            FirestoreSourceRepository(authRepository)
        } catch (_: Throwable) {
            InMemorySourceRepository()
        }
    }

    val creatorDnaRepository: CreatorDnaRepository = remember {
        try {
            FirestoreCreatorDnaRepository(authRepository)
        } catch (_: Throwable) {
            InMemoryCreatorDnaRepository()
        }
    }

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
                            text = currentUser?.let { "Signed in as ${it.email ?: "Creator"}" } ?: "One spark. Endless stories.",
                            style = FlintTheme.typography.labelSmall,
                            color = FlintTheme.colors.textSecondary
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(FlintTheme.spacing.small),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (currentUser != null) {
                            FlintButton(
                                onClick = {
                                    coroutineScope.launch {
                                        authRepository.signOut()
                                    }
                                },
                                text = "Sign Out",
                                variant = FlintButtonVariant.TEXT
                            )
                        }

                        FlintChip(
                            selected = isDarkTheme,
                            onClick = { isDarkTheme = !isDarkTheme },
                            label = if (isDarkTheme) "🌙 Dark" else "☀️ Light"
                        )
                    }
                }

                if (currentUser == null) {
                    AuthScreen(
                        navigationManager = navigationManager,
                        authRepository = authRepository
                    )
                } else {
                    // Responsive Adaptive Navigation Scaffold
                    FlintAppScaffold(
                        navigationManager = navigationManager
                    ) { activeScreen ->
                        when (activeScreen) {
                            FlintScreen.Onboarding -> OnboardingScreen(navigationManager = navigationManager)
                            FlintScreen.Dashboard -> DashboardScreen(
                                navigationManager = navigationManager,
                                campaignRepository = campaignRepository,
                                contentRepository = contentRepository
                            )
                            FlintScreen.Create -> CreateSparkScreen(
                                navigationManager = navigationManager,
                                campaignRepository = campaignRepository,
                                contentRepository = contentRepository
                            )
                            FlintScreen.ContentLibrary -> ContentLibraryScreen(
                                navigationManager = navigationManager,
                                contentRepository = contentRepository
                            )
                            FlintScreen.Campaigns -> CampaignsScreen(
                                navigationManager = navigationManager,
                                campaignRepository = campaignRepository
                            )
                            FlintScreen.Calendar -> CalendarScreen(
                                navigationManager = navigationManager,
                                contentRepository = contentRepository
                            )
                            FlintScreen.Analytics -> AnalyticsScreen(
                                navigationManager = navigationManager,
                                contentRepository = contentRepository
                            )
                            FlintScreen.Projects -> IdeaEngineScreen(
                                navigationManager = navigationManager,
                                sourceRepository = sourceRepository
                            )
                            FlintScreen.CreatorDNA -> CreatorDnaScreen(
                                creatorDnaRepository = creatorDnaRepository
                            )
                            FlintScreen.Settings, FlintScreen.Membership -> SettingsScreen(
                                navigationManager = navigationManager,
                                isDarkTheme = isDarkTheme,
                                onToggleDarkTheme = { isDarkTheme = !isDarkTheme }
                            )
                            else -> DashboardScreen(
                                navigationManager = navigationManager,
                                campaignRepository = campaignRepository,
                                contentRepository = contentRepository
                            )
                        }
                    }
                }
            }
        }
    }
}
