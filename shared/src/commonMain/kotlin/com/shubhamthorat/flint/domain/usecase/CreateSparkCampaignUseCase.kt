package com.shubhamthorat.flint.domain.usecase

import com.shubhamthorat.flint.core.FlintLogger
import com.shubhamthorat.flint.domain.model.AppError
import com.shubhamthorat.flint.domain.model.Campaign
import com.shubhamthorat.flint.domain.model.CreatorDNA
import com.shubhamthorat.flint.domain.model.FlintResult
import com.shubhamthorat.flint.domain.repository.ContentAsset
import com.shubhamthorat.flint.domain.repository.ContentType
import kotlin.random.Random

class CreateSparkCampaignUseCase(
    private val generateContentUseCase: GenerateContentUseCase
) {
    suspend fun execute(
        ideaOrSource: String,
        targetTypes: List<ContentType>,
        creatorDna: CreatorDNA = CreatorDNA()
    ): FlintResult<Campaign, AppError> {
        val tag = "CreateSparkCampaignUseCase"
        FlintLogger.i(tag, "Executing campaign creation for source: \"$ideaOrSource\" across ${targetTypes.size} channels: ${targetTypes.map { it.name }}")

        if (targetTypes.isEmpty()) {
            FlintLogger.e(tag, "Validation failed: Campaign requires at least one target content type")
            return FlintResult.Error(
                AppError.Validation("Campaign requires at least one target content type")
            )
        }

        val items = mutableListOf<ContentAsset>()

        for (type in targetTypes) {
            FlintLogger.d(tag, "Generating asset for channel: ${type.name}")
            when (val result = generateContentUseCase.execute(ideaOrSource, type, creatorDna)) {
                is FlintResult.Success -> items.add(result.data)
                is FlintResult.Error -> {
                    FlintLogger.e(tag, "Failed to generate asset for type ${type.name}: ${result.error.message}")
                    return result
                }
            }
        }

        val randomSuffix = Random.nextInt(100000, 999999)
        val shortTitle = if (ideaOrSource.length > 40) "${ideaOrSource.take(37)}..." else ideaOrSource
        val campaign = Campaign(
            id = "campaign_$randomSuffix",
            title = "Campaign: $shortTitle",
            ideaOrSource = ideaOrSource,
            items = items
        )

        FlintLogger.i(tag, "Campaign successfully assembled with ${campaign.items.size} channel assets | ID: ${campaign.id}")
        return FlintResult.Success(campaign)
    }
}
