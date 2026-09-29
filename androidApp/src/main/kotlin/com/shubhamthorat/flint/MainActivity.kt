package com.shubhamthorat.flint

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.shubhamthorat.flint.data.repository.FirebaseAuthRepository
import com.shubhamthorat.flint.domain.model.FlintResult
import com.shubhamthorat.flint.domain.repository.AuthRepository
import java.security.MessageDigest
import java.util.UUID

class MainActivity : ComponentActivity() {

    private val authRepository: AuthRepository by lazy { FirebaseAuthRepository() }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            val context = LocalContext.current
            val webClientId = stringResource(R.string.default_web_client_id)

            App(
                authRepository = authRepository,
                onGoogleSignInClick = {
                    val credentialManager = CredentialManager.create(context)

                    val rawNonce = UUID.randomUUID().toString()
                    val bytes = MessageDigest.getInstance("SHA-256").digest(rawNonce.toByteArray())
                    val hashedNonce = bytes.fold("") { str, it -> str + "%02x".format(it) }

                    val googleIdOption = GetGoogleIdOption.Builder()
                        .setFilterByAuthorizedAccounts(false)
                        .setServerClientId(webClientId)
                        .setNonce(hashedNonce)
                        .build()

                    val request = GetCredentialRequest.Builder()
                        .addCredentialOption(googleIdOption)
                        .build()

                    try {
                        val result = credentialManager.getCredential(
                            context = context,
                            request = request
                        )
                        val credential = result.credential
                        if (credential is CustomCredential &&
                            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                        ) {
                            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                            val idToken = googleIdTokenCredential.idToken
                            val signInResult = authRepository.signInWithGoogleIdToken(idToken)
                            if (signInResult is FlintResult.Error) {
                                throw Exception(signInResult.error.message)
                            }
                        } else {
                            throw Exception("Unexpected credential type returned from Google Sign-In.")
                        }
                    } catch (_: GetCredentialCancellationException) {
                        // User canceled sign-in prompt; do nothing
                    } catch (e: Exception) {
                        throw Exception(e.message ?: "Google Sign-In failed.")
                    }
                }
            )
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
