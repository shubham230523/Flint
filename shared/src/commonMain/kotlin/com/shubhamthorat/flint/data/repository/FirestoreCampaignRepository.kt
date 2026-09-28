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
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalCoroutinesApi::class)
class FirestoreCampaignRepository(
    private val authRepository: AuthRepository = FirebaseAuthRepository(),
    private val restApi: FirestoreRestApi = FirestoreRestApi()
) : CampaignRepository {

    private val tag = "FirestoreCampaignRepository"

    override fun observeCampaigns(): Flow<List<Campaign>> {
        return authRepository.currentUserFlow.flatMapLatest { user ->
            if (user == null) {
                flowOf(emptyList())
            } else {
                flow {
                    try {
                        Firebase.firestore.collection("users").document(user.id).collection("campaigns")
                            .snapshots
                            .map { querySnapshot ->
                                querySnapshot.documents.mapNotNull { doc ->
                                    try { doc.data<Campaign>() } catch (_: Exception) { null }
                                }
                            }.collect { emit(it) }
                    } catch (e: Throwable) {
                        FlintLogger.w(tag, "Native Firestore observe failed (${e.message}). Falling back to REST API...")
                        val idToken = authRepository.getIdToken()
                        val campaigns = restApi.fetchCampaigns(user.id, idToken)
                        emit(campaigns)
                    }
                }
            }
        }
    }

    override suspend fun saveCampaign(campaign: Campaign): FlintResult<Campaign, AppError> {
        val user = authRepository.getCurrentUser()
            ?: return FlintResult.Error(AppError.Auth("User must be signed in to save campaigns."))
        val idToken = authRepository.getIdToken()

        try {
            val col = Firebase.firestore.collection("users").document(user.id).collection("campaigns")
            col.document(campaign.id).set(campaign)
            FlintLogger.i(tag, "Successfully saved campaign ID ${campaign.id} to Firestore natively")
            return FlintResult.Success(campaign)
        } catch (e: Throwable) {
            FlintLogger.w(tag, "Native Firestore save failed (${e.message}). Falling back to Firestore REST API...")
        }

        return restApi.saveCampaign(user.id, campaign, idToken)
    }

    override suspend fun deleteCampaign(id: String): FlintResult<Unit, AppError> {
        val user = authRepository.getCurrentUser()
            ?: return FlintResult.Error(AppError.Auth("User must be signed in to delete campaigns."))

        try {
            val col = Firebase.firestore.collection("users").document(user.id).collection("campaigns")
            col.document(id).delete()
            return FlintResult.Success(Unit)
        } catch (_: Throwable) {
            return FlintResult.Success(Unit)
        }
    }
}
