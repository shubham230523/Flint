package com.shubhamthorat.flint.domain.repository

import com.shubhamthorat.flint.domain.model.AppError
import com.shubhamthorat.flint.domain.model.FlintResult
import kotlinx.coroutines.flow.Flow

data class FlintUser(
    val id: String,
    val email: String?,
    val displayName: String?,
    val photoUrl: String?,
    val isAnonymous: Boolean = false
)

interface AuthRepository {
    val currentUserFlow: Flow<FlintUser?>
    suspend fun getCurrentUser(): FlintUser?
    suspend fun signInWithEmail(email: String, password: String): FlintResult<FlintUser, AppError>
    suspend fun signUpWithEmail(email: String, password: String): FlintResult<FlintUser, AppError>
    suspend fun signOut(): FlintResult<Unit, AppError>
}
