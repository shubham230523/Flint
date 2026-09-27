package com.shubhamthorat.flint.domain.ai

import com.shubhamthorat.flint.domain.model.AppError
import com.shubhamthorat.flint.domain.model.FlintResult
import com.shubhamthorat.flint.domain.repository.AiRequest
import com.shubhamthorat.flint.domain.repository.AiResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class OllamaCloudProvider(
    private val endpointUrl: String? = null
) : AiProvider {

    override val providerName: String = "OllamaCloud"

    override suspend fun generate(request: AiRequest): FlintResult<AiResponse, AppError> {
        if (endpointUrl.isNullOrBlank()) {
            return FlintResult.Error(
                AppError.AiProvider("OllamaCloud endpoint URL is not configured.")
            )
        }

        return FlintResult.Error(
            AppError.AiProvider("OllamaCloud provider endpoint is not available.")
        )
    }

    override fun generateStream(request: AiRequest): Flow<FlintResult<String, AppError>> = flow {
        emit(FlintResult.Error(AppError.AiProvider("OllamaCloud provider is not configured.")))
    }

    override suspend fun isHealthy(): Boolean {
        return !endpointUrl.isNullOrBlank()
    }
}
