package com.shubhamthorat.flint.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.shubhamthorat.flint.core.FlintBuildConfig
import com.shubhamthorat.flint.domain.model.FlintResult
import com.shubhamthorat.flint.domain.repository.AuthRepository
import com.shubhamthorat.flint.presentation.component.FlintButton
import com.shubhamthorat.flint.presentation.component.FlintButtonVariant
import com.shubhamthorat.flint.presentation.component.FlintCard
import com.shubhamthorat.flint.presentation.component.FlintCircularProgressIndicator
import com.shubhamthorat.flint.presentation.component.FlintTextField
import com.shubhamthorat.flint.presentation.navigation.FlintScreen
import com.shubhamthorat.flint.presentation.navigation.NavigationManager
import com.shubhamthorat.flint.presentation.theme.FlintColorTokens
import com.shubhamthorat.flint.presentation.theme.FlintTheme
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(
    navigationManager: NavigationManager,
    authRepository: AuthRepository,
    onGoogleSignInClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var isSignUpMode by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf(FlintBuildConfig.TEST_EMAIL) }
    var password by remember { mutableStateOf(FlintBuildConfig.TEST_PASSWORD) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(FlintTheme.spacing.large),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // App Branding Header
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(FlintTheme.spacing.extraSmall)
        ) {
            Text(
                text = "FLINT",
                style = FlintTheme.typography.displayMedium,
                color = FlintTheme.colors.primary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "One spark. Endless stories.",
                style = FlintTheme.typography.bodyLarge,
                color = FlintTheme.colors.textSecondary
            )
        }

        Spacer(modifier = Modifier.height(FlintTheme.spacing.large))

        // Main Authentication Card (Reduced width by 60% for a compact, centered layout)
        FlintCard(
            modifier = Modifier
                .widthIn(max = 420.dp)
                .fillMaxWidth(0.9f)
        ) {
            Column(
                modifier = Modifier.padding(FlintTheme.spacing.large),
                verticalArrangement = Arrangement.spacedBy(FlintTheme.spacing.medium)
            ) {
                // Mode Toggle: Equal Weight Side-by-Side Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(FlintTheme.spacing.small)
                ) {
                    FlintButton(
                        onClick = { isSignUpMode = false; errorMessage = null },
                        text = "Sign In",
                        variant = if (!isSignUpMode) FlintButtonVariant.PRIMARY else FlintButtonVariant.OUTLINED,
                        modifier = Modifier.weight(1f)
                    )
                    FlintButton(
                        onClick = { isSignUpMode = true; errorMessage = null },
                        text = "Create Account",
                        variant = if (isSignUpMode) FlintButtonVariant.PRIMARY else FlintButtonVariant.OUTLINED,
                        modifier = Modifier.weight(1f)
                    )
                }

                Text(
                    text = if (isSignUpMode) "Join Flint AI Workspace" else "Welcome Back Creator",
                    style = FlintTheme.typography.titleLarge,
                    color = FlintTheme.colors.onSurface,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                // Google Sign-In Option
                FlintButton(
                    onClick = {
                        if (onGoogleSignInClick != null) {
                            onGoogleSignInClick()
                        } else {
                            errorMessage = "Google Sign-In is not configured for this platform."
                        }
                    },
                    text = if (isSignUpMode) "🌐 Sign up with Google" else "🌐 Sign in with Google",
                    variant = FlintButtonVariant.OUTLINED,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth()
                )

                // Visual Divider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(FlintTheme.spacing.small)
                ) {
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = FlintTheme.colors.surfaceVariant
                    )
                    Text(
                        text = "OR EMAIL",
                        style = FlintTheme.typography.labelSmall,
                        color = FlintTheme.colors.textSecondary
                    )
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = FlintTheme.colors.surfaceVariant
                    )
                }

                // Email & Password Fields
                FlintTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = "Email Address",
                    placeholder = "creator@example.com",
                    modifier = Modifier.fillMaxWidth()
                )

                FlintTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = "Password",
                    placeholder = "••••••••",
                    isPassword = true,
                    modifier = Modifier.fillMaxWidth()
                )

                errorMessage?.let { error ->
                    Text(
                        text = error,
                        style = FlintTheme.typography.bodyMedium,
                        color = FlintColorTokens.Error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Primary Submit Button
                FlintButton(
                    onClick = {
                        if (email.isNotBlank() && password.isNotBlank()) {
                            isLoading = true
                            errorMessage = null
                            coroutineScope.launch {
                                val result = if (isSignUpMode) {
                                    authRepository.signUpWithEmail(email.trim(), password)
                                } else {
                                    authRepository.signInWithEmail(email.trim(), password)
                                }
                                when (result) {
                                    is FlintResult.Success -> {
                                        navigationManager.navigateTo(FlintScreen.Dashboard)
                                    }
                                    is FlintResult.Error -> {
                                        errorMessage = result.error.message
                                    }
                                }
                                isLoading = false
                            }
                        }
                    },
                    text = if (isLoading) "Processing..." else if (isSignUpMode) "✨ Create Account with Email" else "🔐 Sign In with Email",
                    variant = FlintButtonVariant.PRIMARY,
                    enabled = !isLoading && email.isNotBlank() && password.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                )

                if (isLoading) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        FlintCircularProgressIndicator()
                    }
                }
            }
        }
    }
}
