package com.shubhamthorat.flint.domain.model

import kotlinx.serialization.Serializable

/**
 * Creator DNA captures the unique brand voice, style, audience, and content preferences
 * used by Flint AI engine to generate personalized, authentic content.
 */
@Serializable
data class CreatorDNA(
    val preferredTone: String = "",
    val writingStyle: String = "",
    val targetAudience: String = "",
    val niche: String = "",
    val technicalDepth: String = "",
    val preferredHooks: List<String> = emptyList(),
    val ctaStyle: String = "",
    val humorLevel: Int = 1,
    val preferredContentLength: String = "",
    val languages: List<String> = emptyList()
)

@Serializable
data class CreatorProfile(
    val userId: String,
    val handle: String,
    val bio: String = "",
    val dna: CreatorDNA = CreatorDNA()
)
