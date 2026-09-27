package com.shubhamthorat.flint.data.repository

import com.shubhamthorat.flint.core.FlintLogger
import com.shubhamthorat.flint.domain.model.AppError
import com.shubhamthorat.flint.domain.model.FlintResult
import com.shubhamthorat.flint.domain.repository.ContentAsset
import com.shubhamthorat.flint.domain.repository.ContentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class InMemoryContentRepository : ContentRepository {

    private val tag = "InMemoryContentRepository"
    private val assetsFlow = MutableStateFlow<List<ContentAsset>>(emptyList())

    override fun observeContentAssets(): Flow<List<ContentAsset>> = assetsFlow.asStateFlow()

    override suspend fun getContentById(id: String): FlintResult<ContentAsset, AppError> {
        val asset = assetsFlow.value.find { it.id == id }
        return if (asset != null) {
            FlintLogger.d(tag, "getContentById: Found asset ID $id (${asset.title})")
            FlintResult.Success(asset)
        } else {
            FlintLogger.w(tag, "getContentById: Asset ID $id not found")
            FlintResult.Error(AppError.Validation("Content asset with id $id not found"))
        }
    }

    override suspend fun saveContent(asset: ContentAsset): FlintResult<ContentAsset, AppError> {
        val current = assetsFlow.value.toMutableList()
        val existingIndex = current.indexOfFirst { it.id == asset.id }
        if (existingIndex >= 0) {
            current[existingIndex] = asset
            FlintLogger.i(tag, "Updated existing content asset ID ${asset.id} (${asset.title})")
        } else {
            current.add(0, asset)
            FlintLogger.i(tag, "Saved new content asset ID ${asset.id} (${asset.title}) | Total assets: ${current.size}")
        }
        assetsFlow.value = current
        return FlintResult.Success(asset)
    }

    override suspend fun deleteContent(id: String): FlintResult<Unit, AppError> {
        val current = assetsFlow.value.toMutableList()
        val removed = current.removeAll { it.id == id }
        assetsFlow.value = current
        FlintLogger.i(tag, "Deleted content asset ID $id | Success: $removed | Remaining: ${current.size}")
        return FlintResult.Success(Unit)
    }
}
