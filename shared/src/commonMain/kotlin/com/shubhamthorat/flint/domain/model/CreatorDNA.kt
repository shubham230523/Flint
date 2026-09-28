package com.shubhamthorat.flint.domain.model

import kotlinx.serialization.Serializable

/**
 * Creator DNA captures the unique brand voice, style, audience, and content preferences
 * used by Flint AI engine to generate personalized, authentic content.
 */
@Serializable
data class CreatorDNA(
    val preferredTone: String = "Conversational",
    val writingStyle: String = "Story-driven",
    val targetAudience: String = "Tech Creators & Developers",
    val niche: String = "Software & AI Tools",
    val technicalDepth: String = "Intermediate",
    val preferredHooks: List<String> = listOf("Question", "Bold Statement", "Story Hook"),
    val ctaStyle: String = "Soft value-add",
    val humorLevel: Int = 3,
    val preferredContentLength: String = "Medium",
    val languages: List<String> = listOf("English")
)

@Serializable
data class CreatorProfile(
    val userId: String,
    val handle: String,
    val bio: String = "",
    val dna: CreatorDNA = CreatorDNA()
)
