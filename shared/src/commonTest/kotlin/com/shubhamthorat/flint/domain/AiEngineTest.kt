package com.shubhamthorat.flint.domain

import com.shubhamthorat.flint.domain.ai.AiTaskRouter
import com.shubhamthorat.flint.domain.ai.FakeAiProvider
import com.shubhamthorat.flint.domain.model.AppError
import com.shubhamthorat.flint.domain.model.FlintResult
import com.shubhamthorat.flint.domain.model.getOrThrow
import com.shubhamthorat.flint.domain.repository.AiRequest
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AiEngineTest {

    @Test
    fun testPrimaryProviderSuccess() = runTest {
        val gemini = FakeAiProvider("Gemini", shouldSucceed = true)
        val openRouter = FakeAiProvider("OpenRouter", shouldSucceed = true)
        val router = AiTaskRouter(providers = listOf(gemini, openRouter))

        val request = AiRequest(prompt = "Generate YouTube hook")
        val result = router.generateContent(request)

        assertTrue(result is FlintResult.Success)
        val response = result.getOrThrow()
        assertEquals("Gemini", response.providerUsed)
        assertEquals("Generated response from Gemini for prompt: Generate YouTube hook", response.content)
    }

    @Test
    fun testProviderFallbackWhenPrimaryFails() = runTest {
        val failingGemini = FakeAiProvider("Gemini", shouldSucceed = false)
        val workingOpenRouter = FakeAiProvider("OpenRouter", shouldSucceed = true)
        val router = AiTaskRouter(providers = listOf(failingGemini, workingOpenRouter))

        val request = AiRequest(prompt = "Generate LinkedIn post")
        val result = router.generateContent(request)

        assertTrue(result is FlintResult.Success)
        val response = result.getOrThrow()
        assertEquals("OpenRouter", response.providerUsed)
        assertEquals("Generated response from OpenRouter for prompt: Generate LinkedIn post", response.content)
    }

    @Test
    fun testAllProvidersFailingReturnsError() = runTest {
        val failingGemini = FakeAiProvider("Gemini", shouldSucceed = false)
        val failingOpenRouter = FakeAiProvider("OpenRouter", shouldSucceed = false)
        val router = AiTaskRouter(providers = listOf(failingGemini, failingOpenRouter))

        val request = AiRequest(prompt = "Generate newsletter")
        val result = router.generateContent(request)

        assertTrue(result is FlintResult.Error)
        val error = result.error
        assertTrue(error is AppError.AiProvider)
    }
}