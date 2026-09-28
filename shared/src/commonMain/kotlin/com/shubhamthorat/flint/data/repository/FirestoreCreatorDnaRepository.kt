package com.shubhamthorat.flint.data.repository

import com.shubhamthorat.flint.core.FlintLogger
import com.shubhamthorat.flint.domain.model.AppError
import com.shubhamthorat.flint.domain.model.CreatorProfile
import com.shubhamthorat.flint.domain.model.FlintResult
import com.shubhamthorat.flint.domain.repository.CreatorDnaRepository
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalCoroutinesApi::class)
class FirestoreCreatorDnaRepository : CreatorDnaRepository {

    private val tag = "FirestoreCreatorDnaRepository"
    private val firestore = Firebase.firestore
    private val auth = Firebase.auth

    private val userProfileDocument
        get() = auth.currentUser?.uid?.let { uid ->
            firestore.collection("users").document(uid).collection("creator_dna").document("profile")
        }

    override fun observeProfile(): Flow<CreatorProfile?> {
        return auth.authStateChanged.flatMapLatest { fbUser ->
            if (fbUser == null) {
                flowOf(null)
            } else {
                firestore.collection("users").document(fbUser.uid).collection("creator_dna").document("profile")
                    .snapshots
                    .map { docSnapshot ->
                        if (docSnapshot.exists) {
                            try {
                                docSnapshot.data<CreatorProfile>()
                            } catch (e: Exception) {
                                FlintLogger.w(tag, "Failed to parse CreatorProfile doc: ${e.message}")
                                CreatorProfile(userId = fbUser.uid, handle = fbUser.email ?: "@creator")
                            }
                        } else {
                            CreatorProfile(userId = fbUser.uid, handle = fbUser.email ?: "@creator")
                        }
                    }
            }
        }
    }

    override suspend fun getProfile(): FlintResult<CreatorProfile, AppError> {
        val docRef = userProfileDocument
            ?: return FlintResult.Error(AppError.Auth("User must be signed in to access Creator DNA profile."))

        return try {
            val doc = docRef.get()
            if (doc.exists) {
                val profile = doc.data<CreatorProfile>()
                FlintResult.Success(profile)
            } else {
                val defaultProfile = CreatorProfile(
                    userId = auth.currentUser?.uid ?: "user_default",
                    handle = auth.currentUser?.email ?: "@creator"
                )
                FlintResult.Success(defaultProfile)
            }
        } catch (e: Exception) {
            FlintLogger.e(tag, "Failed to fetch CreatorProfile: ${e.message}")
            FlintResult.Error(AppError.Storage("Firestore error: ${e.message}"))
        }
    }

    override suspend fun updateProfile(profile: CreatorProfile): FlintResult<CreatorProfile, AppError> {
        val docRef = userProfileDocument
            ?: return FlintResult.Error(AppError.Auth("User must be signed in to update Creator DNA profile."))

        return try {
            docRef.set(profile)
            FlintLogger.i(tag, "Successfully updated CreatorProfile in Firestore for ${profile.handle}")
            FlintResult.Success(profile)
        } catch (e: Exception) {
            FlintLogger.e(tag, "Failed to update CreatorProfile: ${e.message}")
            FlintResult.Error(AppError.Storage("Firestore error: ${e.message}"))
        }
    }
}
