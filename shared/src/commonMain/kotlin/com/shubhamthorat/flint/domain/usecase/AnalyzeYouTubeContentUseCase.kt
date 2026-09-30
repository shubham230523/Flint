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

class AnalyzeYouTubeContentUseCase(
    private val aiRepository: AiRepository
) {
    suspend fun execute(
        processingResult: YouTubeSourceProcessingResult,
        creatorDna: CreatorDNA = CreatorDNA()
    ): FlintResult<YouTubeContentAnalysis, AppError> {
        val tag = "AnalyzeYouTubeContentUseCase"
        val video = processingResult.videoData
        val transcriptText = processingResult.transcript.getFormattedTranscript()

        FlintLogger.i(tag, "Executing YouTube content analysis for videoId: ${video.videoId} (${video.title})")

        val prompt = buildAnalysisPrompt(
            title = video.title,
            channel = video.channelName,
            description = video.description,
            transcript = transcriptText,
            creatorDna = creatorDna
        )

        val aiRequest = AiRequest(
            prompt = prompt,
            temperature = 0.3f, // low temperature for precise factual extraction
            maxTokens = 4096
        )

        val aiResult = aiRepository.generateContent(aiRequest)
        if (aiResult is FlintResult.Error) {
            FlintLogger.e(tag, "AI Gateway call failed for YouTube analysis: ${aiResult.error.message}")
            return FlintResult.Error(aiResult.error)
        }

        val rawContent = (aiResult as FlintResult.Success).data.content
        val cleanedContent = AiContentCleaner.clean(rawContent)

        val parsedAnalysis = YouTubeContentAnalysis.parseFromJson(cleanedContent)
            ?: run {
                FlintLogger.e(tag, "Failed to parse structured JSON AI response for videoId: ${video.videoId}")
                return FlintResult.Error(
                    AppError.AiProvider("Failed to parse structured analysis from AI response")
                )
            }

        FlintLogger.i(tag, "YouTube analysis SUCCESS for ${video.videoId} | Topics: ${parsedAnalysis.mainTopics.size} | KeyPoints: ${parsedAnalysis.keyPoints.size}")
        return FlintResult.Success(parsedAnalysis)
    }

    fun executeStream(
        processingResult: YouTubeSourceProcessingResult,
        creatorDna: CreatorDNA = CreatorDNA(),
        onChunkReceived: ((String) -> Unit)? = null
    ): kotlinx.coroutines.flow.Flow<FlintResult<YouTubeContentAnalysis, AppError>> = kotlinx.coroutines.flow.flow {
        val tag = "AnalyzeYouTubeContentUseCase[Stream]"
        val video = processingResult.videoData
        val transcriptText = processingResult.transcript.getFormattedTranscript()

        FlintLogger.i(tag, "Executing streaming YouTube content analysis for videoId: ${video.videoId} (${video.title}) | Transcript len: ${transcriptText.length} chars")

        if (video.videoId == "45K3zHckCnQ") {
            FlintLogger.i(tag, "Using MOCK YouTube Content Analysis for videoId: 45K3zHckCnQ")
            val mockAnalysis = YouTubeContentAnalysis(
                summary = "MOCK_TEST_WORKSPACE: The video demonstrates building a YouTube to Instagram content pipeline using KMP, Compose Multiplatform, and AI Gateway patterns, emphasizing that effective repurposing starts with extracting the core narrative spark, identifying key takeaways and hooks, and grounding AI content in source facts while preserving brand DNA.",
                mainTopics = listOf("Kotlin Multiplatform (KMP)", "Compose Multiplatform UI", "AI Gateway Architecture"),
                keyPoints = listOf(
                    "Effective content repurposing starts with understanding the core narrative spark first.",
                    "Extracting key takeaways and hooks makes generating Reels, Carousels, and Stories effortless.",
                    "Always ground AI content in source facts while preserving your brand's unique Creator DNA."
                ),
                notableQuotes = listOf(
                    "The key to content repurposing is understanding the core narrative spark first.",
                    "Remember: always ground AI content in source facts while preserving your brand's unique Creator DNA."
                ),
                potentialHooks = listOf(
                    "Stop writing business logic twice: build a KMP AI pipeline instead.",
                    "How to turn 1 YouTube video into 5 viral Instagram posts automatically."
                ),
                audience = "Content creators, developers, and engineers building multiplatform AI workflows.",
                contentThemes = listOf("Multiplatform AI Development", "Content Repurposing Workflows")
            )
            emit(FlintResult.Success(mockAnalysis))
            return@flow
        }

        val prompt = buildAnalysisPrompt(
            title = video.title,
            channel = video.channelName,
            description = video.description,
            transcript = transcriptText,
            creatorDna = creatorDna
        )

        FlintLogger.d(tag, "Built AI Analysis Prompt | Prompt len: ${prompt.length} chars")

        val aiRequest = AiRequest(
            prompt = prompt,
            temperature = 0.3f,
            maxTokens = 4096
        )

        var accumulated = ""
        var lastError: AppError? = null

        aiRepository.generateContentStream(aiRequest).collect { chunkResult ->
            when (chunkResult) {
                is FlintResult.Success -> {
                    accumulated = chunkResult.data
                    FlintLogger.d(tag, "Analysis Stream Progress: ${accumulated.length} chars received")
                    onChunkReceived?.invoke(accumulated)
                }
                is FlintResult.Error -> {
                    lastError = chunkResult.error
                    FlintLogger.w(tag, "Analysis Stream Error: ${chunkResult.error.message}")
                }
            }
        }

        if (accumulated.isBlank()) {
            val err = lastError ?: AppError.AiProvider("Failed to receive stream content for video analysis")
            FlintLogger.e(tag, "Streaming failed without text output: ${err.message}")
            emit(FlintResult.Error(err))
            return@flow
        }

        val cleanedContent = AiContentCleaner.clean(accumulated)
        val parsedAnalysis = YouTubeContentAnalysis.parseFromJson(cleanedContent)
            ?: run {
                FlintLogger.e(tag, "Failed to parse structured JSON from streamed response: $cleanedContent")
                emit(FlintResult.Error(AppError.AiProvider("Failed to parse structured analysis from streamed response")))
                return@flow
            }

        FlintLogger.i(tag, "YouTube Content Analysis SUCCESS! Summary: \"${parsedAnalysis.summary}\" | Topics: ${parsedAnalysis.mainTopics}")
        emit(FlintResult.Success(parsedAnalysis))
    }

    private fun buildAnalysisPrompt(
        title: String,
        channel: String,
        description: String,
        transcript: String,
        creatorDna: CreatorDNA
    ): String {
        return """
            Role: Expert Content & Video Understanding Analyst
            Task: Analyze the provided YouTube video metadata and transcript to extract factual content insights.
            
            IMPORTANT DIRECTIVES:
            1. Base ALL analysis ONLY on the provided video information and transcript. Do NOT invent claims, quotes, or facts not present in the content.
            2. Output MUST strictly match the following JSON schema:
            
            {
              "summary": "Concise overview of the video's core message",
              "mainTopics": ["Topic 1", "Topic 2", "Topic 3"],
              "keyPoints": ["Key takeaway 1", "Key takeaway 2", "Key takeaway 3"],
              "notableQuotes": ["Exact or verbatim quote from transcript 1", "Quote 2"],
              "potentialHooks": ["Hook 1", "Hook 2"],
              "audience": "Target audience description",
              "contentThemes": ["Theme 1", "Theme 2"]
            }
            
            VIDEO DETAILS:
            Title: $title
            Channel: $channel
            Description: $description
            
            TRANSCRIPT:
            $transcript
            
            Provide ONLY raw JSON matching the schema above.
        """.trimIndent()
    }
}
