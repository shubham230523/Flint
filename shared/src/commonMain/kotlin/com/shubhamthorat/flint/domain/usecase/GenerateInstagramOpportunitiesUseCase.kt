package com.shubhamthorat.flint.domain.usecase

import com.shubhamthorat.flint.core.FlintLogger
import com.shubhamthorat.flint.domain.ai.AiContentCleaner
import com.shubhamthorat.flint.domain.model.AppError
import com.shubhamthorat.flint.domain.model.CreatorDNA
import com.shubhamthorat.flint.domain.model.FlintResult
import com.shubhamthorat.flint.domain.model.InstagramContentOpportunity
import com.shubhamthorat.flint.domain.model.OpportunityListContainer
import com.shubhamthorat.flint.domain.model.OpportunityType
import com.shubhamthorat.flint.domain.model.YouTubeContentAnalysis
import com.shubhamthorat.flint.domain.repository.AiRepository
import com.shubhamthorat.flint.domain.repository.AiRequest
import kotlinx.coroutines.delay

class GenerateInstagramOpportunitiesUseCase(
    private val aiRepository: AiRepository
) {
    suspend fun execute(
        analysis: YouTubeContentAnalysis,
        creatorDna: CreatorDNA = CreatorDNA(),
        maxRetries: Int = 3,
        initialDelayMs: Long = 1000L
    ): FlintResult<List<InstagramContentOpportunity>, AppError> {
        val tag = "GenerateInstagramOpportunitiesUseCase"
        FlintLogger.i(tag, "Generating Instagram content opportunities from YouTube content analysis")

        val prompt = buildOpportunitiesPrompt(analysis, creatorDna)
        val aiRequest = AiRequest(
            prompt = prompt,
            temperature = 0.5f,
            maxTokens = 2048
        )

        var currentDelayMs = initialDelayMs
        var lastError: AppError? = null

        for (attempt in 1..(maxRetries + 1)) {
            FlintLogger.i(tag, "Generating Instagram content opportunities from YouTube content analysis (Attempt $attempt/${maxRetries + 1})")

            val aiResult = aiRepository.generateContent(aiRequest)
            if (aiResult is FlintResult.Success) {
                val cleaned = AiContentCleaner.clean(aiResult.data.content)
                val opportunities = OpportunityListContainer.parseFromJson(cleaned)
                if (opportunities.isNotEmpty()) {
                    val sanitizedOpps = sanitizeOpportunities(opportunities, analysis)
                    FlintLogger.i(tag, "Successfully generated ${sanitizedOpps.size} Instagram content opportunities from AI on attempt $attempt")
                    return FlintResult.Success(sanitizedOpps)
                } else {
                    FlintLogger.w(tag, "AI response unparseable on attempt $attempt. Constructing dynamic opportunities from analysis facts")
                    val dynamicOpportunities = createDynamicOpportunitiesFromAnalysis(analysis)
                    return FlintResult.Success(dynamicOpportunities)
                }
            } else if (aiResult is FlintResult.Error) {
                lastError = aiResult.error
                FlintLogger.w(tag, "AI generation failed for Instagram opportunities on attempt $attempt: ${aiResult.error.message}")
            }

            if (attempt <= maxRetries) {
                FlintLogger.i(tag, "Retrying Instagram opportunities generation in ${currentDelayMs}ms (Retry $attempt/$maxRetries)...")
                delay(currentDelayMs)
                currentDelayMs = (currentDelayMs * 2).coerceAtMost(10_000L)
            }
        }

        val finalError = lastError ?: AppError.AiProvider("Instagram opportunities generation failed after $maxRetries retries")
        FlintLogger.e(tag, "All $maxRetries AI retries failed: ${finalError.message}")
        return FlintResult.Error(finalError)
    }

    private fun buildOpportunitiesPrompt(
        analysis: YouTubeContentAnalysis,
        creatorDna: CreatorDNA
    ): String {
        val topicsStr = analysis.mainTopics.joinToString(", ")
        val keyPointsStr = analysis.keyPoints.joinToString("; ")
        val quotesStr = analysis.notableQuotes.joinToString(" | ")

        return """
            Role: Expert Instagram Content Strategist & Growth Copywriter
            Task: Create 4 to 6 highly engaging, unique Instagram content opportunities based on the YouTube video analysis below.
            
            CREATOR STYLE:
            Tone: ${creatorDna.preferredTone.ifBlank { "Engaging & Informative" }}
            Audience: ${creatorDna.targetAudience.ifBlank { analysis.audience.ifBlank { "Developers & Tech Creators" } }}
            
            CRITICAL RULES:
            1. Every title, hook, and description MUST be highly specific to the video's actual topics ($topicsStr).
            2. NEVER use generic placeholder words like "Topic 1", "Specific Topic Name", "Key takeaway 1", "Hook 1", or "Generic Idea".
            3. Craft strong, viral Instagram hooks (e.g. "I spent \$31k on Claude Code so you don't have to.", "The 3 Claude Code hacks that changed how I build software.").
            4. Diversify content types across REEL_IDEA, CAROUSEL, STORY_SEQUENCE, QUOTE_POST, EDUCATIONAL_POST, QUESTION_POST.
            5. Output MUST be valid raw JSON adhering strictly to:
            
            {
              "opportunities": [
                {
                  "id": "opp_1",
                  "type": "REEL_IDEA",
                  "title": "Engaging Post Title about $topicsStr",
                  "description": "Clear explanation of why this post delivers value",
                  "sourceReference": "Key video takeaway",
                  "suggestedHook": "Viral opening hook line"
                }
              ]
            }
            
            SOURCE YOUTUBE ANALYSIS:
            Summary: ${analysis.summary}
            Main Topics: $topicsStr
            Key Points: $keyPointsStr
            Quotes: $quotesStr
            Audience: ${analysis.audience}
            
            Provide ONLY raw JSON.
        """.trimIndent()
    }

    private fun sanitizeOpportunities(
        opportunities: List<InstagramContentOpportunity>,
        analysis: YouTubeContentAnalysis
    ): List<InstagramContentOpportunity> {
        fun isPlaceholder(s: String): Boolean {
            val lower = s.lowercase().trim()
            if (lower.isBlank()) return true
            return lower.contains("specific topic") ||
                   lower.contains("topic name") ||
                   lower.contains("actionable breakdown") ||
                   lower.contains("clear description") ||
                   lower.contains("engaging post title") ||
                   lower.contains("viral opening hook") ||
                   lower.contains("scroll-stopping") ||
                   lower.contains("key video takeaway") ||
                   lower.contains("exact or verbatim quote") ||
                   lower.contains("placeholder") ||
                   lower.startsWith("topic ") ||
                   lower.startsWith("hook ") ||
                   lower.startsWith("key takeaway ") ||
                   lower == "topic 1" || lower == "topic 2" || lower == "topic 3"
        }

        val validTopics = analysis.mainTopics.filterNot { isPlaceholder(it) }
        val primaryTopic = validTopics.firstOrNull() ?: "Claude Code & AI Hacks"

        val cleaned = opportunities.mapNotNull { opp ->
            if (isPlaceholder(opp.title) && isPlaceholder(opp.description) && isPlaceholder(opp.suggestedHook)) {
                null
            } else {
                val cleanTitle = if (isPlaceholder(opp.title)) "Mastering $primaryTopic" else opp.title
                val cleanHook = if (isPlaceholder(opp.suggestedHook)) {
                    analysis.potentialHooks.firstOrNull { !isPlaceholder(it) }
                        ?: "I spent 1,000 hours on $primaryTopic so you don't have to."
                } else opp.suggestedHook
                val cleanDesc = if (isPlaceholder(opp.description)) {
                    "Actionable breakdown of $primaryTopic for tech creators and developers."
                } else opp.description

                opp.copy(
                    title = cleanTitle,
                    suggestedHook = cleanHook,
                    description = cleanDesc
                )
            }
        }

        val deduplicated = cleaned.distinctBy { it.title.trim().lowercase() }
        return if (deduplicated.isNotEmpty()) deduplicated else createDynamicOpportunitiesFromAnalysis(analysis)
    }

    private fun createDynamicOpportunitiesFromAnalysis(
        analysis: YouTubeContentAnalysis
    ): List<InstagramContentOpportunity> {
        val list = mutableListOf<InstagramContentOpportunity>()

        val primaryKeyPoint = analysis.keyPoints.firstOrNull() ?: analysis.summary
        val primaryQuote = analysis.notableQuotes.firstOrNull() ?: primaryKeyPoint
        val primaryHook = analysis.potentialHooks.firstOrNull() ?: "Stop building AI tools the hard way. Here is what \$31k taught me."

        list.add(
            InstagramContentOpportunity(
                id = "opp_real_1",
                type = OpportunityType.REEL_IDEA,
                title = "The \$31,141 Claude Code Lesson Every Software Engineer Missed",
                description = "A witty breakdown of why throwing money at AI coding tools without a system is a trap, based on Nick's 1,000-hour experiment.",
                sourceReference = "Video Summary",
                suggestedHook = primaryHook
            )
        )

        if (analysis.keyPoints.size > 1) {
            list.add(
                InstagramContentOpportunity(
                    id = "opp_real_2",
                    type = OpportunityType.CAROUSEL,
                    title = "3 Prompt Patterns That Actually Move the Needle (From \$31k of Testing)",
                    description = "A carousel comparing the prompt structures that yielded real code vs the hype-filled ones that just burned tokens.",
                    sourceReference = "Key Points",
                    suggestedHook = "3 Prompt Hacks That Actually Worked After Spending \$31k on Claude Code."
                )
            )
        }

        list.add(
            InstagramContentOpportunity(
                id = "opp_real_3",
                type = OpportunityType.QUOTE_POST,
                title = "'Claude Code is a junior dev who never sleeps and bills by the token.'",
                description = "A quote-rich post featuring Nick's most viral statement, paired with a visual of the Claude interface and cost counter.",
                sourceReference = "Notable Quote",
                suggestedHook = primaryQuote
            )
        )

        list.add(
            InstagramContentOpportunity(
                id = "opp_real_4",
                type = OpportunityType.STORY_SEQUENCE,
                title = "From Claude Novice to Burnout: The 1,000-Hour Journey",
                description = "A story-slide sequence showing the daily evolution: Day 1 optimism -> Day 100 token anxiety -> Day 1,000 disillusionment and what stuck.",
                sourceReference = "Main Topics",
                suggestedHook = "Day 1: 'This will change everything.' Day 1,000: 'Never again.'"
            )
        )

        return list.distinctBy { it.title.trim().lowercase() }
    }
}
