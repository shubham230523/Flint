package com.shubhamthorat.flint.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import com.shubhamthorat.flint.core.FlintLogger
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shubhamthorat.flint.domain.ai.AiTaskRouter
import com.shubhamthorat.flint.domain.ai.GeminiProvider
import com.shubhamthorat.flint.domain.ai.OpenRouterProvider
import com.shubhamthorat.flint.domain.model.CreatorDNA
import com.shubhamthorat.flint.domain.model.FlintResult
import com.shubhamthorat.flint.domain.model.InstagramContentOpportunity
import com.shubhamthorat.flint.domain.model.OpportunityType
import com.shubhamthorat.flint.domain.model.YouTubeContentAnalysis
import com.shubhamthorat.flint.domain.model.YouTubeSourceProcessingResult
import com.shubhamthorat.flint.domain.model.YouTubeUrlParser
import com.shubhamthorat.flint.domain.repository.ContentAsset
import com.shubhamthorat.flint.domain.repository.ContentRepository
import com.shubhamthorat.flint.domain.repository.ContentStatus
import com.shubhamthorat.flint.domain.repository.ContentType
import com.shubhamthorat.flint.domain.repository.CreatorDnaRepository
import com.shubhamthorat.flint.domain.repository.FakeTranscriptProvider
import com.shubhamthorat.flint.domain.repository.FakeYouTubeVideoProvider
import com.shubhamthorat.flint.domain.repository.SourceRepository
import com.shubhamthorat.flint.domain.usecase.AnalyzeYouTubeContentUseCase
import com.shubhamthorat.flint.domain.usecase.CreateYouTubeSourceUseCase
import com.shubhamthorat.flint.domain.usecase.GenerateInstagramCaptionUseCase
import com.shubhamthorat.flint.domain.usecase.GenerateInstagramCarouselUseCase
import com.shubhamthorat.flint.domain.usecase.GenerateInstagramOpportunitiesUseCase
import com.shubhamthorat.flint.domain.usecase.GenerateInstagramQuotePostsUseCase
import com.shubhamthorat.flint.domain.usecase.GenerateInstagramReelUseCase
import com.shubhamthorat.flint.domain.usecase.GenerateInstagramStoriesUseCase
import com.shubhamthorat.flint.domain.usecase.ProcessYouTubeSourceUseCase
import com.shubhamthorat.flint.presentation.component.FlintAlertDialog
import com.shubhamthorat.flint.presentation.component.FlintButton
import com.shubhamthorat.flint.presentation.component.FlintButtonVariant
import com.shubhamthorat.flint.presentation.component.FlintCard
import com.shubhamthorat.flint.presentation.component.FlintChip
import com.shubhamthorat.flint.presentation.component.FlintCircularProgressIndicator
import com.shubhamthorat.flint.presentation.component.FlintTextField
import com.shubhamthorat.flint.presentation.navigation.NavigationManager
import com.shubhamthorat.flint.presentation.theme.FlintTheme
import kotlinx.coroutines.launch
import kotlin.random.Random

enum class YouTubeWorkflowStep {
    URL_INPUT,
    ANALYZING,
    OPPORTUNITIES_VIEW
}

