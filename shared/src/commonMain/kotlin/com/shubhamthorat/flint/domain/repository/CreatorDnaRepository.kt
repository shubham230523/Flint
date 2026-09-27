package com.shubhamthorat.flint.domain.repository

import com.shubhamthorat.flint.domain.model.AppError
import com.shubhamthorat.flint.domain.model.CreatorProfile
import com.shubhamthorat.flint.domain.model.FlintResult
import kotlinx.coroutines.flow.Flow

interface CreatorDnaRepository {
    fun observeProfile(): Flow<CreatorProfile?>
    suspend fun getProfile(): FlintResult<CreatorProfile, AppError>
    suspend fun updateProfile(profile: CreatorProfile): FlintResult<CreatorProfile, AppError>
}
