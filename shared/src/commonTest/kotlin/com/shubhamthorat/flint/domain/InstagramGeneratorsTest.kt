package com.shubhamthorat.flint.domain

import com.shubhamthorat.flint.domain.ai.AiTaskRouter
import com.shubhamthorat.flint.domain.ai.FakeAiProvider
import com.shubhamthorat.flint.domain.model.CreatorDNA
import com.shubhamthorat.flint.domain.model.FlintResult
import com.shubhamthorat.flint.domain.model.InstagramCaption
import com.shubhamthorat.flint.domain.model.InstagramCarousel
import com.shubhamthorat.flint.domain.model.InstagramContentOpportunity
import com.shubhamthorat.flint.domain.model.InstagramQuotePost
import com.shubhamthorat.flint.domain.model.InstagramReelContent
import com.shubhamthorat.flint.domain.model.InstagramStorySequence
import com.shubhamthorat.flint.domain.model.OpportunityType
import com.shubhamthorat.flint.domain.usecase.GenerateInstagramCaptionUseCase
import com.shubhamthorat.flint.domain.usecase.GenerateInstagramCarouselUseCase
import com.shubhamthorat.flint.domain.usecase.GenerateInstagramQuotePostsUseCase
import com.shubhamthorat.flint.domain.usecase.GenerateInstagramReelUseCase
import com.shubhamthorat.flint.domain.usecase.GenerateInstagramStoriesUseCase
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class InstagramGeneratorsTest {

    private val sampleOpp = InstagramContentOpportunity(
        id = "opp_1",
        type = OpportunityType.REEL_IDEA,
        title = "AI Gateway Architecture",
        description = "Reel script about KMP fallback strategy",
        sourceReference = "02:15 transcript",
        suggestedHook = "Never trust a single AI provider."
    )

    @Test
    fun testGenerateInstagramReelUseCaseSuccess() = runTest {
        val json = """
            {
              "hook": "Never trust a single AI provider.",
              "body": "When Gemini rate limits you, OpenRouter steps in instantly.",
              "ending": "That is how Flint stays reliable.",
              "CTA": "Comment FLINT to get the architecture guide.",
              "suggestedDuration": "30s"
            }
        """.trimIndent()

        val provider = FakeAiProvider(fixedResponseText = json)
        val aiRepo = AiTaskRouter(listOf(provider))
        val useCase = GenerateInstagramReelUseCase(aiRepo)

        val result = useCase.execute(sampleOpp, CreatorDNA(preferredTone = "Punchy"))
        assertTrue(result is FlintResult.Success<*>)

        val reel = (result as FlintResult.Success<*>).data as InstagramReelContent
        assertEquals("Never trust a single AI provider.", reel.hook)
        assertEquals("30s", reel.suggestedDuration)
    }

    @Test
    fun testGenerateInstagramCarouselUseCaseSuccess() = runTest {
        val json = """
            {
              "title": "5 Rules of Flint AI Gateway",
              "slides": [
                { "slideNumber": 1, "headline": "Rule 1", "body": "Abstract AI calls" },
                { "slideNumber": 2, "headline": "Rule 2", "body": "Enforce fallbacks" }
              ]
            }
        """.trimIndent()

        val provider = FakeAiProvider(fixedResponseText = json)
        val aiRepo = AiTaskRouter(listOf(provider))
        val useCase = GenerateInstagramCarouselUseCase(aiRepo)

        val result = useCase.execute(sampleOpp)
        assertTrue(result is FlintResult.Success<*>)

        val carousel = (result as FlintResult.Success<*>).data as InstagramCarousel
        assertEquals("5 Rules of Flint AI Gateway", carousel.title)
        assertEquals(2, carousel.slides.size)
    }

    @Test
    fun testGenerateInstagramStoriesUseCaseSuccess() = runTest {
        val json = """
            {
              "title": "Behind the Scenes Story Sequence",
              "stories": [
                { "sequenceNumber": 1, "headline": "Frame 1", "body": "How we process YouTube videos", "interactionSuggestion": "Poll", "CTA": "Vote below" }
              ]
            }
        """.trimIndent()

        val provider = FakeAiProvider(fixedResponseText = json)
        val aiRepo = AiTaskRouter(listOf(provider))
        val useCase = GenerateInstagramStoriesUseCase(aiRepo)

        val result = useCase.execute(sampleOpp)
        assertTrue(result is FlintResult.Success<*>)

        val sequence = (result as FlintResult.Success<*>).data as InstagramStorySequence
        assertEquals(1, sequence.stories.size)
        assertEquals("Frame 1", sequence.stories[0].headline)
    }

    @Test
    fun testGenerateInstagramQuotePostsUseCaseSuccess() = runTest {
        val json = """
            {
              "quote": "One spark. Endless stories.",
              "context": "Said during Flint launch announcement",
              "caption": "Why single-purpose tools are obsolete.",
              "CTA": "Save this post for later."
            }
        """.trimIndent()

        val provider = FakeAiProvider(fixedResponseText = json)
        val aiRepo = AiTaskRouter(listOf(provider))
        val useCase = GenerateInstagramQuotePostsUseCase(aiRepo)

        val result = useCase.execute(sampleOpp)
        assertTrue(result is FlintResult.Success<*>)

        val quote = (result as FlintResult.Success<*>).data as InstagramQuotePost
        assertEquals("One spark. Endless stories.", quote.quote)
    }

    @Test
    fun testGenerateInstagramCaptionUseCaseSuccess() = runTest {
        val json = """
            {
              "caption": "Transform your YouTube videos into 10 Instagram posts in seconds with Flint.",
              "CTA": "Link in bio to try Flint free.",
              "hashtags": ["#KotlinMultiplatform", "#JetpackCompose", "#AIContent"]
            }
        """.trimIndent()

        val provider = FakeAiProvider(fixedResponseText = json)
        val aiRepo = AiTaskRouter(listOf(provider))
        val useCase = GenerateInstagramCaptionUseCase(aiRepo)

        val result = useCase.execute("Reel", sampleOpp)
        assertTrue(result is FlintResult.Success<*>)

        val caption = (result as FlintResult.Success<*>).data as InstagramCaption
        assertNotNull(caption)
        assertEquals(3, caption.hashtags.size)
        assertTrue(caption.caption.contains("Flint"))
    }
}
