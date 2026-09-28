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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.shubhamthorat.flint.data.repository.FirebaseAuthRepository
import com.shubhamthorat.flint.domain.model.FlintResult
import com.shubhamthorat.flint.presentation.component.FlintButton
import com.shubhamthorat.flint.presentation.component.FlintButtonVariant
import com.shubhamthorat.flint.presentation.component.FlintCard
import com.shubhamthorat.flint.presentation.component.FlintChip
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
    authRepository: FirebaseAuthRepository,
    modifier: Modifier = Modifier
) {
    var isSignUpMode by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
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

        FlintCard(
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            Column(
                modifier = Modifier.padding(FlintTheme.spacing.large),
                verticalArrangement = Arrangement.spacedBy(FlintTheme.spacing.medium)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    FlintChip(
                        selected = !isSignUpMode,
                        onClick = { isSignUpMode = false; errorMessage = null },
                        label = "Sign In"
                    )
                    FlintChip(
                        selected = isSignUpMode,
                        onClick = { isSignUpMode = true; errorMessage = null },
                        label = "Create Account"
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
                    text = if (isLoading) "Processing..." else if (isSignUpMode) "✨ Create Account" else "🔐 Sign In",
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
