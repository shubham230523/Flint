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
import kotlinx.coroutines.flow.MutableStateFlow
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
    private val cachedCampaignsFlow = MutableStateFlow<List<Campaign>>(emptyList())

    override fun observeCampaigns(): Flow<List<Campaign>> {
        return authRepository.currentUserFlow.flatMapLatest { user ->
            if (user == null) {
                cachedCampaignsFlow.value = emptyList()
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
                            }.collect {
                                cachedCampaignsFlow.value = it
                                emit(it)
                            }
                    } catch (e: Throwable) {
                        FlintLogger.w(tag, "Native Firestore observe failed (${e.message}). Fetching from Firestore REST API...")
                        refreshCampaignsFromRest(user.id)
                        cachedCampaignsFlow.collect { emit(it) }
                    }
                }
            }
        }
    }

    private suspend fun refreshCampaignsFromRest(userId: String) {
        val idToken = authRepository.getIdToken()
        val campaigns = restApi.fetchCampaigns(userId, idToken)
        cachedCampaignsFlow.value = campaigns
        FlintLogger.i(tag, "Fetched ${campaigns.size} campaigns from Firestore REST API")
    }

    override suspend fun saveCampaign(campaign: Campaign): FlintResult<Campaign, AppError> {
        val user = authRepository.getCurrentUser()
            ?: return FlintResult.Error(AppError.Auth("User must be signed in to save campaigns."))
        val idToken = authRepository.getIdToken()

        try {
            val col = Firebase.firestore.collection("users").document(user.id).collection("campaigns")
            col.document(campaign.id).set(campaign)
            FlintLogger.i(tag, "Successfully saved campaign ID ${campaign.id} to Firestore natively")
            refreshCampaignsFromRest(user.id)
            return FlintResult.Success(campaign)
        } catch (e: Throwable) {
            FlintLogger.w(tag, "Native Firestore save failed (${e.message}). Falling back to Firestore REST API...")
        }

        val result = restApi.saveCampaign(user.id, campaign, idToken)
        if (result is FlintResult.Success) {
            refreshCampaignsFromRest(user.id)
        }
        return result
    }

    override suspend fun deleteCampaign(id: String): FlintResult<Unit, AppError> {
        val user = authRepository.getCurrentUser()
            ?: return FlintResult.Error(AppError.Auth("User must be signed in to delete campaigns."))

        try {
            val col = Firebase.firestore.collection("users").document(user.id).collection("campaigns")
            col.document(id).delete()
            refreshCampaignsFromRest(user.id)
            return FlintResult.Success(Unit)
        } catch (_: Throwable) {
            refreshCampaignsFromRest(user.id)
            return FlintResult.Success(Unit)
        }
    }
}
