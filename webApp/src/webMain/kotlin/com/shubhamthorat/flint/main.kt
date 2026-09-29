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
                val idToken = requestGoogleIdTokenWeb(WEB_CLIENT_ID)
                if (!idToken.isNullOrBlank()) {
                    val result = authRepository.signInWithGoogleIdToken(idToken)
                    if (result is FlintResult.Error) {
                        throw Exception(result.error.message)
                    }
                } else {
                    throw Exception("Google Sign-In prompt was dismissed or failed.")
                }
            }
        )
    }
}

private suspend fun requestGoogleIdTokenWeb(clientId: String): String? {
    val deferred = CompletableDeferred<String?>()

    try {
        window.asDynamic().onGoogleSignInCallback = { response: dynamic ->
            val token = response?.credential as? String
            deferred.complete(token)
        }

        val hasGoogle = js("typeof google !== 'undefined' && google.accounts && google.accounts.id") as Boolean
        if (hasGoogle) {
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
    } catch (_: Throwable) {
        if (!deferred.isCompleted) {
            deferred.complete(null)
        }
    }

    return withTimeoutOrNull(60.seconds) {
        deferred.await()
    }
}
