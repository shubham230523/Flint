package com.shubhamthorat.flint.data.repository

import com.shubhamthorat.flint.core.FlintLogger
import com.shubhamthorat.flint.domain.model.AppError
import com.shubhamthorat.flint.domain.model.FlintResult
import com.shubhamthorat.flint.domain.repository.AuthRepository
import com.shubhamthorat.flint.domain.repository.FlintUser
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.GoogleAuthProvider
import dev.gitlive.firebase.auth.auth
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class FirebaseAuthRestRequest(
    val email: String? = null,
    val password: String? = null,
    val returnSecureToken: Boolean = true
)

@Serializable
private data class FirebaseAuthRestIdpRequest(
    val postBody: String,
    val requestUri: String = "http://localhost",
    val returnSecureToken: Boolean = true
)

@Serializable
private data class FirebaseAuthRestResponse(
    val localId: String? = null,
    val email: String? = null,
    val idToken: String? = null,
    val refreshToken: String? = null,
    val error: FirebaseAuthRestErrorDetail? = null
)

@Serializable
private data class FirebaseAuthRestErrorContainer(
    val error: FirebaseAuthRestErrorDetail? = null
)

@Serializable
private data class FirebaseAuthRestErrorDetail(
    val message: String? = null,
    val code: Int? = null
)