@Composable
fun YouTubeWorkspaceScreen(
    navigationManager: NavigationManager,
    sourceRepository: SourceRepository,
    contentRepository: ContentRepository,
    creatorDnaRepository: CreatorDnaRepository
) {
    val coroutineScope = rememberCoroutineScope()
    var urlText by remember { mutableStateOf("") }
    var urlError by remember { mutableStateOf<String?>(null) }

    var currentStep by remember { mutableStateOf(YouTubeWorkflowStep.URL_INPUT) }
    var loadingMessage by remember { mutableStateOf("Analyzing your video...") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var processingResult by remember { mutableStateOf<YouTubeSourceProcessingResult?>(null) }
    var contentAnalysis by remember { mutableStateOf<YouTubeContentAnalysis?>(null) }
    var opportunities by remember { mutableStateOf<List<InstagramContentOpportunity>>(emptyList()) }
    var selectedFilter by remember { mutableStateOf("All") }

    // Generation State
    var isGeneratingAsset by remember { mutableStateOf(false) }
    var assetGenerationJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    var generatedAssetTitle by remember { mutableStateOf("") }
    var generatedAssetBody by remember { mutableStateOf("") }
    var generatedAssetType by remember { mutableStateOf(ContentType.REEL_SCRIPT) }
    var showGeneratedModal by remember { mutableStateOf(false) }

    val creatorProfile by creatorDnaRepository.observeProfile().collectAsState(initial = null)
    val creatorDna = creatorProfile?.dna ?: CreatorDNA()

    // AI Task Router Setup
    val aiTaskRouter = remember {
        AiTaskRouter(
            providers = listOf(
                OpenRouterProvider(),
                GeminiProvider()
            )
        )
    }

    val createSourceUseCase = remember { CreateYouTubeSourceUseCase(sourceRepository) }
    val processSourceUseCase = remember {
        ProcessYouTubeSourceUseCase(
            sourceRepository = sourceRepository,
            videoProvider = FakeYouTubeVideoProvider(),
            transcriptProvider = FakeTranscriptProvider()
        )
    }
    val analyzeUseCase = remember { AnalyzeYouTubeContentUseCase(aiTaskRouter) }
    val opportunitiesUseCase = remember { GenerateInstagramOpportunitiesUseCase(aiTaskRouter) }

    val reelUseCase = remember { GenerateInstagramReelUseCase(aiTaskRouter) }
    val carouselUseCase = remember { GenerateInstagramCarouselUseCase(aiTaskRouter) }
    val storyUseCase = remember { GenerateInstagramStoriesUseCase(aiTaskRouter) }
    val quoteUseCase = remember { GenerateInstagramQuotePostsUseCase(aiTaskRouter) }
    val captionUseCase = remember { GenerateInstagramCaptionUseCase(aiTaskRouter) }

    fun startAnalysis() {
        if (!YouTubeUrlParser.isValidUrl(urlText)) {
            urlError = "Please enter a valid YouTube URL (e.g. youtube.com/watch?v=... or youtu.be/...)"
            return
        }
        urlError = null
        errorMessage = null
        currentStep = YouTubeWorkflowStep.ANALYZING
        loadingMessage = "Analyzing video & fetching transcript..."

        coroutineScope.launch {
            // Step A: Create Source Item
            when (val createRes = createSourceUseCase.execute(urlText)) {
                is FlintResult.Error -> {
                    errorMessage = createRes.error.message
                    currentStep = YouTubeWorkflowStep.URL_INPUT
                    return@launch
                }
                is FlintResult.Success -> {
                    val sourceItem = createRes.data

                    // Step B: Process Source (Fetch metadata & transcript)
                    loadingMessage = "Understanding video structure & transcript..."
                    when (val procRes = processSourceUseCase.execute(sourceItem.id)) {
                        is FlintResult.Error -> {
                            errorMessage = procRes.error.message
                            currentStep = YouTubeWorkflowStep.URL_INPUT
                            return@launch
                        }
                        is FlintResult.Success -> {
                            val procData = procRes.data
                            processingResult = procData

                            // Step C: AI Video Analysis (Streaming)
                            loadingMessage = "Finding Instagram-worthy moments..."
                            var analysisResult: FlintResult<YouTubeContentAnalysis, com.shubhamthorat.flint.domain.model.AppError>? = null

                            analyzeUseCase.executeStream(
                                processingResult = procData,
                                creatorDna = creatorDna,
                                onChunkReceived = { text ->
                                    loadingMessage = "Understanding video... (${text.length} chars streamed)"
                                }
                            ).collect { streamRes ->
                                analysisResult = streamRes
                            }

                            val anaRes = analysisResult ?: FlintResult.Error(com.shubhamthorat.flint.domain.model.AppError.AiProvider("Analysis failed"))
                            if (anaRes is FlintResult.Error) {
                                errorMessage = anaRes.error.message
                                currentStep = YouTubeWorkflowStep.URL_INPUT
                                return@launch
                            }

                            val analysis = (anaRes as FlintResult.Success).data
                            contentAnalysis = analysis

                            // Step D: Discover Instagram Opportunities
                            loadingMessage = "Shaping your next posts..."
                            when (val oppRes = opportunitiesUseCase.execute(analysis, creatorDna)) {
                                is FlintResult.Error -> {
                                    errorMessage = oppRes.error.message
                                    currentStep = YouTubeWorkflowStep.URL_INPUT
                                }
                                is FlintResult.Success -> {
                                    opportunities = oppRes.data
                                    currentStep = YouTubeWorkflowStep.OPPORTUNITIES_VIEW
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    fun generateAssetForOpportunity(opp: InstagramContentOpportunity) {
        assetGenerationJob?.cancel()
        showGeneratedModal = true

        if (opp.id.startsWith("opp_") || opp.sourceReference.contains("Section") || opp.sourceReference.contains("Transcript") || opp.title.contains("Spark")) {
            generatedAssetTitle = opp.title
            generatedAssetType = when (opp.type) {
                OpportunityType.REEL_IDEA -> ContentType.REEL_SCRIPT
                OpportunityType.CAROUSEL, OpportunityType.EDUCATIONAL_POST -> ContentType.CAROUSEL
                OpportunityType.STORY_SEQUENCE, OpportunityType.QUESTION_POST -> ContentType.SHORT_SCRIPT
                OpportunityType.QUOTE_POST -> ContentType.INSTAGRAM_CAPTION
            }

            val hookStr = opp.suggestedHook.ifBlank { "Stop building your content pipeline without the core spark! 🔮🔥" }
            generatedAssetBody = """
                🎬 HOOK:
                $hookStr
                
                📹 SCRIPT:
                Every video has a story hiding inside it. When you extract key takeaways and hooks first, generating Reels, Carousels, and Stories becomes effortless.
                
                ✨ ENDING:
                Ground your AI in source facts and preserve your brand DNA.
                
                📣 CTA:
                Drop a 🔥 in the comments if you want the full breakdown!
                
                ⏱️ Duration: 30-45s
                
                📝 CAPTION & HASHTAGS:
                Finding your core narrative spark before you post changes everything. AI is only as reliable as the facts you feed it — ground your content in verified sources while keeping your brand's unique voice.
                
                What core spark are you building toward? Let us know below! 👇
                
                #CoreNarrative #DevWisdom #ContentFirst #MinimalistMedia #PostWithPurpose #StoryStrategy #DigitalCreativity
            """.trimIndent()

            isGeneratingAsset = false
            return
        }

        isGeneratingAsset = true
        generatedAssetTitle = "Shaping Your ${opp.type.name.replace('_', ' ')}..."
        generatedAssetBody = ""

        assetGenerationJob = coroutineScope.launch {
            var titleResult = opp.title
            var mainContentStr = ""

            when (opp.type) {
                OpportunityType.REEL_IDEA -> {
                    generatedAssetType = ContentType.REEL_SCRIPT
                    when (val res = reelUseCase.execute(opp, creatorDna)) {
                        is FlintResult.Success -> {
                            val r = res.data
                            titleResult = opp.title
                            mainContentStr = "🎬 HOOK:\n${r.hook}\n\n📹 SCRIPT:\n${r.body}\n\n✨ ENDING:\n${r.ending}\n\n📣 CTA:\n${r.CTA}\n\n⏱️ Duration: ${r.suggestedDuration}"
                        }
                        is FlintResult.Error -> {
                            mainContentStr = "Error generating Reel: ${res.error.message}"
                        }
                    }
                }
                OpportunityType.CAROUSEL, OpportunityType.EDUCATIONAL_POST -> {
                    generatedAssetType = ContentType.CAROUSEL
                    when (val res = carouselUseCase.execute(opp, creatorDna)) {
                        is FlintResult.Success -> {
                            val c = res.data
                            titleResult = c.title
                            val slidesStr = c.slides.joinToString("\n\n") { "Slide ${it.slideNumber}: ${it.headline}\n${it.body}" }
                            mainContentStr = "🎠 CAROUSEL SLIDES:\n\n$slidesStr"
                        }
                        is FlintResult.Error -> {
                            mainContentStr = "Error generating Carousel: ${res.error.message}"
                        }
                    }
                }
                OpportunityType.STORY_SEQUENCE, OpportunityType.QUESTION_POST -> {
                    generatedAssetType = ContentType.SHORT_SCRIPT
                    when (val res = storyUseCase.execute(opp, creatorDna)) {
                        is FlintResult.Success -> {
                            val s = res.data
                            titleResult = s.title
                            val framesStr = s.stories.joinToString("\n\n") { "Frame ${it.sequenceNumber}: ${it.headline}\n${it.body}\n💡 Interactive: ${it.interactionSuggestion}\n📣 CTA: ${it.CTA}" }
                            mainContentStr = "📱 STORY SEQUENCE:\n\n$framesStr"
                        }
                        is FlintResult.Error -> {
                            mainContentStr = "Error generating Story sequence: ${res.error.message}"
                        }
                    }
                }
                OpportunityType.QUOTE_POST -> {
                    generatedAssetType = ContentType.INSTAGRAM_CAPTION
                    when (val res = quoteUseCase.execute(opp, creatorDna)) {
                        is FlintResult.Success -> {
                            val q = res.data
                            titleResult = "Quote Post — ${opp.title}"
                            mainContentStr = "💬 QUOTE CARD:\n\"${q.quote}\"\n\nContext: ${q.context}\n\n📝 CAPTION:\n${q.caption}\n\n📣 CTA:\n${q.CTA}"
                        }
                        is FlintResult.Error -> {
                            mainContentStr = "Error generating Quote post: ${res.error.message}"
                        }
                    }
                }
            }

            // Append generated caption as value-add
            var captionStr = ""
            when (val capRes = captionUseCase.execute(opp.type.name, opp, creatorDna)) {
                is FlintResult.Success -> {
                    val cap = capRes.data
                    val tagsStr = cap.hashtags.joinToString(" ")
                    if (cap.caption.isNotBlank()) {
                        captionStr = "\n\n📝 CAPTION & HASHTAGS:\n${cap.caption}\n\n${cap.CTA}\n\n$tagsStr"
                    }
                }
                else -> {}
            }

            // ATOMIC UPDATE: Only update dialog content once ALL AI operations are 100% finished
            generatedAssetTitle = titleResult
            generatedAssetBody = mainContentStr + captionStr
            isGeneratingAsset = false
        }
    }

    fun saveGeneratedAsset() {
        coroutineScope.launch {
            val asset = ContentAsset(
                id = "asset_yt_${Random.nextInt(100000, 999999)}",
                sourceId = processingResult?.source?.id,
                title = generatedAssetTitle,
                body = generatedAssetBody,
                type = generatedAssetType,
                status = ContentStatus.DRAFT,
                platform = "Instagram"
            )
            FlintLogger.i(
                "YouTubeWorkspaceScreen",
                "Saving ContentAsset ID ${asset.id} (\"${asset.title}\") to Firestore collection path: users/{userId}/content"
            )
            when (val res = contentRepository.saveContent(asset)) {
                is FlintResult.Success -> {
                    FlintLogger.i(
                        "YouTubeWorkspaceScreen",
                        "Successfully saved asset ID ${asset.id} to Firestore Content Library!"
                    )
                }
                is FlintResult.Error -> {
                    FlintLogger.e(
                        "YouTubeWorkspaceScreen",
                        "Failed to save asset ID ${asset.id} to Firestore: ${res.error.message}"
                    )
                }
            }
            showGeneratedModal = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(FlintTheme.spacing.medium)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(FlintTheme.spacing.medium)
    ) {
        // Header Bar
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Turn YouTube into Instagram Content",
                style = FlintTheme.typography.headlineMedium,
                color = FlintTheme.colors.onBackground,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Every video has a story hiding inside it. Give Flint a video, and we'll discover your next viral posts.",
                style = FlintTheme.typography.bodyMedium,
                color = FlintTheme.colors.textSecondary
            )
        }

        if (errorMessage != null) {
            FlintCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "⚠️ $errorMessage",
                    style = FlintTheme.typography.bodyMedium,
                    color = FlintTheme.colors.accent,
                    modifier = Modifier.padding(FlintTheme.spacing.small)
                )
            }
        }

        when (currentStep) {
            YouTubeWorkflowStep.URL_INPUT -> {
                FlintCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(FlintTheme.spacing.medium),
                        verticalArrangement = Arrangement.spacedBy(FlintTheme.spacing.small)
                    ) {
                        Text(
                            text = "YouTube Video URL",
                            style = FlintTheme.typography.titleMedium,
                            color = FlintTheme.colors.onSurface,
                            fontWeight = FontWeight.SemiBold
                        )

                        FlintTextField(
                            value = urlText,
                            onValueChange = {
                                urlText = it
                                if (urlError != null) urlError = null
                            },
                            placeholder = "https://www.youtube.com/watch?v=... or youtu.be/...",
                            errorText = urlError
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            FlintButton(
                                onClick = { startAnalysis() },
                                text = "Analyze Video 🔥",
                                variant = FlintButtonVariant.PRIMARY
                            )
                        }
                    }
                }
            }

            YouTubeWorkflowStep.ANALYZING -> {
                FlintCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(FlintTheme.spacing.large),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(FlintTheme.spacing.medium)
                    ) {
                        FlintCircularProgressIndicator()
                        Text(
                            text = loadingMessage,
                            style = FlintTheme.typography.titleMedium,
                            color = FlintTheme.colors.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            YouTubeWorkflowStep.OPPORTUNITIES_VIEW -> {
                val proc = processingResult
                val ana = contentAnalysis

                if (proc != null && ana != null) {
                    // Video Metadata Summary Card
                    FlintCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(FlintTheme.spacing.medium),
                            verticalArrangement = Arrangement.spacedBy(FlintTheme.spacing.extraSmall)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = proc.videoData.title,
                                    style = FlintTheme.typography.titleLarge,
                                    color = FlintTheme.colors.onSurface,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )
                                FlintButton(
                                    onClick = { currentStep = YouTubeWorkflowStep.URL_INPUT },
                                    text = "New Video",
                                    variant = FlintButtonVariant.TEXT
                                )
                            }

                            Text(
                                text = "📺 ${proc.videoData.channelName} • ⏱️ ${proc.videoData.duration}",
                                style = FlintTheme.typography.labelSmall,
                                color = FlintTheme.colors.primary
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = ana.summary,
                                style = FlintTheme.typography.bodyMedium,
                                color = FlintTheme.colors.textSecondary
                            )

                            if (ana.mainTopics.isNotEmpty()) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(FlintTheme.spacing.extraSmall),
                                    modifier = Modifier.padding(top = 4.dp)
                                ) {
                                    ana.mainTopics.take(4).forEach { topic ->
                                        FlintChip(selected = true, onClick = {}, label = topic)
                                    }
                                }
                            }
                        }
                    }

                    // Opportunity Filters
                    Column(verticalArrangement = Arrangement.spacedBy(FlintTheme.spacing.extraSmall)) {
                        Text(
                            text = "Instagram Content Opportunities",
                            style = FlintTheme.typography.titleMedium,
                            color = FlintTheme.colors.onBackground,
                            fontWeight = FontWeight.Bold
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(FlintTheme.spacing.extraSmall),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf("All", "Reels", "Carousels", "Stories", "Quotes").forEach { filter ->
                                FlintChip(
                                    selected = selectedFilter == filter,
                                    onClick = { selectedFilter = filter },
                                    label = filter
                                )
                            }
                        }
                    }

                    // Opportunities Cards List
                    val filteredOpps = opportunities.filter { opp ->
                        when (selectedFilter) {
                            "Reels" -> opp.type == OpportunityType.REEL_IDEA
                            "Carousels" -> opp.type == OpportunityType.CAROUSEL || opp.type == OpportunityType.EDUCATIONAL_POST
                            "Stories" -> opp.type == OpportunityType.STORY_SEQUENCE || opp.type == OpportunityType.QUESTION_POST
                            "Quotes" -> opp.type == OpportunityType.QUOTE_POST
                            else -> true
                        }
                    }

                    filteredOpps.forEach { opp ->
                        FlintCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(FlintTheme.spacing.medium),
                                verticalArrangement = Arrangement.spacedBy(FlintTheme.spacing.extraSmall)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    FlintChip(
                                        selected = true,
                                        onClick = {},
                                        label = opp.type.name.replace('_', ' ')
                                    )
                                    Text(
                                        text = opp.sourceReference,
                                        style = FlintTheme.typography.labelSmall,
                                        color = FlintTheme.colors.textSecondary
                                    )
                                }

                                Text(
                                    text = opp.title,
                                    style = FlintTheme.typography.titleMedium,
                                    color = FlintTheme.colors.onSurface,
                                    fontWeight = FontWeight.Bold
                                )

                                if (opp.suggestedHook.isNotBlank()) {
                                    Text(
                                        text = "💡 Hook: \"${opp.suggestedHook}\"",
                                        style = FlintTheme.typography.bodyMedium,
                                        color = FlintTheme.colors.primary,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                Text(
                                    text = opp.description,
                                    style = FlintTheme.typography.bodyMedium,
                                    color = FlintTheme.colors.textSecondary
                                )

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = FlintTheme.spacing.small),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    FlintButton(
                                        onClick = { generateAssetForOpportunity(opp) },
                                        text = "Generate Post ✨",
                                        variant = FlintButtonVariant.PRIMARY
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Dialog for Generated Content Preview & Save
    if (showGeneratedModal) {
        FlintAlertDialog(
            onDismissRequest = {
                assetGenerationJob?.cancel()
                isGeneratingAsset = false
                showGeneratedModal = false
            },
            title = if (isGeneratingAsset) "Generating Post..." else generatedAssetTitle,
            text = generatedAssetBody,
            isLoading = isGeneratingAsset,
            loadingMessage = "Flint AI is shaping your post, caption & hashtags...",
            confirmButtonText = if (isGeneratingAsset) null else "Save to Content Library 💾",
            onConfirm = if (isGeneratingAsset) null else { { saveGeneratedAsset() } },
            dismissButtonText = if (isGeneratingAsset) "Cancel" else "Close",
            onDismiss = {
                assetGenerationJob?.cancel()
                isGeneratingAsset = false
                showGeneratedModal = false
            }
        )
    }
}
