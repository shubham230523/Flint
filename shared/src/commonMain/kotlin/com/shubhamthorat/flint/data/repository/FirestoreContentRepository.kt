package com.shubhamthorat.flint.data.repository

import com.shubhamthorat.flint.core.FlintLogger
import com.shubhamthorat.flint.domain.model.AppError
import com.shubhamthorat.flint.domain.model.FlintResult
import com.shubhamthorat.flint.domain.repository.ContentAsset
import com.shubhamthorat.flint.domain.repository.ContentRepository
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalCoroutinesApi::class)
class FirestoreContentRepository : ContentRepository {

    private val tag = "FirestoreContentRepository"
    private val firestore = Firebase.firestore
    private val auth = Firebase.auth

    private val userContentCollection
        get() = auth.currentUser?.uid?.let { uid ->
            firestore.collection("users").document(uid).collection("content")
        }

    override fun observeContentAssets(): Flow<List<ContentAsset>> {
        return auth.authStateChanged.flatMapLatest { fbUser ->
            if (fbUser == null) {
                flowOf(emptyList())
            } else {
                firestore.collection("users").document(fbUser.uid).collection("content")
                    .snapshots
                    .map { querySnapshot ->
                        querySnapshot.documents.mapNotNull { doc ->
                            try {
                                doc.data<ContentAsset>()
                            } catch (e: Exception) {
                                FlintLogger.w(tag, "Failed to parse content doc ${doc.id}: ${e.message}")
                                null
                            }
                        }
                    }
            }
        }
    }

    override suspend fun getContentById(id: String): FlintResult<ContentAsset, AppError> {
        val col = userContentCollection
            ?: return FlintResult.Error(AppError.Auth("User must be signed in to access content."))

        return try {
            val doc = col.document(id).get()
            if (doc.exists) {
                val asset = doc.data<ContentAsset>()
                FlintResult.Success(asset)
            } else {
                FlintResult.Error(AppError.Validation("Content asset $id not found"))
            }
        } catch (e: Exception) {
            FlintLogger.e(tag, "Failed to fetch content $id: ${e.message}")
            FlintResult.Error(AppError.Storage("Firestore error: ${e.message}"))
        }
    }

    override suspend fun saveContent(asset: ContentAsset): FlintResult<ContentAsset, AppError> {
        val col = userContentCollection
            ?: return FlintResult.Error(AppError.Auth("User must be signed in to save content."))

        return try {
            col.document(asset.id).set(asset)
            FlintLogger.i(tag, "Successfully saved content asset ID ${asset.id} (${asset.title}) to Firestore")
            FlintResult.Success(asset)
        } catch (e: Exception) {
            FlintLogger.e(tag, "Failed to save content asset ID ${asset.id}: ${e.message}")
            FlintResult.Error(AppError.Storage("Firestore error: ${e.message}"))
        }
    }

    override suspend fun deleteContent(id: String): FlintResult<Unit, AppError> {
        val col = userContentCollection
            ?: return FlintResult.Error(AppError.Auth("User must be signed in to delete content."))

        return try {
            col.document(id).delete()
            FlintLogger.i(tag, "Successfully deleted content asset ID $id from Firestore")
            FlintResult.Success(Unit)
        } catch (e: Exception) {
            FlintLogger.e(tag, "Failed to delete content asset ID $id: ${e.message}")
            FlintResult.Error(AppError.Storage("Firestore error: ${e.message}"))
        }
    }
}
