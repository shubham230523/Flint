package com.shubhamthorat.flint.data.repository

import com.shubhamthorat.flint.core.FlintLogger
import com.shubhamthorat.flint.domain.model.AppError
import com.shubhamthorat.flint.domain.model.FlintResult
import com.shubhamthorat.flint.domain.repository.AuthRepository
import com.shubhamthorat.flint.domain.repository.FlintUser
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.GoogleAuthProvider
import dev.gitlive.firebase.auth.auth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FirebaseAuthRepository : AuthRepository {

    private val tag = "FirebaseAuthRepository"
    private val auth = Firebase.auth

    override val currentUserFlow: Flow<FlintUser?> = auth.authStateChanged.map { fbUser ->
        fbUser?.toFlintUser()
    }

    override suspend fun getCurrentUser(): FlintUser? {
        return auth.currentUser?.toFlintUser()
    }

    override suspend fun signInWithEmail(email: String, password: String): FlintResult<FlintUser, AppError> {
        return try {
            FlintLogger.i(tag, "Attempting email sign-in for $email")
            val authResult = auth.signInWithEmailAndPassword(email, password)
            val user = authResult.user?.toFlintUser()
            if (user != null) {
                FlintLogger.i(tag, "Sign-in successful for user ID ${user.id} ($email)")
                FlintResult.Success(user)
            } else {
                FlintResult.Error(AppError.Auth("Sign-in failed: User details missing."))
            }
        } catch (e: Exception) {
            FlintLogger.e(tag, "Sign-in error: ${e.message}")
            FlintResult.Error(AppError.Auth("Sign-in failed: ${e.message ?: "Authentication error"}"))
        }
    }

    override suspend fun signUpWithEmail(email: String, password: String): FlintResult<FlintUser, AppError> {
        return try {
            FlintLogger.i(tag, "Attempting email registration for $email")
            val authResult = auth.createUserWithEmailAndPassword(email, password)
            val user = authResult.user?.toFlintUser()
            if (user != null) {
                FlintLogger.i(tag, "Registration successful for user ID ${user.id} ($email)")
                FlintResult.Success(user)
            } else {
                FlintResult.Error(AppError.Auth("Registration failed: User details missing."))
            }
        } catch (e: Exception) {
            FlintLogger.e(tag, "Registration error: ${e.message}")
            FlintResult.Error(AppError.Auth("Registration failed: ${e.message ?: "Account creation error"}"))
        }
    }

    suspend fun signInWithGoogleIdToken(idToken: String): FlintResult<FlintUser, AppError> {
        return try {
            FlintLogger.i(tag, "Attempting Google credential sign-in")
            val credential = GoogleAuthProvider.credential(idToken = idToken, accessToken = null)
            val authResult = auth.signInWithCredential(credential)
            val user = authResult.user?.toFlintUser()
            if (user != null) {
                FlintLogger.i(tag, "Google sign-in successful for user ID ${user.id}")
                FlintResult.Success(user)
            } else {
                FlintResult.Error(AppError.Auth("Google Sign-In failed: User details missing."))
            }
        } catch (e: Exception) {
            FlintLogger.e(tag, "Google Sign-In error: ${e.message}")
            FlintResult.Error(AppError.Auth("Google Sign-In failed: ${e.message ?: "Authentication error"}"))
        }
    }

    override suspend fun signOut(): FlintResult<Unit, AppError> {
        return try {
            auth.signOut()
            FlintLogger.i(tag, "User signed out successfully")
            FlintResult.Success(Unit)
        } catch (e: Exception) {
            FlintLogger.e(tag, "Sign-out error: ${e.message}")
            FlintResult.Error(AppError.Auth("Sign-out failed: ${e.message}"))
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
}
