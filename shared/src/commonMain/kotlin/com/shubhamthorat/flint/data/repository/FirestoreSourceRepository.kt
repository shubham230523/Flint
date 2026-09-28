package com.shubhamthorat.flint.data.repository

import com.shubhamthorat.flint.core.FlintLogger
import com.shubhamthorat.flint.domain.model.AppError
import com.shubhamthorat.flint.domain.model.FlintResult
import com.shubhamthorat.flint.domain.repository.SourceItem
import com.shubhamthorat.flint.domain.repository.SourceRepository
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalCoroutinesApi::class)
class FirestoreSourceRepository : SourceRepository {

    private val tag = "FirestoreSourceRepository"
    private val firestore = Firebase.firestore
    private val auth = Firebase.auth

    private val userSourcesCollection
        get() = auth.currentUser?.uid?.let { uid ->
            firestore.collection("users").document(uid).collection("sources")
        }

    override fun observeSources(): Flow<List<SourceItem>> {
        return auth.authStateChanged.flatMapLatest { fbUser ->
            if (fbUser == null) {
                flowOf(emptyList())
            } else {
                firestore.collection("users").document(fbUser.uid).collection("sources")
                    .snapshots
                    .map { querySnapshot ->
                        querySnapshot.documents.mapNotNull { doc ->
                            try {
                                doc.data<SourceItem>()
                            } catch (e: Exception) {
                                FlintLogger.w(tag, "Failed to parse source doc ${doc.id}: ${e.message}")
                                null
                            }
                        }
                    }
            }
        }
    }

    override suspend fun getSourceById(id: String): FlintResult<SourceItem, AppError> {
        val col = userSourcesCollection
            ?: return FlintResult.Error(AppError.Auth("User must be signed in to access sources."))

        return try {
            val doc = col.document(id).get()
            if (doc.exists) {
                val item = doc.data<SourceItem>()
                FlintResult.Success(item)
            } else {
                FlintResult.Error(AppError.Validation("Source item $id not found"))
            }
        } catch (e: Exception) {
            FlintLogger.e(tag, "Failed to fetch source $id: ${e.message}")
            FlintResult.Error(AppError.Storage("Firestore error: ${e.message}"))
        }
    }

    override suspend fun addSource(source: SourceItem): FlintResult<SourceItem, AppError> {
        val col = userSourcesCollection
            ?: return FlintResult.Error(AppError.Auth("User must be signed in to save sources."))

        return try {
            col.document(source.id).set(source)
            FlintLogger.i(tag, "Successfully added source ID ${source.id} (${source.title}) to Firestore")
            FlintResult.Success(source)
        } catch (e: Exception) {
            FlintLogger.e(tag, "Failed to add source ID ${source.id}: ${e.message}")
            FlintResult.Error(AppError.Storage("Firestore error: ${e.message}"))
        }
    }

    override suspend fun deleteSource(id: String): FlintResult<Unit, AppError> {
        val col = userSourcesCollection
            ?: return FlintResult.Error(AppError.Auth("User must be signed in to delete sources."))

        return try {
            col.document(id).delete()
            FlintLogger.i(tag, "Successfully deleted source ID $id from Firestore")
            FlintResult.Success(Unit)
        } catch (e: Exception) {
            FlintLogger.e(tag, "Failed to delete source ID $id: ${e.message}")
            FlintResult.Error(AppError.Storage("Firestore error: ${e.message}"))
        }
    }
}
