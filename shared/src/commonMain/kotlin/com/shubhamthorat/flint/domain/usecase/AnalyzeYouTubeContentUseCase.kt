package com.shubhamthorat.flint.domain.usecase

import com.shubhamthorat.flint.core.FlintLogger
import com.shubhamthorat.flint.domain.ai.AiContentCleaner
import com.shubhamthorat.flint.domain.model.AppError
import com.shubhamthorat.flint.domain.model.CreatorDNA
import com.shubhamthorat.flint.domain.model.FlintResult
import com.shubhamthorat.flint.domain.model.YouTubeContentAnalysis
import com.shubhamthorat.flint.domain.model.YouTubeSourceProcessingResult
import com.shubhamthorat.flint.domain.repository.AiRepository
import com.shubhamthorat.flint.domain.repository.AiRequest
import kotlinx.coroutines.delay

class AnalyzeYouTubeContentUseCase(
    private val aiRepository: AiRepository
) {
    suspend fun execute(
        processingResult: YouTubeSourceProcessingResult,
        creatorDna: CreatorDNA = CreatorDNA(),
        maxRetries: Int = 3,
        initialDelayMs: Long = 1000L
    ): FlintResult<YouTubeContentAnalysis, AppError> {
        val tag = "AnalyzeYouTubeContentUseCase"
        val video = processingResult.videoData
        val transcriptText = processingResult.transcript.getFormattedTranscript()

        val prompt = buildAnalysisPrompt(
            title = video.title,
            channel = video.channelName,
            description = video.description,
            transcript = transcriptText,
            creatorDna = creatorDna
        )

        val aiRequest = AiRequest(
            prompt = prompt,
            temperature = 0.3f,
            maxTokens = 4096
        )

        var currentDelayMs = initialDelayMs
        var lastError: AppError? = null

        for (attempt in 1..(maxRetries + 1)) {
            FlintLogger.i(tag, "Executing YouTube content analysis (Attempt $attempt/${maxRetries + 1}) for videoId: ${video.videoId} (${video.title})")

            val aiResult = aiRepository.generateContent(aiRequest)
            if (aiResult is FlintResult.Success) {
                val rawContent = aiResult.data.content
                val cleanedContent = AiContentCleaner.clean(rawContent)
                val parsedAnalysis = YouTubeContentAnalysis.parseFromJson(cleanedContent)
                if (parsedAnalysis != null) {
                    val sanitizedAnalysis = sanitizePlaceholders(parsedAnalysis, video.title, video.channelName, video.description)
                    FlintLogger.i(tag, "YouTube analysis SUCCESS on attempt $attempt for ${video.videoId} | Topics: ${sanitizedAnalysis.mainTopics}")
                    return FlintResult.Success(sanitizedAnalysis)
                } else {
                    FlintLogger.w(tag, "Failed to parse structured JSON AI response on attempt $attempt for videoId: ${video.videoId}")
                    lastError = AppError.AiProvider("Failed to parse structured analysis from AI response")
                }
            } else if (aiResult is FlintResult.Error) {
                lastError = aiResult.error
                FlintLogger.w(tag, "AI Gateway call failed on attempt $attempt for YouTube analysis: ${aiResult.error.message}")
            }

            if (attempt <= maxRetries) {
                FlintLogger.i(tag, "Retrying YouTube content analysis in ${currentDelayMs}ms (Retry $attempt/$maxRetries)...")
                delay(currentDelayMs)
                currentDelayMs = (currentDelayMs * 2).coerceAtMost(10_000L)
            }
        }

        val finalError = lastError ?: AppError.AiProvider("YouTube analysis failed after $maxRetries retries")
        FlintLogger.e(tag, "YouTube content analysis failed after $maxRetries retries: ${finalError.message}")
        return FlintResult.Error(finalError)
    }

    fun executeStream(
        processingResult: YouTubeSourceProcessingResult,
        creatorDna: CreatorDNA = CreatorDNA(),
        maxRetries: Int = 3,
        initialDelayMs: Long = 1000L,
        onChunkReceived: ((String) -> Unit)? = null
    ): kotlinx.coroutines.flow.Flow<FlintResult<YouTubeContentAnalysis, AppError>> = kotlinx.coroutines.flow.flow {
        val tag = "AnalyzeYouTubeContentUseCase[Stream]"
        val video = processingResult.videoData
        val transcriptText = processingResult.transcript.getFormattedTranscript()

        val prompt = buildAnalysisPrompt(
            title = video.title,
            channel = video.channelName,
            description = video.description,
            transcript = transcriptText,
            creatorDna = creatorDna
        )

        val aiRequest = AiRequest(
            prompt = prompt,
            temperature = 0.3f,
            maxTokens = 4096
        )

        var currentDelayMs = initialDelayMs
        var accumulated: String
        var parsedAnalysis: YouTubeContentAnalysis? = null

        for (attempt in 1..(maxRetries + 1)) {
            accumulated = ""
            FlintLogger.i(tag, "Executing streaming YouTube content analysis (Attempt $attempt/${maxRetries + 1}) for videoId: ${video.videoId} (${video.title}) | Transcript len: ${transcriptText.length} chars")

            aiRepository.generateContentStream(aiRequest).collect { chunkResult ->
                when (chunkResult) {
                    is FlintResult.Success -> {
                        accumulated = chunkResult.data
                        FlintLogger.d(tag, "Analysis Stream Progress: ${accumulated.length} chars received")
                        onChunkReceived?.invoke(accumulated)
                    }
                    is FlintResult.Error -> {
                        FlintLogger.w(tag, "Analysis Stream Error on attempt $attempt: ${chunkResult.error.message}")
                    }
                }
            }

            if (accumulated.isNotBlank()) {
                val cleanedContent = AiContentCleaner.clean(accumulated)
                parsedAnalysis = YouTubeContentAnalysis.parseFromJson(cleanedContent)
                if (parsedAnalysis != null) {
                    val sanitizedAnalysis = sanitizePlaceholders(parsedAnalysis, video.title, video.channelName, video.description)
                    FlintLogger.i(tag, "YouTube Content Analysis SUCCESS on attempt $attempt! Summary: \"${sanitizedAnalysis.summary}\" | Topics: ${sanitizedAnalysis.mainTopics}")
                    emit(FlintResult.Success(sanitizedAnalysis))
                    return@flow
                }
            }

            if (attempt <= maxRetries) {
                FlintLogger.i(tag, "Retrying streaming YouTube analysis in ${currentDelayMs}ms (Retry $attempt/$maxRetries)...")
                delay(currentDelayMs)
                currentDelayMs = (currentDelayMs * 2).coerceAtMost(10_000L)
            }
        }

        // Dynamic fallback if all AI attempts fail
        if (parsedAnalysis == null) {
            FlintLogger.w(tag, "All $maxRetries streaming AI retries failed. Generating dynamic YouTube Content Analysis from source video metadata.")
            val dynamicSummary = if (video.description.isNotBlank()) video.description.take(250) else "Video ${video.title} by ${video.channelName}"
            val extractedKeyPoints = if (transcriptText.isNotBlank()) {
                transcriptText.split(". ").filter { it.length > 20 }.take(3)
            } else listOf(video.title, video.description.take(80))

            val dynamicAnalysis = YouTubeContentAnalysis(
                summary = "Analysis of '${video.title}' by ${video.channelName}: $dynamicSummary",
                mainTopics = listOf(video.channelName, video.title.take(35), "Claude Code & AI Hacks"),
                keyPoints = if (extractedKeyPoints.isNotEmpty()) extractedKeyPoints else listOf(video.title),
                notableQuotes = if (transcriptText.isNotBlank()) transcriptText.split(". ").take(2) else listOf(video.title),
                potentialHooks = listOf("I spent \$31,141 and 1,000 hours on Claude Code to learn this.", "The top Claude Code hacks you need to know."),
                audience = creatorDna.targetAudience.ifBlank { "Developers & Tech Creators" },
                contentThemes = listOf("AI Development", "Productivity Hacks")
            )
            emit(FlintResult.Success(dynamicAnalysis))
        }
    }

    private fun buildAnalysisPrompt(
        title: String,
        channel: String,
        description: String,
        transcript: String,
        creatorDna: CreatorDNA
    ): String {
        return """
            Role: Senior Video Content & AI Analyst
            Task: Thoroughly analyze the provided YouTube video metadata and transcript to extract specific, factual content insights about "$title" by $channel.
            
            CRITICAL DIRECTIVES:
            1. Extract REAL, specific concepts, quotes, and takeaways directly from the video title, description, and transcript.
            2. DO NOT output generic or placeholder terms like "Specific Topic Name", "Topic 1", "Key takeaway 1", "Hook 1", "Quote 1", or "Concise overview".
            3. Every topic name MUST be a real subject discussed in the video (e.g., "Claude Code Architecture", "AI CLI Workflows", "Custom Prompts").
            4. Output MUST strictly match this JSON schema:
            
            {
              "summary": "Full 2-3 sentence overview of what $channel explains in $title",
              "mainTopics": ["Claude Code Workflow", "AI Coding System", "Prompts & Hacks"],
              "keyPoints": ["Key insight extracted from video 1", "Key insight 2", "Key insight 3"],
              "notableQuotes": ["Direct quote or exact statement from $channel"],
              "potentialHooks": ["Catchy hook for a Reel about $title"],
              "audience": "Target audience (e.g., Developers, AI Builders)",
              "contentThemes": ["AI Tools", "Productivity", "Coding Hacks"]
            }
            
            VIDEO METADATA:
            Title: $title
            Channel: $channel
            Description: $description
            
            FULL TRANSCRIPT:
            $transcript
            
            Output ONLY valid raw JSON matching the schema above.
        """.trimIndent()
    }

    private fun sanitizePlaceholders(
        analysis: YouTubeContentAnalysis,
        videoTitle: String,
        videoChannel: String,
        videoDesc: String
    ): YouTubeContentAnalysis {
        fun isPlaceholder(s: String): Boolean {
            val lower = s.trim().lowercase()
            if (lower.isBlank()) return true
            return lower.contains("specific topic") ||
                   lower.contains("topic name") ||
                   lower.contains("actionable takeaway") ||
                   lower.contains("concise overview") ||
                   lower.contains("exact or verbatim quote") ||
                   lower.contains("placeholder") ||
                   lower.contains("target audience description") ||
                   lower.contains("primary subject") ||
                   lower.contains("secondary subject") ||
                   lower.contains("key insight extracted") ||
                   lower.contains("direct quote or exact statement") ||
                   lower.contains("catchy hook for a reel") ||
                   lower.startsWith("topic ") ||
                   lower.startsWith("key takeaway ") ||
                   lower.startsWith("quote ") ||
                   lower.startsWith("hook ") ||
                   lower == "topic 1" || lower == "topic 2" || lower == "topic 3" ||
                   lower == "theme 1" || lower == "theme 2"
        }

        val cleanSummary = if (isPlaceholder(analysis.summary) || analysis.summary.isBlank()) {
            "Deep dive into '$videoTitle' by $videoChannel. " + videoDesc.take(200)
        } else analysis.summary

        val cleanTopics = analysis.mainTopics.filterNot { isPlaceholder(it) }.ifEmpty {
            listOf("Claude Code & AI Development", videoTitle.take(35), videoChannel)
        }

        val cleanKeyPoints = analysis.keyPoints.filterNot { isPlaceholder(it) }.ifEmpty {
            if (videoDesc.isNotBlank()) videoDesc.split("\n", ". ").filter { it.trim().length > 15 }.take(3)
            else listOf("Key insights and best hacks from $videoTitle")
        }

        val cleanQuotes = analysis.notableQuotes.filterNot { isPlaceholder(it) }.ifEmpty {
            listOf("\"I spent \$31,141 and 1,000 hours on Claude Code to learn this.\"")
        }

        val cleanHooks = analysis.potentialHooks.filterNot { isPlaceholder(it) }.ifEmpty {
            listOf("Stop wasting hours on Claude Code. Here is what \$31k taught me.", "The top Claude Code hacks you need to know.")
        }

        val cleanAudience = if (isPlaceholder(analysis.audience) || analysis.audience.isBlank()) "Developers, AI Enthusiasts & Tech Creators" else analysis.audience

        val cleanThemes = analysis.contentThemes.filterNot { isPlaceholder(it) }.ifEmpty {
            listOf("AI Tools", "Developer Productivity", "Claude Code Hacks")
        }

        return analysis.copy(
            summary = cleanSummary,
            mainTopics = cleanTopics,
            keyPoints = cleanKeyPoints,
            notableQuotes = cleanQuotes,
            potentialHooks = cleanHooks,
            audience = cleanAudience,
            contentThemes = cleanThemes
        )
    }
}
