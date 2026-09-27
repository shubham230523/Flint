package com.shubhamthorat.flint.domain.repository

import com.shubhamthorat.flint.domain.model.AppError
import com.shubhamthorat.flint.domain.model.Campaign
import com.shubhamthorat.flint.domain.model.FlintResult
import kotlinx.coroutines.flow.Flow

interface CampaignRepository {
    fun observeCampaigns(): Flow<List<Campaign>>
    suspend fun saveCampaign(campaign: Campaign): FlintResult<Campaign, AppError>
    suspend fun deleteCampaign(id: String): FlintResult<Unit, AppError>
}
