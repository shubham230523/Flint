package com.shubhamthorat.flint.presentation.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shubhamthorat.flint.core.FlintLogger
import com.shubhamthorat.flint.domain.model.ReelCandidate
import com.shubhamthorat.flint.domain.repository.ContentAsset
import com.shubhamthorat.flint.domain.repository.ContentRepository
import com.shubhamthorat.flint.domain.repository.ContentStatus
import com.shubhamthorat.flint.domain.repository.ContentType
import com.shubhamthorat.flint.presentation.component.FlintButton
import com.shubhamthorat.flint.presentation.component.FlintButtonVariant
import com.shubhamthorat.flint.presentation.component.FlintCard
import com.shubhamthorat.flint.presentation.component.FlintChip
import com.shubhamthorat.flint.presentation.component.FlintTextField
import com.shubhamthorat.flint.presentation.navigation.NavigationManager
import com.shubhamthorat.flint.presentation.theme.FlintTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

@Composable
fun FlintReelEditorScreen(
    navigationManager: NavigationManager,
    contentRepository: ContentRepository,
    initialCandidate: ReelCandidate,
    onSaveComplete: () -> Unit = {},
    onBack: (() -> Unit)? = null
) {
    val coroutineScope = rememberCoroutineScope()
    var title by remember { mutableStateOf(initialCandidate.title) }
    var hook by remember { mutableStateOf(initialCandidate.hook) }
    var ctaText by remember { mutableStateOf(initialCandidate.ctaText.ifBlank { "Save & Share this Reel!" }) }
    var transcriptSnippet by remember { mutableStateOf(initialCandidate.transcript) }
    var isSaving by remember { mutableStateOf(false) }

    // Reel Player Simulation State
    var isPlaying by remember { mutableStateOf(true) }
    var currentProgressMs by remember { mutableStateOf(0L) }
    val durationMs = initialCandidate.durationMs.coerceAtLeast(20000L)

    val videoPath = initialCandidate.videoUrl.ifBlank { "C:/tmp/flint_media/reels/job_prod_local_1_final_reel.mp4" }
    var totalPreviewFrames by remember { mutableStateOf(0) }

    // Prepare Preview Frames and Audio on load
    LaunchedEffect(videoPath) {
        totalPreviewFrames = preparePreviewFramesAndAudio(videoPath)
    }

    // Play/Pause Audio according to playback state
    LaunchedEffect(isPlaying, currentProgressMs) {
        if (isPlaying) {
            playPreviewAudio(currentProgressMs)
        } else {
            pausePreviewAudio()
        }
    }

    // Simulated Reel Playback Timer Loop
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            delay(100)
            currentProgressMs += 100L
            if (currentProgressMs >= durationMs) {
                currentProgressMs = 0L
            }
        }
    }

    fun openSystemVideoFile() {
        FlintLogger.i("FlintReelEditorScreen", "Opening Reel video file: $videoPath")
        openVideoFileInSystemPlayer(videoPath)
    }

    fun saveToContentLibrary() {
        isSaving = true
        coroutineScope.launch {
            val asset = ContentAsset(
                id = "reel_asset_${Random.nextInt(100000, 999999)}",
                sourceId = initialCandidate.sourceId,
                title = title,
                body = "🎬 REEL HOOK:\n$hook\n\n📹 SCRIPT:\n$transcriptSnippet\n\n📣 CTA:\n$ctaText\n\n⏱️ Duration: ${initialCandidate.durationMs / 1000}s",
                type = ContentType.INSTAGRAM_REEL,
                status = ContentStatus.DRAFT,
                platform = "Instagram"
            )

            FlintLogger.i("FlintReelEditorScreen", "Saving Reel asset ID ${asset.id} ('$title') to Content Library")
            contentRepository.saveContent(asset)
            isSaving = false
            onSaveComplete()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(FlintTheme.spacing.medium),
        verticalArrangement = Arrangement.spacedBy(FlintTheme.spacing.medium)
    ) {
        // Top Navigation Bar
        if (onBack != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FlintButton(
                    onClick = { onBack() },
                    text = "← Back to Opportunities",
                    variant = FlintButtonVariant.SECONDARY
                )
            }
        }
        // Header
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Flint Reel Player & Editor",
                style = FlintTheme.typography.headlineMedium,
                color = FlintTheme.colors.onBackground,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Preview your vertical 9:16 Reel, edit text overlays & captions, or open in system media player.",
                style = FlintTheme.typography.bodyMedium,
                color = FlintTheme.colors.textSecondary
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(FlintTheme.spacing.medium),
            verticalAlignment = Alignment.Top
        ) {
            // Authentic Vertical 9:16 Instagram Reel Player Phone Container
            Box(
                modifier = Modifier
                    .width(260.dp)
                    .height(460.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF1C1B29),
                                Color(0xFF12111E),
                                Color(0xFF090812)
                            )
                        )
                    )
                    .border(2.dp, Color(0xFFFF9F0A).copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                    .clickable { isPlaying = !isPlaying }
                    .padding(12.dp)
            ) {
                // Render Real Video Frame Preview Image if available
                val currentFrameNum = ((currentProgressMs / 1000L) % 30L + 1L).toInt()
                val frameBitmap = remember(currentFrameNum, totalPreviewFrames) {
                    loadPreviewFrameBitmap(currentFrameNum)
                }

                if (frameBitmap != null) {
                    androidx.compose.foundation.Image(
                        bitmap = frameBitmap,
                        contentDescription = "Reel Frame Preview",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                }
                // Top Hook Text Overlay Card (FFmpeg Hook Filter Style)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.75f))
                        .border(1.dp, Color(0xFFFF9F0A), RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    Text(
                        text = hook,
                        style = FlintTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Center Speaker Animated Visual Container & Play State Button
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(64.dp)
                            .height(64.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFF9F0A).copy(alpha = 0.2f))
                            .border(2.dp, Color(0xFFFF9F0A), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isPlaying) "⏸" else "▶",
                            fontSize = 24.sp,
                            color = Color.White
                        )
                    }

                    Text(
                        text = if (isPlaying) "Playing Reel..." else "Paused",
                        style = FlintTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )

                    // Scrubber progress bar
                    val progressFraction = (currentProgressMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color.White.copy(alpha = 0.2f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progressFraction)
                                .height(4.dp)
                                .background(Color(0xFFFF9F0A))
                        )
                    }
                }

                // Bottom Subtitle Captions Overlay Container
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 44.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Burned-in Subtitle Subtitle Bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.Black.copy(alpha = 0.85f))
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "💬 " + transcriptSnippet.take(80) + "...",
                            style = FlintTheme.typography.labelSmall,
                            color = Color.Yellow,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // CTA Banner Overlay
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFFF9F0A).copy(alpha = 0.9f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = ctaText,
                            style = FlintTheme.typography.labelSmall,
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Aspect ratio badge
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(4.dp)
                ) {
                    FlintChip(selected = true, onClick = {}, label = "9:16 Reel")
                }
            }

            // Right-Side Editor & Actions Panel
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(FlintTheme.spacing.medium)
            ) {
                FlintCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(FlintTheme.spacing.medium),
                        verticalArrangement = Arrangement.spacedBy(FlintTheme.spacing.small)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Reel MP4 Actions",
                                style = FlintTheme.typography.titleMedium,
                                color = FlintTheme.colors.onSurface,
                                fontWeight = FontWeight.Bold
                            )

                            FlintButton(
                                onClick = { openSystemVideoFile() },
                                text = "Open MP4 Video 🚀",
                                variant = FlintButtonVariant.SECONDARY
                            )
                        }

                        val displayPath = initialCandidate.videoUrl.ifBlank { "C:/tmp/flint_media/reels/${initialCandidate.id}_final_reel.mp4" }
                        Text(
                            text = "Rendered file location: $displayPath",
                            style = FlintTheme.typography.labelSmall,
                            color = FlintTheme.colors.primary
                        )
                    }
                }

                FlintCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(FlintTheme.spacing.medium),
                        verticalArrangement = Arrangement.spacedBy(FlintTheme.spacing.small)
                    ) {
                        Text(
                            text = "Edit Overlay & Captions",
                            style = FlintTheme.typography.titleMedium,
                            color = FlintTheme.colors.onSurface,
                            fontWeight = FontWeight.Bold
                        )

                        FlintTextField(
                            value = title,
                            onValueChange = { title = it },
                            placeholder = "Reel Title"
                        )

                        FlintTextField(
                            value = hook,
                            onValueChange = { hook = it },
                            placeholder = "Hook Text Overlay"
                        )

                        FlintTextField(
                            value = ctaText,
                            onValueChange = { ctaText = it },
                            placeholder = "Call To Action (CTA)"
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(FlintTheme.spacing.small, Alignment.End)
                        ) {
                            FlintButton(
                                onClick = { saveToContentLibrary() },
                                text = if (isSaving) "Saving..." else "Save to Content Library 💾",
                                variant = FlintButtonVariant.PRIMARY,
                                enabled = !isSaving
                            )
                        }
                    }
                }
            }
        }
    }
}
