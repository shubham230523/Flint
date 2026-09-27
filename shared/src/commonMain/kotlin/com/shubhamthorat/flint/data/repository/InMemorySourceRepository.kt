package com.shubhamthorat.flint.data.repository

import com.shubhamthorat.flint.domain.model.AppError
import com.shubhamthorat.flint.domain.model.FlintResult
import com.shubhamthorat.flint.domain.repository.SourceItem
import com.shubhamthorat.flint.domain.repository.SourceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class InMemorySourceRepository : SourceRepository {

    private val sourcesFlow = MutableStateFlow<List<SourceItem>>(emptyList())

    override fun observeSources(): Flow<List<SourceItem>> = sourcesFlow.asStateFlow()

    override suspend fun getSourceById(id: String): FlintResult<SourceItem, AppError> {
        val item = sourcesFlow.value.find { it.id == id }
        return if (item != null) {
            FlintResult.Success(item)
        } else {
            FlintResult.Error(AppError.Validation("Source with id $id not found"))
        }
    }

    override suspend fun addSource(source: SourceItem): FlintResult<SourceItem, AppError> {
        val current = sourcesFlow.value.toMutableList()
        current.add(0, source)
        sourcesFlow.value = current
        return FlintResult.Success(source)
    }

    override suspend fun deleteSource(id: String): FlintResult<Unit, AppError> {
        val current = sourcesFlow.value.toMutableList()
        current.removeAll { it.id == id }
        sourcesFlow.value = current
        return FlintResult.Success(Unit)
    }
}
