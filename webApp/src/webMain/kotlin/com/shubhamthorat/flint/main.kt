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
                val token = requestGoogleTokenWeb(WEB_CLIENT_ID)
                val result = authRepository.signInWithGoogleIdToken(token)
                if (result is FlintResult.Error) {
                    throw Exception(result.error.message)
                }
            }
        )
    }
}

private suspend fun requestGoogleTokenWeb(clientId: String): String {
    val deferred = CompletableDeferred<String>()

    try {
        window.asDynamic().onGoogleTokenClientCallback = { response: dynamic ->
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
                if (!deferred.isCompleted) {
                    deferred.completeExceptionally(Exception("Google Sign-In Error ($fullMsg)"))
                }
            } else {
                val accessToken = response?.access_token as? String
                val idToken = response?.id_token as? String
                val token = idToken ?: accessToken
                if (!token.isNullOrBlank()) {
                    if (!deferred.isCompleted) {
                        deferred.complete(token)
                    }
                } else {
                    if (!deferred.isCompleted) {
                        deferred.completeExceptionally(Exception("Google Sign-In popup was closed or dismissed."))
                    }
                }
            }
        }

        window.asDynamic().onGoogleTokenClientError = { err: dynamic ->
            val msg = err?.message as? String ?: err?.type as? String ?: "Google OAuth Initialization Error"
            if (!deferred.isCompleted) {
                deferred.completeExceptionally(Exception(msg))
            }
        }

        val hasOAuth2 = js("typeof google !== 'undefined' && google.accounts && google.accounts.oauth2") as Boolean
        if (hasOAuth2) {
            val tokenClient = js("""
                google.accounts.oauth2.initTokenClient({
                    client_id: clientId,
                    scope: 'openid email profile',
                    callback: window.onGoogleTokenClientCallback,
                    error_callback: window.onGoogleTokenClientError
                })
            """)
            tokenClient.requestAccessToken(js("({ prompt: 'select_account' })"))
        } else {
            val hasGoogleId = js("typeof google !== 'undefined' && google.accounts && google.accounts.id") as Boolean
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
                        if (!deferred.isCompleted) {
                            deferred.completeExceptionally(Exception("Google Sign-In prompt not displayed ($reason)"))
                        }
                    }
                }
            } else {
                deferred.completeExceptionally(Exception("Google Identity Services script not loaded. Check your internet connection."))
            }
        }
    } catch (e: Throwable) {
        if (!deferred.isCompleted) {
            deferred.completeExceptionally(e)
        }
    }

    return withTimeoutOrNull(60.seconds) {
        deferred.await()
    } ?: throw Exception("Google Sign-In timed out.")
}
