package com.shubhamthorat.flint.data.repository

import com.shubhamthorat.flint.core.FlintLogger
import com.shubhamthorat.flint.domain.model.AppError
import com.shubhamthorat.flint.domain.model.CreatorProfile
import com.shubhamthorat.flint.domain.model.FlintResult
import com.shubhamthorat.flint.domain.repository.AuthRepository
import com.shubhamthorat.flint.domain.repository.CreatorDnaRepository
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalCoroutinesApi::class)
class FirestoreCreatorDnaRepository(
    private val authRepository: AuthRepository = FirebaseAuthRepository(),
    private val restApi: FirestoreRestApi = FirestoreRestApi()
) : CreatorDnaRepository {

    private val tag = "FirestoreCreatorDnaRepository"
    private val cachedProfileFlow = MutableStateFlow<CreatorProfile?>(null)

    override fun observeProfile(): Flow<CreatorProfile?> {
        return authRepository.currentUserFlow.flatMapLatest { user ->
            if (user == null) {
                cachedProfileFlow.value = null
                flowOf(null)
            } else {
                flow {
                    try {
                        Firebase.firestore.collection("users").document(user.id).collection("creator_dna").document("profile")
                            .snapshots
                            .map { docSnapshot ->
                                if (docSnapshot.exists) {
                                    try { docSnapshot.data<CreatorProfile>() } catch (_: Exception) { null }
                                } else null
                            }.collect {
                                cachedProfileFlow.value = it
                                emit(it)
                            }
                    } catch (e: Throwable) {
                        FlintLogger.w(tag, "Native Firestore observe profile failed (${e.message}). Fetching from REST API...")
                        refreshProfileFromRest(user)
                        cachedProfileFlow.collect { emit(it) }
                    }
                }
            }
        }
    }

    private suspend fun refreshProfileFromRest(user: com.shubhamthorat.flint.domain.repository.FlintUser) {
        val idToken = authRepository.getIdToken()
        val profile = restApi.fetchProfile(user.id, idToken)
        cachedProfileFlow.value = profile
        if (profile != null) {
            FlintLogger.i(tag, "Fetched CreatorProfile for ${profile.handle} from Firestore REST API")
        } else {
            FlintLogger.i(tag, "No CreatorProfile found in Firestore for user ${user.id}")
        }
    }

    override suspend fun getProfile(): FlintResult<CreatorProfile, AppError> {
        val user = authRepository.getCurrentUser()
            ?: return FlintResult.Error(AppError.Auth("User must be signed in to access Creator DNA profile."))
        val idToken = authRepository.getIdToken()

        val profile = restApi.fetchProfile(user.id, idToken)
        cachedProfileFlow.value = profile
        return if (profile != null) {
            FlintResult.Success(profile)
        } else {
            FlintResult.Error(AppError.Validation("No CreatorProfile found in Firestore."))
        }
    }

    override suspend fun updateProfile(profile: CreatorProfile): FlintResult<CreatorProfile, AppError> {
        val user = authRepository.getCurrentUser()
            ?: return FlintResult.Error(AppError.Auth("User must be signed in to update Creator DNA profile."))
        val idToken = authRepository.getIdToken()

        try {
            val docRef = Firebase.firestore.collection("users").document(user.id).collection("creator_dna").document("profile")
            docRef.set(profile)
            FlintLogger.i(tag, "Successfully updated CreatorProfile in Firestore natively for ${profile.handle}")
            cachedProfileFlow.value = profile
            return FlintResult.Success(profile)
        } catch (e: Throwable) {
            FlintLogger.w(tag, "Native Firestore updateProfile failed (${e.message}). Falling back to Firestore REST API...")
        }

        val result = restApi.saveProfile(user.id, profile, idToken)
        if (result is FlintResult.Success) {
            cachedProfileFlow.value = profile
        }
        return result
    }
}
