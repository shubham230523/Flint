package com.shubhamthorat.flint.data.repository

import com.shubhamthorat.flint.domain.model.AppError
import com.shubhamthorat.flint.domain.model.Campaign
import com.shubhamthorat.flint.domain.model.FlintResult
import com.shubhamthorat.flint.domain.repository.CampaignRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class InMemoryCampaignRepository : CampaignRepository {

    private val campaignsFlow = MutableStateFlow<List<Campaign>>(emptyList())

    override fun observeCampaigns(): Flow<List<Campaign>> = campaignsFlow.asStateFlow()

    override suspend fun saveCampaign(campaign: Campaign): FlintResult<Campaign, AppError> {
        val current = campaignsFlow.value.toMutableList()
        val existingIndex = current.indexOfFirst { it.id == campaign.id }
        if (existingIndex >= 0) {
            current[existingIndex] = campaign
        } else {
            current.add(0, campaign)
        }
        campaignsFlow.value = current
        return FlintResult.Success(campaign)
    }

    override suspend fun deleteCampaign(id: String): FlintResult<Unit, AppError> {
        val current = campaignsFlow.value.toMutableList()
        current.removeAll { it.id == id }
        campaignsFlow.value = current
        return FlintResult.Success(Unit)
    }
}
