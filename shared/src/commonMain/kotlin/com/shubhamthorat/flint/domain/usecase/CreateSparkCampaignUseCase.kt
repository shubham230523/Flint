package com.shubhamthorat.flint.domain.usecase

import com.shubhamthorat.flint.domain.model.AppError
import com.shubhamthorat.flint.domain.model.Campaign
import com.shubhamthorat.flint.domain.model.CreatorDNA
import com.shubhamthorat.flint.domain.model.FlintResult
import com.shubhamthorat.flint.domain.repository.ContentAsset
import com.shubhamthorat.flint.domain.repository.ContentType

class CreateSparkCampaignUseCase(
    private val generateContentUseCase: GenerateContentUseCase
) {
    suspend fun execute(
        ideaOrSource: String,
        targetTypes: List<ContentType>,
        creatorDna: CreatorDNA = CreatorDNA()
    ): FlintResult<Campaign, AppError> {
        if (targetTypes.isEmpty()) {
            return FlintResult.Error(
                AppError.Validation("Campaign requires at least one target content type")
            )
        }

        val items = mutableListOf<ContentAsset>()

        for (type in targetTypes) {
            when (val result = generateContentUseCase.execute(ideaOrSource, type, creatorDna)) {
                is FlintResult.Success -> items.add(result.data)
                is FlintResult.Error -> return result
            }
        }

        val campaign = Campaign(
            id = "campaign_${items.size}_assets",
            title = "Campaign: $ideaOrSource",
            ideaOrSource = ideaOrSource,
            items = items
        )

        return FlintResult.Success(campaign)
    }
}
