package com.shubhamthorat.flint.data.repository

import com.shubhamthorat.flint.domain.model.AppError
import com.shubhamthorat.flint.domain.model.CreatorProfile
import com.shubhamthorat.flint.domain.model.FlintResult
import com.shubhamthorat.flint.domain.repository.CreatorDnaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class InMemoryCreatorDnaRepository : CreatorDnaRepository {

    private val profileFlow = MutableStateFlow<CreatorProfile?>(
        CreatorProfile(userId = "user_default", handle = "@creator")
    )

    override fun observeProfile(): Flow<CreatorProfile?> = profileFlow.asStateFlow()

    override suspend fun getProfile(): FlintResult<CreatorProfile, AppError> {
        val current = profileFlow.value
        return if (current != null) {
            FlintResult.Success(current)
        } else {
            FlintResult.Error(AppError.Validation("Creator profile not found"))
        }
    }

    override suspend fun updateProfile(profile: CreatorProfile): FlintResult<CreatorProfile, AppError> {
        profileFlow.value = profile
        return FlintResult.Success(profile)
    }
}
