package com.shubhamthorat.flint.data.repository

import com.shubhamthorat.flint.core.FlintLogger
import com.shubhamthorat.flint.domain.model.AppError
import com.shubhamthorat.flint.domain.model.FlintResult
import com.shubhamthorat.flint.domain.repository.AuthRepository
import com.shubhamthorat.flint.domain.repository.ContentAsset
import com.shubhamthorat.flint.domain.repository.ContentRepository
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalCoroutinesApi::class)
class FirestoreContentRepository(
    private val authRepository: AuthRepository = FirebaseAuthRepository(),
    private val restApi: FirestoreRestApi = FirestoreRestApi()
) : ContentRepository {

    private val tag = "FirestoreContentRepository"

    override fun observeContentAssets(): Flow<List<ContentAsset>> {
        return authRepository.currentUserFlow.flatMapLatest { user ->
            if (user == null) {
                flowOf(emptyList())
            } else {
                flow {
                    try {
                        Firebase.firestore.collection("users").document(user.id).collection("content")
                            .snapshots
                            .map { querySnapshot ->
                                querySnapshot.documents.mapNotNull { doc ->
                                    try { doc.data<ContentAsset>() } catch (_: Exception) { null }
                                }
                            }.collect { emit(it) }
                    } catch (e: Throwable) {
                        FlintLogger.w(tag, "Native Firestore observe content failed (${e.message}). Falling back to REST API...")
                        val idToken = authRepository.getIdToken()
                        val assets = restApi.fetchContentAssets(user.id, idToken)
                        emit(assets)
                    }
                }
            }
        }
    }

    override suspend fun getContentById(id: String): FlintResult<ContentAsset, AppError> {
        val user = authRepository.getCurrentUser()
            ?: return FlintResult.Error(AppError.Auth("User must be signed in to access content."))
        val idToken = authRepository.getIdToken()

        val allAssets = restApi.fetchContentAssets(user.id, idToken)
        val asset = allAssets.firstOrNull { it.id == id }
            ?: return FlintResult.Error(AppError.Validation("Content asset $id not found"))

        return FlintResult.Success(asset)
    }

    override suspend fun saveContent(asset: ContentAsset): FlintResult<ContentAsset, AppError> {
        val user = authRepository.getCurrentUser()
            ?: return FlintResult.Error(AppError.Auth("User must be signed in to save content."))
        val idToken = authRepository.getIdToken()

        try {
            val col = Firebase.firestore.collection("users").document(user.id).collection("content")
            col.document(asset.id).set(asset)
            FlintLogger.i(tag, "Successfully saved content asset ID ${asset.id} (${asset.title}) to Firestore natively")
            return FlintResult.Success(asset)
        } catch (e: Throwable) {
            FlintLogger.w(tag, "Native Firestore save content failed (${e.message}). Falling back to Firestore REST API...")
        }

        return restApi.saveContent(user.id, asset, idToken)
    }

    override suspend fun deleteContent(id: String): FlintResult<Unit, AppError> {
        val user = authRepository.getCurrentUser()
            ?: return FlintResult.Error(AppError.Auth("User must be signed in to delete content."))

        try {
            val col = Firebase.firestore.collection("users").document(user.id).collection("content")
            col.document(id).delete()
            return FlintResult.Success(Unit)
        } catch (_: Throwable) {
            return FlintResult.Success(Unit)
        }
    }
}
