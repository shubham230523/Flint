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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.shubhamthorat.flint.domain.monetization.FreePlanLimits
import com.shubhamthorat.flint.domain.monetization.MemberPlanLimits
import com.shubhamthorat.flint.presentation.component.FlintButton
import com.shubhamthorat.flint.presentation.component.FlintButtonVariant
import com.shubhamthorat.flint.presentation.component.FlintCard
import com.shubhamthorat.flint.presentation.component.FlintChip
import com.shubhamthorat.flint.presentation.navigation.FlintScreen
import com.shubhamthorat.flint.presentation.navigation.NavigationManager
import com.shubhamthorat.flint.presentation.theme.FlintTheme

@Composable
fun SettingsScreen(
    navigationManager: NavigationManager,
    isDarkTheme: Boolean,
    onToggleDarkTheme: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedProvider by remember { mutableStateOf("Gemini") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(FlintTheme.spacing.medium),
        verticalArrangement = Arrangement.spacedBy(FlintTheme.spacing.medium)
    ) {
        Text(
            text = "Settings & Membership",
            style = FlintTheme.typography.displayMedium,
            color = FlintTheme.colors.primary,
            fontWeight = FontWeight.Bold
        )

        // Membership Plan Card
        FlintCard(
            modifier = Modifier.fillMaxWidth(),
            outlined = true
        ) {
            Column(
                modifier = Modifier.padding(FlintTheme.spacing.medium),
                verticalArrangement = Arrangement.spacedBy(FlintTheme.spacing.small)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Current Membership: Free Tier",
                        style = FlintTheme.typography.titleLarge,
                        color = FlintTheme.colors.onSurface
                    )
                    FlintChip(selected = true, onClick = {}, label = "Active")
                }

                Text(
                    text = "Free Tier Includes: ${FreePlanLimits.MAX_AI_GENERATIONS} generations/mo, ${FreePlanLimits.MAX_PROJECTS} projects, non-intrusive dashboard ads.",
                    style = FlintTheme.typography.bodyMedium,
                    color = FlintTheme.colors.textSecondary
                )

                Text(
                    text = "Flint Member Includes: ${MemberPlanLimits.MAX_AI_GENERATIONS} generations/mo, ${MemberPlanLimits.MAX_PROJECTS} projects, priority processing, zero ads.",
                    style = FlintTheme.typography.bodyMedium,
                    color = FlintTheme.colors.primary
                )

                FlintButton(
                    onClick = { navigationManager.navigateTo(FlintScreen.Membership) },
                    text = "Upgrade to Flint Member",
                    variant = FlintButtonVariant.PRIMARY,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // AI Provider Settings
        FlintCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(FlintTheme.spacing.medium),
                verticalArrangement = Arrangement.spacedBy(FlintTheme.spacing.small)
            ) {
                Text(
                    text = "Primary AI Engine Provider",
                    style = FlintTheme.typography.titleLarge,
                    color = FlintTheme.colors.onSurface
                )
                Text(
                    text = "Select your preferred primary AI provider. Flint automatically falls back to alternative providers if primary health checks fail.",
                    style = FlintTheme.typography.bodyMedium,
                    color = FlintTheme.colors.textSecondary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(FlintTheme.spacing.small)
                ) {
                    val providers = listOf("Gemini", "OpenRouter", "Ollama Cloud")
                    providers.forEach { provider ->
                        FlintChip(
                            selected = selectedProvider == provider,
                            onClick = { selectedProvider = provider },
                            label = provider
                        )
                    }
                }
            }
        }

        // Appearance Settings
        FlintCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(FlintTheme.spacing.medium),
                verticalArrangement = Arrangement.spacedBy(FlintTheme.spacing.small)
            ) {
                Text(
                    text = "Appearance",
                    style = FlintTheme.typography.titleLarge,
                    color = FlintTheme.colors.onSurface
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (isDarkTheme) "Dark Theme Enabled 🌙" else "Light Theme Enabled ☀️",
                        style = FlintTheme.typography.bodyLarge,
                        color = FlintTheme.colors.onSurface
                    )

                    FlintChip(
                        selected = isDarkTheme,
                        onClick = onToggleDarkTheme,
                        label = if (isDarkTheme) "Switch Light" else "Switch Dark"
                    )
                }
            }
        }
    }
}
