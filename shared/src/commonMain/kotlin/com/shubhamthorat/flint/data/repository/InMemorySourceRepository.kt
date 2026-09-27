package com.shubhamthorat.flint.data.repository

import com.shubhamthorat.flint.core.FlintLogger
import com.shubhamthorat.flint.domain.model.AppError
import com.shubhamthorat.flint.domain.model.FlintResult
import com.shubhamthorat.flint.domain.repository.SourceItem
import com.shubhamthorat.flint.domain.repository.SourceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class InMemorySourceRepository : SourceRepository {

    private val tag = "InMemorySourceRepository"
    private val sourcesFlow = MutableStateFlow<List<SourceItem>>(emptyList())

    override fun observeSources(): Flow<List<SourceItem>> = sourcesFlow.asStateFlow()

    override suspend fun getSourceById(id: String): FlintResult<SourceItem, AppError> {
        val item = sourcesFlow.value.find { it.id == id }
        return if (item != null) {
            FlintLogger.d(tag, "getSourceById: Found source ID $id (${item.title})")
            FlintResult.Success(item)
        } else {
            FlintLogger.w(tag, "getSourceById: Source ID $id not found")
            FlintResult.Error(AppError.Validation("Source with id $id not found"))
        }
    }

    override suspend fun addSource(source: SourceItem): FlintResult<SourceItem, AppError> {
        val current = sourcesFlow.value.toMutableList()
        current.add(0, source)
        sourcesFlow.value = current
        FlintLogger.i(tag, "Added source item ID ${source.id} (${source.title}) | Type: ${source.type.name} | Total sources: ${current.size}")
        return FlintResult.Success(source)
    }

    override suspend fun deleteSource(id: String): FlintResult<Unit, AppError> {
        val current = sourcesFlow.value.toMutableList()
        val removed = current.removeAll { it.id == id }
        sourcesFlow.value = current
        FlintLogger.i(tag, "Deleted source ID $id | Success: $removed | Remaining: ${current.size}")
        return FlintResult.Success(Unit)
    }
}
