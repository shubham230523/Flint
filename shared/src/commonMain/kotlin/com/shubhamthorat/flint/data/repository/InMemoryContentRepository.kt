package com.shubhamthorat.flint.data.repository

import com.shubhamthorat.flint.domain.model.AppError
import com.shubhamthorat.flint.domain.model.FlintResult
import com.shubhamthorat.flint.domain.repository.ContentAsset
import com.shubhamthorat.flint.domain.repository.ContentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class InMemoryContentRepository : ContentRepository {

    private val assetsFlow = MutableStateFlow<List<ContentAsset>>(emptyList())

    override fun observeContentAssets(): Flow<List<ContentAsset>> = assetsFlow.asStateFlow()

    override suspend fun getContentById(id: String): FlintResult<ContentAsset, AppError> {
        val asset = assetsFlow.value.find { it.id == id }
        return if (asset != null) {
            FlintResult.Success(asset)
        } else {
            FlintResult.Error(AppError.Validation("Content asset with id $id not found"))
        }
    }

    override suspend fun saveContent(asset: ContentAsset): FlintResult<ContentAsset, AppError> {
        val current = assetsFlow.value.toMutableList()
        val existingIndex = current.indexOfFirst { it.id == asset.id }
        if (existingIndex >= 0) {
            current[existingIndex] = asset
        } else {
            current.add(0, asset)
        }
        assetsFlow.value = current
        return FlintResult.Success(asset)
    }

    override suspend fun deleteContent(id: String): FlintResult<Unit, AppError> {
        val current = assetsFlow.value.toMutableList()
        current.removeAll { it.id == id }
        assetsFlow.value = current
        return FlintResult.Success(Unit)
    }
}
