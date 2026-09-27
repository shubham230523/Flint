package com.shubhamthorat.flint.domain

import com.shubhamthorat.flint.domain.ai.AiTaskRouter
import com.shubhamthorat.flint.domain.ai.FakeAiProvider
import com.shubhamthorat.flint.domain.model.AppError
import com.shubhamthorat.flint.domain.model.FlintResult
import com.shubhamthorat.flint.domain.model.getOrThrow
import com.shubhamthorat.flint.domain.repository.AiRequest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AiEngineTest {

    @Test
    fun testPrimaryProviderSuccess() = runTest {
        val openRouter = FakeAiProvider("OpenRouter", shouldSucceed = true)
        val gemini = FakeAiProvider("Gemini", shouldSucceed = true)
        val router = AiTaskRouter(providers = listOf(openRouter, gemini))

        val request = AiRequest(prompt = "Generate YouTube hook")
        val result = router.generateContent(request)

        assertTrue(result is FlintResult.Success)
        val response = result.getOrThrow()
        assertEquals("OpenRouter", response.providerUsed)
        assertEquals("Generated response from OpenRouter for prompt: Generate YouTube hook", response.content)
    }

    @Test
    fun testOpenRouterStreamingDefault() = runTest {
        val openRouter = FakeAiProvider("OpenRouter", shouldSucceed = true)
        val gemini = FakeAiProvider("Gemini", shouldSucceed = true)
        val router = AiTaskRouter(providers = listOf(openRouter, gemini))

        val request = AiRequest(prompt = "Stream OpenRouter prompt")
        val streamResult = router.generateContentStream(request).first()

        assertTrue(streamResult is FlintResult.Success)
        assertEquals("Generated response from OpenRouter for prompt: Stream OpenRouter prompt", streamResult.getOrThrow())
    }

    @Test
    fun testProviderFallbackWhenPrimaryFails() = runTest {
        val failingOpenRouter = FakeAiProvider("OpenRouter", shouldSucceed = false)
        val workingGemini = FakeAiProvider("Gemini", shouldSucceed = true)
        val router = AiTaskRouter(providers = listOf(failingOpenRouter, workingGemini))

        val request = AiRequest(prompt = "Generate LinkedIn post", providerPreference = "Gemini")
        val result = router.generateContent(request)

        assertTrue(result is FlintResult.Success)
        val response = result.getOrThrow()
        assertEquals("Gemini", response.providerUsed)
        assertEquals("Generated response from Gemini for prompt: Generate LinkedIn post", response.content)
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