class FirebaseAuthRepository(
    private val firebaseApiKey: String = "AIzaSyD8HK_j6JqduOivNgtAFgdf5i4sqpB5Cxs",
    private val httpClient: HttpClient = createDefaultHttpClient()
) : AuthRepository {

    private val tag = "FirebaseAuthRepository"
    private val restUserFlow = MutableStateFlow<FlintUser?>(null)

    override val currentUserFlow: Flow<FlintUser?>
        get() = try {
            Firebase.auth.authStateChanged.map { fbUser ->
                fbUser?.toFlintUser()
            }
        } catch (_: Throwable) {
            restUserFlow.asStateFlow()
        }

    override suspend fun getCurrentUser(): FlintUser? {
        return try {
            Firebase.auth.currentUser?.toFlintUser()
        } catch (_: Throwable) {
            restUserFlow.value
        }
    }

    override suspend fun signInWithEmail(email: String, password: String): FlintResult<FlintUser, AppError> {
        FlintLogger.i(tag, "Attempting email sign-in for $email")
        try {
            val authResult = Firebase.auth.signInWithEmailAndPassword(email, password)
            val user = authResult.user?.toFlintUser()
            if (user != null) {
                FlintLogger.i(tag, "Native Firebase Sign-in successful for user ID ${user.id}")
                return FlintResult.Success(user)
            }
        } catch (e: Throwable) {
            if (!isUninitializedFirebaseError(e.message)) {
                FlintLogger.e(tag, "Native Firebase Sign-In failed: ${e.message}")
                return FlintResult.Error(AppError.Auth(cleanFirebaseErrorMessage(e.message)))
            }
            FlintLogger.w(tag, "Native Firebase not initialized (${e.message}). Falling back to REST API...")
        }

        // Fallback to Firebase REST Auth API (for Desktop/Web or uninitialized native processes)
        return signInWithEmailRest(email, password)
    }

    override suspend fun signUpWithEmail(email: String, password: String): FlintResult<FlintUser, AppError> {
        FlintLogger.i(tag, "Attempting email registration for $email")
        try {
            val authResult = Firebase.auth.createUserWithEmailAndPassword(email, password)
            val user = authResult.user?.toFlintUser()
            if (user != null) {
                FlintLogger.i(tag, "Native Firebase Registration successful for user ID ${user.id}")
                return FlintResult.Success(user)
            }
        } catch (e: Throwable) {
            if (!isUninitializedFirebaseError(e.message)) {
                FlintLogger.e(tag, "Native Firebase Registration failed: ${e.message}")
                return FlintResult.Error(AppError.Auth(cleanFirebaseErrorMessage(e.message)))
            }
            FlintLogger.w(tag, "Native Firebase not initialized (${e.message}). Falling back to REST API...")
        }

        // Fallback to Firebase REST Auth API
        return signUpWithEmailRest(email, password)
    }

    override suspend fun signInWithGoogleIdToken(idToken: String): FlintResult<FlintUser, AppError> {
        FlintLogger.i(tag, "Attempting Google credential sign-in")
        try {
            val credential = GoogleAuthProvider.credential(idToken = idToken, accessToken = null)
            val authResult = Firebase.auth.signInWithCredential(credential)
            val user = authResult.user?.toFlintUser()
            if (user != null) {
                FlintLogger.i(tag, "Native Google sign-in successful for user ID ${user.id}")
                return FlintResult.Success(user)
            }
        } catch (e: Throwable) {
            FlintLogger.w(tag, "Native Google Sign-In not supported on this process (${e.message}). Falling back to Firebase REST API...")
        }

        return signInWithGoogleIdTokenRest(idToken)
    }

    private suspend fun signInWithGoogleIdTokenRest(idToken: String): FlintResult<FlintUser, AppError> {
        val url = "https://identitytoolkit.googleapis.com/v1/accounts:signInWithIdp?key=$firebaseApiKey"
        return try {
            val postPayload = if (idToken.startsWith("ya29.") || idToken.startsWith("1//")) {
                "access_token=$idToken&providerId=google.com"
            } else {
                "id_token=$idToken&providerId=google.com"
            }
            val requestBody = FirebaseAuthRestIdpRequest(
                postBody = postPayload,
                requestUri = "http://localhost",
                returnSecureToken = true
            )

            val response = httpClient.post(url) {
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }

            val bodyText = response.bodyAsText()

            if (!response.status.isSuccess()) {
                FlintLogger.e(tag, "Firebase REST Google Auth HTTP Error ${response.status.value}: $bodyText")
                val parsedError = parseRestErrorMessage(bodyText) ?: "HTTP ${response.status.value}"
                return FlintResult.Error(AppError.Auth(cleanFirebaseErrorMessage(parsedError)))
            }

            val res = jsonParser.decodeFromString<FirebaseAuthRestResponse>(bodyText)
            val userId = res.localId

            if (userId.isNullOrBlank()) {
                val err = res.error?.message ?: "Missing user ID in Firebase response."
                return FlintResult.Error(AppError.Auth(cleanFirebaseErrorMessage(err)))
            }

            val user = FlintUser(
                id = userId,
                email = res.email,
                displayName = res.email?.substringBefore('@') ?: "Creator",
                photoUrl = null,
                idToken = res.idToken
            )

            restUserFlow.value = user
            FlintLogger.i(tag, "Firebase REST Google Sign-In SUCCESS for user ID $userId")
            FlintResult.Success(user)
        } catch (e: Exception) {
            FlintLogger.e(tag, "Firebase REST Google Sign-In Exception: ${e.message}")
            FlintResult.Error(AppError.Auth("Google Sign-In failed: ${e.message ?: "Network error"}"))
        }
    }

    override suspend fun signOut(): FlintResult<Unit, AppError> {
        return try {
            try {
                Firebase.auth.signOut()
            } catch (_: Throwable) {}
            restUserFlow.value = null
            FlintLogger.i(tag, "User signed out successfully")
            FlintResult.Success(Unit)
        } catch (e: Exception) {
            FlintLogger.e(tag, "Sign-out error: ${e.message}")
            FlintResult.Error(AppError.Auth("Sign-out failed: ${e.message}"))
        }
    }

    private suspend fun signInWithEmailRest(email: String, password: String): FlintResult<FlintUser, AppError> {
        val url = "https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=$firebaseApiKey"
        return executeRestAuthCall(url, email, password, "Sign-In")
    }

    private suspend fun signUpWithEmailRest(email: String, password: String): FlintResult<FlintUser, AppError> {
        val url = "https://identitytoolkit.googleapis.com/v1/accounts:signUp?key=$firebaseApiKey"
        return executeRestAuthCall(url, email, password, "Registration")
    }

    private suspend fun executeRestAuthCall(
        url: String,
        email: String,
        password: String,
        actionName: String
    ): FlintResult<FlintUser, AppError> {
        return try {
            val response = httpClient.post(url) {
                contentType(ContentType.Application.Json)
                setBody(FirebaseAuthRestRequest(email = email, password = password))
            }

            val bodyText = response.bodyAsText()

            if (!response.status.isSuccess()) {
                FlintLogger.e(tag, "Firebase REST Auth HTTP Error ${response.status.value}: $bodyText")
                val parsedError = parseRestErrorMessage(bodyText) ?: "HTTP ${response.status.value}"
                return FlintResult.Error(AppError.Auth(cleanFirebaseErrorMessage(parsedError)))
            }

            val res = jsonParser.decodeFromString<FirebaseAuthRestResponse>(bodyText)
            val userId = res.localId

            if (userId.isNullOrBlank()) {
                val err = res.error?.message ?: "Missing user ID in Firebase response."
                return FlintResult.Error(AppError.Auth(cleanFirebaseErrorMessage(err)))
            }

            val user = FlintUser(
                id = userId,
                email = res.email ?: email,
                displayName = (res.email ?: email).substringBefore('@'),
                photoUrl = null,
                idToken = res.idToken
            )

            restUserFlow.value = user
            FlintLogger.i(tag, "Firebase REST $actionName SUCCESS for user ID $userId")
            FlintResult.Success(user)
        } catch (e: Exception) {
            FlintLogger.e(tag, "Firebase REST $actionName Exception: ${e.message}")
            FlintResult.Error(AppError.Auth("Firebase $actionName failed: ${e.message ?: "Network error"}"))
        }
    }

    override suspend fun getIdToken(): String? {
        return try {
            Firebase.auth.currentUser?.getIdToken(false)
        } catch (_: Throwable) {
            restUserFlow.value?.idToken
        } ?: restUserFlow.value?.idToken
    }

    private fun parseRestErrorMessage(jsonText: String): String? {
        return try {
            val container = jsonParser.decodeFromString<FirebaseAuthRestErrorContainer>(jsonText)
            container.error?.message
        } catch (_: Exception) {
            null
        }
    }

    private fun isUninitializedFirebaseError(msg: String?): Boolean {
        if (msg.isNullOrBlank()) return true
        val lower = msg.lowercase()
        return lower.contains("firebaseapp is not initialized") ||
                lower.contains("no firebase app") ||
                lower.contains("app/no-app") ||
                lower.contains("initializeapp") ||
                lower.contains("not initialized")
    }

    private fun cleanFirebaseErrorMessage(raw: String?): String {
        if (raw.isNullOrBlank()) return "Authentication failed. Please check your credentials."
        return when {
            raw.contains("EMAIL_NOT_FOUND", ignoreCase = true) ->
                "Account not found. If you are a new user, please click 'Sign Up' above first."
            raw.contains("INVALID_LOGIN_CREDENTIALS", ignoreCase = true) ->
                "Invalid credentials or account not registered. Please check your password or switch to 'Sign Up'."
            raw.contains("INVALID_PASSWORD", ignoreCase = true) -> "Incorrect password. Please try again."
            raw.contains("EMAIL_EXISTS", ignoreCase = true) -> "An account with this email address already exists. Please switch to 'Sign In'."
            raw.contains("WEAK_PASSWORD", ignoreCase = true) -> "Password is too weak. Please use at least 6 characters."
            raw.contains("INVALID_EMAIL", ignoreCase = true) -> "Please enter a valid email address."
            raw.contains("USER_DISABLED", ignoreCase = true) -> "This user account has been disabled."
            raw.contains("OPERATION_NOT_ALLOWED", ignoreCase = true) ->
                "Email/Password sign-in is disabled in Firebase Console. Enable 'Email/Password' under Firebase Console > Authentication > Sign-in method."
            raw.contains("INVALID_IDP_RESPONSE", ignoreCase = true) ->
                "Invalid Google credential or Web Client ID mismatch."
            else -> raw.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }
        }
    }

    private fun dev.gitlive.firebase.auth.FirebaseUser.toFlintUser(): FlintUser {
        return FlintUser(
            id = uid,
            email = email,
            displayName = displayName,
            photoUrl = photoURL,
            isAnonymous = isAnonymous
        )
    }

    companion object {
        private val jsonParser = Json {
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = true
        }

        private fun createDefaultHttpClient(): HttpClient {
            return HttpClient {
                install(ContentNegotiation) {
                    json(jsonParser)
                }
                install(io.ktor.client.plugins.HttpTimeout) {
                    requestTimeoutMillis = 30_000L
                    connectTimeoutMillis = 15_000L
                }
            }
        }
    }
}
