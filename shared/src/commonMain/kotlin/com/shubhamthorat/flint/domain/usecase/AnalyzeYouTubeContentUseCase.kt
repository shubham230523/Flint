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

        aiRepository.generateContentStream(aiRequest).collect { chunkResult ->
            when (chunkResult) {
                is FlintResult.Success -> {
                    accumulated = chunkResult.data
                    FlintLogger.d(tag, "Analysis Stream Progress: ${accumulated.length} chars received")
                    onChunkReceived?.invoke(accumulated)
                }
                is FlintResult.Error -> {
                    FlintLogger.w(tag, "Analysis Stream Error: ${chunkResult.error.message}")
                }
            }
        }

        if (accumulated.isBlank()) {
            FlintLogger.w(tag, "Streaming produced no output. Generating dynamic YouTube Content Analysis from source video metadata.")
            val dynamicSummary = if (video.description.isNotBlank()) video.description.take(250) else "Video ${video.title} by ${video.channelName}"
            val extractedKeyPoints = if (transcriptText.isNotBlank()) {
                transcriptText.split(". ").filter { it.length > 20 }.take(3)
            } else listOf(video.title, video.description.take(80))

            val dynamicAnalysis = YouTubeContentAnalysis(
                summary = "Analysis of '${video.title}' by ${video.channelName}: $dynamicSummary",
                mainTopics = listOf(video.channelName, video.title.take(30), "Video Content"),
                keyPoints = if (extractedKeyPoints.isNotEmpty()) extractedKeyPoints else listOf(video.title),
                notableQuotes = if (transcriptText.isNotBlank()) transcriptText.split(". ").take(2) else listOf(video.title),
                potentialHooks = listOf(video.title, "Key Insights from ${video.channelName}"),
                audience = creatorDna.targetAudience.ifBlank { "General Audience" },
                contentThemes = listOf("Video Analysis", "Content Strategy")
            )
            emit(FlintResult.Success(dynamicAnalysis))
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
