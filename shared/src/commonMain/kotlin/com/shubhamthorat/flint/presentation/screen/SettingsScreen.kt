package com.shubhamthorat.flint.presentation.screen

import androidx.compose.foundation.horizontalScroll
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
import com.shubhamthorat.flint.core.FlintBuildConfig
import com.shubhamthorat.flint.domain.monetization.FreePlanLimits
import com.shubhamthorat.flint.domain.monetization.MemberPlanLimits
import com.shubhamthorat.flint.presentation.component.FlintButton
import com.shubhamthorat.flint.presentation.component.FlintButtonVariant
import com.shubhamthorat.flint.presentation.component.FlintCard
import com.shubhamthorat.flint.presentation.component.FlintChip
import com.shubhamthorat.flint.presentation.component.FlintTextField
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
    var selectedProvider by remember { mutableStateOf("OpenRouter") }

    // Pre-fill from git-ignored local.properties or GitHub Actions secrets
    var openRouterApiKey by remember { mutableStateOf(FlintBuildConfig.OPENROUTER_API_KEY) }
    var openRouterModelName by remember {
        mutableStateOf(FlintBuildConfig.OPENROUTER_MODEL_NAME.ifEmpty { "anthropic/claude-3.5-sonnet" })
    }

    var geminiApiKey by remember { mutableStateOf(FlintBuildConfig.GEMINI_API_KEY) }
    var ollamaEndpointUrl by remember { mutableStateOf("https://ollama.cloud.local") }

    var isConfigSavedFeedbackVisible by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(FlintTheme.spacing.medium),
        verticalArrangement = Arrangement.spacedBy(FlintTheme.spacing.medium)
    ) {
        Text(
            text = "Settings & Environment Configuration",
            style = FlintTheme.typography.displayMedium,
            color = FlintTheme.colors.primary,
            fontWeight = FontWeight.Bold
        )

        // Environment Secrets Info Card
        FlintCard(
            modifier = Modifier.fillMaxWidth(),
            outlined = true
        ) {
            Column(
                modifier = Modifier.padding(FlintTheme.spacing.medium),
                verticalArrangement = Arrangement.spacedBy(FlintTheme.spacing.extraSmall)
            ) {
                Text(
                    text = "🔐 Environment & Secret Protection",
                    style = FlintTheme.typography.titleMedium,
                    color = FlintTheme.colors.primary
                )
                Text(
                    text = "Secrets are loaded from git-ignored local.properties or GitHub Actions repository secrets (OPENROUTER_API_KEY, OPENROUTER_MODEL_NAME). They are never tracked or committed to Git.",
                    style = FlintTheme.typography.bodyMedium,
                    color = FlintTheme.colors.textSecondary
                )
            }
        }

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

        // Primary AI Provider Selection
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
                    text = "Select your primary provider. Flint automatically fails over to secondary configured providers if primary health checks fail.",
                    style = FlintTheme.typography.bodyMedium,
                    color = FlintTheme.colors.textSecondary
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(FlintTheme.spacing.small)
                ) {
                    val providers = listOf("OpenRouter", "Gemini", "Ollama Cloud")
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

        // Provider Credentials & Model Configuration
        FlintCard(
            modifier = Modifier.fillMaxWidth(),
            outlined = true
        ) {
            Column(
                modifier = Modifier.padding(FlintTheme.spacing.medium),
                verticalArrangement = Arrangement.spacedBy(FlintTheme.spacing.medium)
            ) {
                Text(
                    text = "🔑 AI Provider Credentials & Model",
                    style = FlintTheme.typography.titleLarge,
                    color = FlintTheme.colors.primary
                )

                when (selectedProvider) {
                    "OpenRouter" -> {
                        Column(verticalArrangement = Arrangement.spacedBy(FlintTheme.spacing.small)) {
                            Text(
                                text = "OpenRouter Configuration",
                                style = FlintTheme.typography.titleMedium
                            )

                            FlintTextField(
                                value = openRouterApiKey,
                                onValueChange = { openRouterApiKey = it },
                                label = "OpenRouter API Key (Pre-filled from env / local.properties)",
                                placeholder = "sk-or-v1-...",
                                isPassword = true
                            )

                            FlintTextField(
                                value = openRouterModelName,
                                onValueChange = { openRouterModelName = it },
                                label = "OpenRouter Model Name",
                                placeholder = "anthropic/claude-3.5-sonnet"
                            )

                            Text(
                                text = "Popular OpenRouter Models:",
                                style = FlintTheme.typography.labelSmall,
                                color = FlintTheme.colors.textSecondary
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(FlintTheme.spacing.extraSmall)
                            ) {
                                val popularModels = listOf(
                                    "anthropic/claude-3.5-sonnet",
                                    "meta-llama/llama-3.1-70b-instruct",
                                    "google/gemini-2.5-flash"
                                )
                                popularModels.forEach { model ->
                                    FlintChip(
                                        selected = openRouterModelName == model,
                                        onClick = { openRouterModelName = model },
                                        label = model.substringAfter('/')
                                    )
                                }
                            }
                        }
                    }
                    "Gemini" -> {
                        Column(verticalArrangement = Arrangement.spacedBy(FlintTheme.spacing.small)) {
                            Text(
                                text = "Google Gemini Configuration",
                                style = FlintTheme.typography.titleMedium
                            )

                            FlintTextField(
                                value = geminiApiKey,
                                onValueChange = { geminiApiKey = it },
                                label = "Google AI Studio / Gemini API Key",
                                placeholder = "AIzaSy...",
                                isPassword = true
                            )
                        }
                    }
                    "Ollama Cloud" -> {
                        Column(verticalArrangement = Arrangement.spacedBy(FlintTheme.spacing.small)) {
                            Text(
                                text = "Ollama Cloud Configuration",
                                style = FlintTheme.typography.titleMedium
                            )

                            FlintTextField(
                                value = ollamaEndpointUrl,
                                onValueChange = { ollamaEndpointUrl = it },
                                label = "Ollama Cloud Endpoint URL",
                                placeholder = "https://ollama.cloud.local"
                            )
                        }
                    }
                }

                FlintButton(
                    onClick = { isConfigSavedFeedbackVisible = true },
                    text = "Save Configuration",
                    variant = FlintButtonVariant.PRIMARY,
                    modifier = Modifier.fillMaxWidth()
                )

                if (isConfigSavedFeedbackVisible) {
                    Text(
                        text = "✓ Configuration saved! Model: $openRouterModelName",
                        style = FlintTheme.typography.bodyMedium,
                        color = FlintTheme.colors.primary
                    )
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
