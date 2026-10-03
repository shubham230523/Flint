package com.shubhamthorat.flint.presentation.screen

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shubhamthorat.flint.domain.model.ReelCandidate
import com.shubhamthorat.flint.domain.model.ReelCandidateStatus
import com.shubhamthorat.flint.presentation.component.FlintButton
import com.shubhamthorat.flint.presentation.component.FlintButtonVariant
import com.shubhamthorat.flint.presentation.component.FlintCard
import com.shubhamthorat.flint.presentation.component.FlintChip
import com.shubhamthorat.flint.presentation.component.FlintTextField
import com.shubhamthorat.flint.presentation.navigation.NavigationManager
import com.shubhamthorat.flint.presentation.theme.FlintTheme

@Composable
fun ReelCandidateReviewScreen(
    navigationManager: NavigationManager,
    initialCandidates: List<ReelCandidate>,
    onAcceptCandidate: (ReelCandidate) -> Unit = {},
    onRenderSelected: (List<ReelCandidate>) -> Unit = {},
    onBack: (() -> Unit)? = null
) {
    var candidates by remember { mutableStateOf(initialCandidates) }
    var editingCandidateId by remember { mutableStateOf<String?>(null) }

    val acceptedCandidates = candidates.filter { it.status == ReelCandidateStatus.ACCEPTED }

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

        // Header Bar
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Review Reel Candidates",
                style = FlintTheme.typography.headlineMedium,
                color = FlintTheme.colors.onBackground,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Flint found potential Instagram Reels in your video. Review, adjust timestamps, and select candidates to render.",
                style = FlintTheme.typography.bodyMedium,
                color = FlintTheme.colors.textSecondary
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val candidateLabel = if (candidates.size == 1) "Candidate" else "Candidates"
            Text(
                text = "Found ${candidates.size} Reel $candidateLabel (${acceptedCandidates.size} Selected)",
                style = FlintTheme.typography.titleMedium,
                color = FlintTheme.colors.primary,
                fontWeight = FontWeight.SemiBold
            )

            FlintButton(
                onClick = { onRenderSelected(acceptedCandidates) },
                text = "Render Selected (${acceptedCandidates.size}) 🎬",
                variant = FlintButtonVariant.PRIMARY,
                enabled = acceptedCandidates.isNotEmpty()
            )
        }

        candidates.forEachIndexed { index, candidate ->
            val isEditing = editingCandidateId == candidate.id

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
                        FlintChip(
                            selected = candidate.status == ReelCandidateStatus.ACCEPTED,
                            onClick = {
                                val updatedStatus = if (candidate.status == ReelCandidateStatus.ACCEPTED) ReelCandidateStatus.DISCOVERED else ReelCandidateStatus.ACCEPTED
                                val updated = candidate.copy(status = updatedStatus)
                                candidates = candidates.toMutableList().also { it[index] = updated }
                                onAcceptCandidate(updated)
                            },
                            label = if (candidate.status == ReelCandidateStatus.ACCEPTED) "✓ Accepted" else "Candidate #${index + 1}"
                        )

                        Text(
                            text = "⏱️ ${candidate.durationMs / 1000}s (${candidate.startTimeMs / 1000}s - ${candidate.endTimeMs / 1000}s)",
                            style = FlintTheme.typography.labelSmall,
                            color = FlintTheme.colors.textSecondary
                        )
                    }

                    if (isEditing) {
                        Text(
                            text = "Edit Title & Hook",
                            style = FlintTheme.typography.titleMedium,
                            color = FlintTheme.colors.onSurface,
                            fontWeight = FontWeight.Bold
                        )

                        FlintTextField(
                            value = candidate.title,
                            onValueChange = { newTitle ->
                                candidates = candidates.toMutableList().also { list ->
                                    list[index] = candidate.copy(title = newTitle)
                                }
                            },
                            placeholder = "Reel Title"
                        )

                        FlintTextField(
                            value = candidate.hook,
                            onValueChange = { newHook ->
                                candidates = candidates.toMutableList().also { list ->
                                    list[index] = candidate.copy(hook = newHook)
                                }
                            },
                            placeholder = "Opening Hook"
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            FlintButton(
                                onClick = { editingCandidateId = null },
                                text = "Done Editing",
                                variant = FlintButtonVariant.SECONDARY
                            )
                        }
                    } else {
                        Text(
                            text = candidate.title,
                            style = FlintTheme.typography.titleMedium,
                            color = FlintTheme.colors.onSurface,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "💡 Hook: \"${candidate.hook}\"",
                            style = FlintTheme.typography.bodyMedium,
                            color = FlintTheme.colors.primary,
                            fontWeight = FontWeight.Medium
                        )

                        Text(
                            text = "Transcript Snippet:\n\"${candidate.transcript}\"",
                            style = FlintTheme.typography.bodyMedium,
                            color = FlintTheme.colors.textSecondary
                        )

                        if (candidate.reason.isNotBlank()) {
                            Text(
                                text = "✨ Reason: ${candidate.reason}",
                                style = FlintTheme.typography.labelSmall,
                                color = FlintTheme.colors.accent
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(FlintTheme.spacing.small, Alignment.End)
                        ) {
                            FlintButton(
                                onClick = { editingCandidateId = candidate.id },
                                text = "Edit",
                                variant = FlintButtonVariant.TEXT
                            )

                            FlintButton(
                                onClick = {
                                    val newStatus = if (candidate.status == ReelCandidateStatus.ACCEPTED) ReelCandidateStatus.DISCOVERED else ReelCandidateStatus.ACCEPTED
                                    val updated = candidate.copy(status = newStatus)
                                    candidates = candidates.toMutableList().also { it[index] = updated }
                                    onAcceptCandidate(updated)
                                },
                                text = if (candidate.status == ReelCandidateStatus.ACCEPTED) "Deselect" else "Select Reel",
                                variant = if (candidate.status == ReelCandidateStatus.ACCEPTED) FlintButtonVariant.SECONDARY else FlintButtonVariant.PRIMARY
                            )
                        }
                    }
                }
            }
        }
    }
}
