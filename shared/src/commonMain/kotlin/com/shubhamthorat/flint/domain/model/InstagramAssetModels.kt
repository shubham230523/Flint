package com.shubhamthorat.flint.domain.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private val json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    coerceInputValues = true
}

private fun cleanJsonString(raw: String): String {
    return raw.replace(Regex("""^```(?:json)?\s*""", RegexOption.IGNORE_CASE), "")
        .replace(Regex("""\s*```$"""), "")
        .trim()
}

@Serializable
data class InstagramReelContent(
    val hook: String,
    val body: String,
    val ending: String,
    val CTA: String,
    val suggestedDuration: String = "30-45s"
) {
    companion object {
        fun parseFromJson(rawJson: String): InstagramReelContent? {
            return try { json.decodeFromString<InstagramReelContent>(cleanJsonString(rawJson)) } catch (_: Exception) { null }
        }
    }
}

@Serializable
data class InstagramCarouselSlide(
    val slideNumber: Int,
    val headline: String,
    val body: String
)

@Serializable
data class InstagramCarousel(
    val title: String,
    val slides: List<InstagramCarouselSlide> = emptyList()
) {
    companion object {
        fun parseFromJson(rawJson: String): InstagramCarousel? {
            return try { json.decodeFromString<InstagramCarousel>(cleanJsonString(rawJson)) } catch (_: Exception) { null }
        }
    }
}

@Serializable
data class InstagramStory(
    val sequenceNumber: Int,
    val headline: String,
    val body: String,
    val interactionSuggestion: String = "",
    val CTA: String = ""
)

@Serializable
data class InstagramStorySequence(
    val title: String,
    val stories: List<InstagramStory> = emptyList()
) {
    companion object {
        fun parseFromJson(rawJson: String): InstagramStorySequence? {
            return try { json.decodeFromString<InstagramStorySequence>(cleanJsonString(rawJson)) } catch (_: Exception) { null }
        }
    }
}

@Serializable
data class InstagramQuotePost(
    val quote: String,
    val context: String,
    val caption: String,
    val CTA: String
) {
    companion object {
        fun parseFromJson(rawJson: String): InstagramQuotePost? {
            return try { json.decodeFromString<InstagramQuotePost>(cleanJsonString(rawJson)) } catch (_: Exception) { null }
        }
    }
}

@Serializable
data class InstagramCaption(
    val caption: String,
    val CTA: String,
    val hashtags: List<String> = emptyList()
) {
    companion object {
        fun parseFromJson(rawJson: String): InstagramCaption? {
            return try { json.decodeFromString<InstagramCaption>(cleanJsonString(rawJson)) } catch (_: Exception) { null }
        }
    }
}
