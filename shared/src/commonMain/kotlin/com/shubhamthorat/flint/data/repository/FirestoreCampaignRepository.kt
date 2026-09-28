package com.shubhamthorat.flint.data.repository

import com.shubhamthorat.flint.core.FlintLogger
import com.shubhamthorat.flint.domain.model.AppError
import com.shubhamthorat.flint.domain.model.Campaign
import com.shubhamthorat.flint.domain.model.FlintResult
import com.shubhamthorat.flint.domain.repository.AuthRepository
import com.shubhamthorat.flint.domain.repository.CampaignRepository
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalCoroutinesApi::class)
class FirestoreCampaignRepository(
    private val authRepository: AuthRepository = FirebaseAuthRepository()
) : CampaignRepository {

    private val tag = "FirestoreCampaignRepository"
    private val firestore = Firebase.firestore

    private suspend fun getUserCampaignsCollection(): dev.gitlive.firebase.firestore.CollectionReference? {
        val uid = authRepository.getCurrentUser()?.id
        return if (!uid.isNullOrBlank()) {
            firestore.collection("users").document(uid).collection("campaigns")
        } else {
            null
        }
    }

    override fun observeCampaigns(): Flow<List<Campaign>> {
        return authRepository.currentUserFlow.flatMapLatest { user ->
            if (user == null) {
                flowOf(emptyList())
            } else {
                firestore.collection("users").document(user.id).collection("campaigns")
                    .snapshots
                    .map { querySnapshot ->
                        querySnapshot.documents.mapNotNull { doc ->
                            try {
                                doc.data<Campaign>()
                            } catch (e: Exception) {
                                FlintLogger.w(tag, "Failed to parse campaign doc ${doc.id}: ${e.message}")
                                null
                            }
                        }
                    }
            }
        }
    }

    override suspend fun saveCampaign(campaign: Campaign): FlintResult<Campaign, AppError> {
        val col = getUserCampaignsCollection()
            ?: return FlintResult.Error(AppError.Auth("User must be signed in to save campaigns."))

        return try {
            col.document(campaign.id).set(campaign)
            FlintLogger.i(tag, "Successfully saved campaign ID ${campaign.id} to Firestore")
            FlintResult.Success(campaign)
        } catch (e: Exception) {
            FlintLogger.e(tag, "Failed to save campaign ID ${campaign.id}: ${e.message}")
            FlintResult.Error(AppError.Storage("Firestore error: ${e.message}"))
        }
    }

    override suspend fun deleteCampaign(id: String): FlintResult<Unit, AppError> {
        val col = getUserCampaignsCollection()
            ?: return FlintResult.Error(AppError.Auth("User must be signed in to delete campaigns."))

        return try {
            col.document(id).delete()
            FlintLogger.i(tag, "Successfully deleted campaign ID $id from Firestore")
            FlintResult.Success(Unit)
        } catch (e: Exception) {
            FlintLogger.e(tag, "Failed to delete campaign ID $id: ${e.message}")
            FlintResult.Error(AppError.Storage("Firestore error: ${e.message}"))
        }
    }
}
