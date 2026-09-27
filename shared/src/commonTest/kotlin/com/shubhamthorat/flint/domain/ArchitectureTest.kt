package com.shubhamthorat.flint.domain

import com.shubhamthorat.flint.core.AppCoroutineDispatchers
import com.shubhamthorat.flint.domain.model.AppError
import com.shubhamthorat.flint.domain.model.FlintResult
import com.shubhamthorat.flint.domain.model.fold
import com.shubhamthorat.flint.domain.model.getOrNull
import com.shubhamthorat.flint.domain.model.getOrThrow
import com.shubhamthorat.flint.domain.model.map
import com.shubhamthorat.flint.domain.model.mapError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ArchitectureTest {

    private fun getSampleSuccess(): FlintResult<String, AppError> =
        FlintResult.Success("Flint Spark")

    private fun getSampleError(): FlintResult<String, AppError> =
        FlintResult.Error(AppError.Network("No internet"))

    @Test
    fun testFlintResultSuccessOperations() {
        val result = getSampleSuccess()

        val folded = result.fold(
            onSuccess = { "Success: $it" },
            onError = { "Error: ${it.message}" }
        )
        assertEquals("Success: Flint Spark", folded)

        assertEquals("Flint Spark", result.getOrNull())
        assertEquals("Flint Spark", result.getOrThrow())

        val mapped = result.map { it.uppercase() }
        assertEquals("FLINT SPARK", mapped.getOrNull())
    }

    @Test
    fun testFlintResultErrorOperations() {
        val result = getSampleError()

        val folded = result.fold(
            onSuccess = { "Success: $it" },
            onError = { "Error: ${it.message}" }
        )
        assertEquals("Error: No internet", folded)

        assertNull(result.getOrNull())

        val mappedError = result.mapError { AppError.Server("Server down: ${it.message}") }
        val mappedFolded = mappedError.fold(
            onSuccess = { "Success: $it" },
            onError = { it.message }
        )
        assertEquals("Server down: No internet", mappedFolded)
    }

    @Test
    fun testAppErrorTaxonomy() {
        val networkErr: AppError = AppError.Network("Connection timeout", code = 408)
        val authErr: AppError = AppError.Auth("Invalid credentials")
        val aiErr: AppError = AppError.AiProvider("Quota exceeded", provider = "Gemini")
        val quotaErr: AppError = AppError.QuotaExceeded("Monthly limit reached")

        assertEquals("Connection timeout", networkErr.message)
        assertEquals("Invalid credentials", authErr.message)
        assertEquals("Gemini", (aiErr as AppError.AiProvider).provider)
        assertEquals("Monthly limit reached", quotaErr.message)
    }

    @Test
    fun testAppCoroutineDispatchers() = runTest {
        val dispatchers = AppCoroutineDispatchers(
            main = Dispatchers.Unconfined,
            io = Dispatchers.Unconfined,
            default = Dispatchers.Unconfined,
            unconfined = Dispatchers.Unconfined
        )

        assertEquals(Dispatchers.Unconfined, dispatchers.main)
        assertEquals(Dispatchers.Unconfined, dispatchers.io)
        assertEquals(Dispatchers.Unconfined, dispatchers.default)
        assertEquals(Dispatchers.Unconfined, dispatchers.unconfined)
    }
}