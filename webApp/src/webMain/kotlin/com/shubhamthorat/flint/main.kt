package com.shubhamthorat.flint

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.shubhamthorat.flint.data.repository.FirebaseAuthRepository
import com.shubhamthorat.flint.domain.model.FlintResult
import com.shubhamthorat.flint.domain.repository.AuthRepository
import kotlinx.browser.window
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.seconds

private const val WEB_CLIENT_ID = "598190078606-ugfhrppcnq1sgc4dq7uade5jp5r9p0ai.apps.googleusercontent.com"

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val authRepository: AuthRepository = FirebaseAuthRepository()

    ComposeViewport {
        App(
            authRepository = authRepository,
            onGoogleSignInClick = {
                println("🌐 [FLINT_WEB] User clicked 'Sign in with Google'")
                try {
                    val token = requestGoogleTokenWeb(WEB_CLIENT_ID)
                    println("🌐 [FLINT_WEB] Obtained Google token (length=${token.length}). Authenticating with Firebase...")
                    val result = authRepository.signInWithGoogleIdToken(token)
                    when (result) {
                        is FlintResult.Success -> {
                            println("✅ [FLINT_WEB] Google Sign-In SUCCESS for user: ${result.data.email}")
                        }
                        is FlintResult.Error -> {
                            println("❌ [FLINT_WEB] Firebase Google Auth Error: ${result.error.message}")
                            throw Exception(result.error.message)
                        }
                    }
                } catch (e: Exception) {
                    val msg = e.message.takeIf { !it.isNullOrBlank() } ?: e.toString()
                    println("🔴 [FLINT_WEB] Google Sign-In Exception: $msg")
                    throw Exception(msg)
                }
            }
        )
    }
}

private suspend fun requestGoogleTokenWeb(clientId: String): String {
    val deferred = CompletableDeferred<String>()

    try {
        window.asDynamic().onGoogleTokenClientCallback = { response: dynamic ->
            println("🌐 [FLINT_WEB] Received Google OAuth Callback response: $response")
            if (response != null && response.error != null) {
                val errCode = response.error as? String ?: "error"
                val errDesc = response.error_description as? String ?: ""
                val fullMsg = if (errDesc.isNotBlank()) {
                    "$errCode: $errDesc"
                } else if (errCode.contains("unregistered_origin", ignoreCase = true)) {
                    "unregistered_origin: Please add http://localhost:8080 to Authorized JavaScript origins in Google Cloud Console."
                } else {
                    errCode
                }
                println("❌ [FLINT_WEB] Google OAuth Callback returned error: $fullMsg")
                if (!deferred.isCompleted) {
                    deferred.completeExceptionally(Exception("Google Sign-In Error ($fullMsg)"))
                }
            } else {
                val accessToken = response?.access_token as? String
                val idToken = response?.id_token as? String
                val token = idToken ?: accessToken
                if (!token.isNullOrBlank()) {
                    println("✅ [FLINT_WEB] Successfully extracted token from Google Callback")
                    if (!deferred.isCompleted) {
                        deferred.complete(token)
                    }
                } else {
                    println("⚠️ [FLINT_WEB] Google Callback received but no token present")
                    if (!deferred.isCompleted) {
                        deferred.completeExceptionally(Exception("Google Sign-In popup was closed or dismissed."))
                    }
                }
            }
        }

        window.asDynamic().onGoogleTokenClientError = { err: dynamic ->
            val msg = err?.message as? String ?: err?.type as? String ?: err?.toString() ?: "Google OAuth Initialization Error"
            println("❌ [FLINT_WEB] Google OAuth Client Error: $msg")
            if (!deferred.isCompleted) {
                deferred.completeExceptionally(Exception(msg))
            }
        }

        val hasOAuth2 = js("typeof google !== 'undefined' && google.accounts && google.accounts.oauth2") == true
        if (hasOAuth2) {
            println("🌐 [FLINT_WEB] Initializing google.accounts.oauth2.initTokenClient with Client ID: $clientId")
            val tokenClientConfig = js("{}")
            tokenClientConfig.client_id = clientId
            tokenClientConfig.scope = "openid email profile"
            tokenClientConfig.callback = window.asDynamic().onGoogleTokenClientCallback
            tokenClientConfig.error_callback = window.asDynamic().onGoogleTokenClientError

            val googleAccounts = js("google.accounts.oauth2")
            val tokenClient = googleAccounts.initTokenClient(tokenClientConfig)

            val promptConfig = js("{}")
            promptConfig.prompt = "select_account"
            tokenClient.requestAccessToken(promptConfig)
        } else {
            println("⚠️ [FLINT_WEB] google.accounts.oauth2 not available, attempting google.accounts.id fallback...")
            val hasGoogleId = js("typeof google !== 'undefined' && google.accounts && google.accounts.id") == true
            if (hasGoogleId) {
                window.asDynamic().onGoogleSignInCallback = { response: dynamic ->
                    val token = response?.credential as? String
                    if (!token.isNullOrBlank()) {
                        if (!deferred.isCompleted) {
                            deferred.complete(token)
                        }
                    } else {
                        if (!deferred.isCompleted) {
                            deferred.completeExceptionally(Exception("Google Sign-In dismissed."))
                        }
                    }
                }
                val config = js("{}")
                config.client_id = clientId
                config.callback = window.asDynamic().onGoogleSignInCallback

                val googleId = js("google.accounts.id")
                googleId.initialize(config)
                googleId.prompt { notification: dynamic ->
                    val isNotDisp = notification?.asDynamic()?.isNotDisplayed() == true
                    val isSkipped = notification?.asDynamic()?.isSkippedMoment() == true
                    if (isNotDisp || isSkipped) {
                        val reason = notification?.asDynamic()?.getNotDisplayedReason()
                            ?: notification?.asDynamic()?.getSkippedReason()
                            ?: "prompt_dismissed"
                        println("⚠️ [FLINT_WEB] google.accounts.id prompt suppressed: $reason")
                        if (!deferred.isCompleted) {
                            deferred.completeExceptionally(Exception("Google Sign-In prompt not displayed ($reason)"))
                        }
                    }
                }
            } else {
                println("❌ [FLINT_WEB] Google Identity Services script not loaded!")
                deferred.completeExceptionally(Exception("Google Identity Services script not loaded. Check your internet connection."))
            }
        }
    } catch (e: Throwable) {
        val errText = e.message.takeIf { !it.isNullOrBlank() } ?: e.toString()
        println("🔴 [FLINT_WEB] Exception in requestGoogleTokenWeb: $errText")
        if (!deferred.isCompleted) {
            deferred.completeExceptionally(Exception(errText))
        }
    }

    return withTimeoutOrNull(60.seconds) {
        deferred.await()
    } ?: throw Exception("Google Sign-In timed out.")
}
