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
            maxTokens = 2048
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
            2. Output MUST be a single raw JSON object strictly matching the following schema:
            
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
            
            Provide ONLY valid JSON.
        """.trimIndent()
    }
}
