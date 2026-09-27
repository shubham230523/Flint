package com.shubhamthorat.flint.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.shubhamthorat.flint.presentation.navigation.FlintScreen
import com.shubhamthorat.flint.presentation.navigation.NavigationManager
import com.shubhamthorat.flint.presentation.theme.FlintTheme

data class OnboardingStep(
    val title: String,
    val description: String,
    val badge: String
)

@Composable
fun OnboardingScreen(
    navigationManager: NavigationManager,
    modifier: Modifier = Modifier
) {
    var activeStepIndex by remember { mutableStateOf(0) }

    val steps = remember {
        listOf(
            OnboardingStep("1. One Source Spark", "Start with a single raw idea, audio memo, YouTube link, or PDF document.", "⚡ Spark"),
            OnboardingStep("2. Authentic Creator DNA", "Flint applies your unique brand voice, tone, audience, and preferred hooks.", "🧬 DNA"),
            OnboardingStep("3. Multi-Channel Campaigns", "Instantly generate formatted LinkedIn posts, X threads, newsletters, and carousels.", "🚀 Campaign"),
            OnboardingStep("4. Performance Learning", "Flint analyzes engagement metrics and continuously refines future content.", "📈 Learn")
        )
    }

    val currentStep = steps[activeStepIndex.coerceIn(0, steps.size - 1)]

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(FlintTheme.spacing.large),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(FlintTheme.spacing.medium)
        ) {
            Text(
                text = "FLINT",
                style = FlintTheme.typography.displayLarge,
                color = FlintTheme.colors.primary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "One spark. Endless stories.",
                style = FlintTheme.typography.titleLarge,
                color = FlintTheme.colors.textSecondary
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Step Card
            FlintCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(FlintTheme.spacing.large),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(FlintTheme.spacing.medium)
                ) {
                    FlintChip(selected = true, onClick = {}, label = currentStep.badge)

                    Text(
                        text = currentStep.title,
                        style = FlintTheme.typography.displayMedium,
                        color = FlintTheme.colors.onSurface,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = currentStep.description,
                        style = FlintTheme.typography.bodyLarge,
                        color = FlintTheme.colors.textSecondary
                    )
                }
            }
        }

        // Navigation Controls
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(FlintTheme.spacing.medium)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(FlintTheme.spacing.small)
            ) {
                if (activeStepIndex < steps.size - 1) {
                    FlintButton(
                        onClick = { activeStepIndex++ },
                        text = "Next →",
                        variant = FlintButtonVariant.PRIMARY,
                        modifier = Modifier.weight(1f)
                    )
                    FlintButton(
                        onClick = { navigationManager.navigateTo(FlintScreen.Dashboard) },
                        text = "Skip Intro",
                        variant = FlintButtonVariant.TEXT,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    FlintButton(
                        onClick = { navigationManager.navigateTo(FlintScreen.Dashboard) },
                        text = "🚀 Get Started with Flint",
                        variant = FlintButtonVariant.PRIMARY,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
