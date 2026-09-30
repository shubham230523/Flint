package com.shubhamthorat.flint.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shubhamthorat.flint.core.FlintLogger
import com.shubhamthorat.flint.domain.model.ReelCandidate
import com.shubhamthorat.flint.domain.repository.ContentAsset
import com.shubhamthorat.flint.domain.repository.ContentRepository
import com.shubhamthorat.flint.domain.repository.ContentStatus
import com.shubhamthorat.flint.domain.repository.ContentType
import com.shubhamthorat.flint.presentation.component.FlintButton
import com.shubhamthorat.flint.presentation.component.FlintButtonVariant
import com.shubhamthorat.flint.presentation.component.FlintCard
import com.shubhamthorat.flint.presentation.component.FlintTextField
import com.shubhamthorat.flint.presentation.navigation.NavigationManager
import com.shubhamthorat.flint.presentation.theme.FlintTheme
import kotlinx.coroutines.launch
import kotlin.random.Random

@Composable
fun FlintReelEditorScreen(
    navigationManager: NavigationManager,
    contentRepository: ContentRepository,
    initialCandidate: ReelCandidate,
    onSaveComplete: () -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()
    var title by remember { mutableStateOf(initialCandidate.title) }
    var hook by remember { mutableStateOf(initialCandidate.hook) }
    var ctaText by remember { mutableStateOf(initialCandidate.ctaText.ifBlank { "Save & Share this Reel!" }) }
    var transcriptSnippet by remember { mutableStateOf(initialCandidate.transcript) }
    var isSaving by remember { mutableStateOf(false) }

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

            FlintLogger.i("FlintReelEditorScreen", "Saving Reel asset ID ${asset.id} to Content Library")
            contentRepository.saveContent(asset)
            isSaving = false
            onSaveComplete()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(FlintTheme.spacing.medium)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(FlintTheme.spacing.medium)
    ) {
        // Header
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Flint Reel Editor",
                style = FlintTheme.typography.headlineMedium,
                color = FlintTheme.colors.onBackground,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Fine-tune hook overlays, captions, CTAs and export your finished Reel.",
                style = FlintTheme.typography.bodyMedium,
                color = FlintTheme.colors.textSecondary
            )
        }

        // Preview Box Card
        FlintCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(FlintTheme.spacing.medium),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(FlintTheme.spacing.small)
            ) {
                Text(
                    text = "📱 9:16 Video Preview Placeholder",
                    style = FlintTheme.typography.titleMedium,
                    color = FlintTheme.colors.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Hook: \"$hook\"",
                    style = FlintTheme.typography.bodyMedium,
                    color = FlintTheme.colors.onSurface
                )
            }
        }

        // Editor Controls Card
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
