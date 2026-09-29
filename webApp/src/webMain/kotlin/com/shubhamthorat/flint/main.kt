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
                if (!token.isNullOrBlank()) {
                    val result = authRepository.signInWithGoogleIdToken(token)
                    if (result is FlintResult.Error) {
                        throw Exception(result.error.message)
                    }
                } else {
                    throw Exception("Google Sign-In prompt was dismissed or popup was closed.")
                }
            }
        )
    }
}

private suspend fun requestGoogleTokenWeb(clientId: String): String? {
    val deferred = CompletableDeferred<String?>()

    try {
        window.asDynamic().onGoogleTokenClientCallback = { response: dynamic ->
            val accessToken = response?.access_token as? String
            val idToken = response?.id_token as? String
            val token = idToken ?: accessToken
            if (!deferred.isCompleted) {
                deferred.complete(token)
            }
        }

        val hasOAuth2 = js("typeof google !== 'undefined' && google.accounts && google.accounts.oauth2") as Boolean
        if (hasOAuth2) {
            val tokenClient = js("""
                google.accounts.oauth2.initTokenClient({
                    client_id: clientId,
                    scope: 'openid email profile',
                    callback: window.onGoogleTokenClientCallback
                })
            """)
            tokenClient.requestAccessToken(js("({ prompt: 'select_account' })"))
        } else {
            val hasGoogleId = js("typeof google !== 'undefined' && google.accounts && google.accounts.id") as Boolean
            if (hasGoogleId) {
                window.asDynamic().onGoogleSignInCallback = { response: dynamic ->
                    val token = response?.credential as? String
                    if (!deferred.isCompleted) {
                        deferred.complete(token)
                    }
                }
                val config = js("{}")
                config.client_id = clientId
                config.callback = window.asDynamic().onGoogleSignInCallback

                val googleId = js("google.accounts.id")
                googleId.initialize(config)
                googleId.prompt { notification: dynamic ->
                    if (notification?.isNotDisplayed() == true || notification?.isSkippedMoment() == true) {
                        if (!deferred.isCompleted) {
                            deferred.complete(null)
                        }
                    }
                }
            } else {
                deferred.complete(null)
            }
        }
    } catch (_: Throwable) {
        if (!deferred.isCompleted) {
            deferred.complete(null)
        }
    }

    return withTimeoutOrNull(60.seconds) {
        deferred.await()
    }
}
