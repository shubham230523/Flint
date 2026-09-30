package com.shubhamthorat.flint.domain.model

import com.shubhamthorat.flint.domain.ai.LenientJsonParser
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject

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
            val sanitized = LenientJsonParser.sanitize(rawJson)
            try {
                return LenientJsonParser.lenientJson.decodeFromString<InstagramReelContent>(sanitized)
            } catch (_: Exception) {}

            try {
                val elem = LenientJsonParser.lenientJson.parseToJsonElement(sanitized)
                if (elem is JsonObject) {
                    val hook = LenientJsonParser.extractString(elem, "hook", "openingHook", "opening_hook")
                    val body = LenientJsonParser.extractString(elem, "body", "script", "spokenScript", "content")
                    val ending = LenientJsonParser.extractString(elem, "ending", "conclusion", "outro")
                    val cta = LenientJsonParser.extractString(elem, "CTA", "cta", "callToAction")
                    val dur = LenientJsonParser.extractString(elem, "suggestedDuration", "duration").ifBlank { "30-45s" }

                    if (hook.isNotBlank() || body.isNotBlank()) {
                        return InstagramReelContent(
                            hook = hook.ifBlank { "Look at this:" },
                            body = body.ifBlank { "Here is the key takeaway." },
                            ending = ending.ifBlank { "Follow for more!" },
                            CTA = cta.ifBlank { "Save this post!" },
                            suggestedDuration = dur
                        )
                    }
                }
            } catch (_: Exception) {}

            return null
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
            val sanitized = LenientJsonParser.sanitize(rawJson)
            try {
                return LenientJsonParser.lenientJson.decodeFromString<InstagramCarousel>(sanitized)
            } catch (_: Exception) {}

            try {
                val elem = LenientJsonParser.lenientJson.parseToJsonElement(sanitized)
                if (elem is JsonObject) {
                    val title = LenientJsonParser.extractString(elem, "title", "headline", "name")
                    val slidesArray = elem.keys.firstOrNull { it.equals("slides", ignoreCase = true) || it.equals("items", ignoreCase = true) }?.let { elem[it] as? JsonArray }
                    val slides = slidesArray?.mapIndexedNotNull { idx, item ->
                        if (item is JsonObject) {
                            val h = LenientJsonParser.extractString(item, "headline", "title")
                            val b = LenientJsonParser.extractString(item, "body", "content", "description")
                            InstagramCarouselSlide(slideNumber = idx + 1, headline = h, body = b)
                        } else null
                    } ?: emptyList()

                    if (title.isNotBlank() || slides.isNotEmpty()) {
                        return InstagramCarousel(
                            title = title.ifBlank { "Instagram Carousel" },
                            slides = slides
                        )
                    }
                }
            } catch (_: Exception) {}

            return null
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
            val sanitized = LenientJsonParser.sanitize(rawJson)
            try {
                return LenientJsonParser.lenientJson.decodeFromString<InstagramStorySequence>(sanitized)
            } catch (_: Exception) {}

            try {
                val elem = LenientJsonParser.lenientJson.parseToJsonElement(sanitized)
                if (elem is JsonObject) {
                    val title = LenientJsonParser.extractString(elem, "title", "name")
                    val storiesArray = elem.keys.firstOrNull { it.equals("stories", ignoreCase = true) || it.equals("sequence", ignoreCase = true) || it.equals("frames", ignoreCase = true) }?.let { elem[it] as? JsonArray }
                    val stories = storiesArray?.mapIndexedNotNull { idx, item ->
                        if (item is JsonObject) {
                            val h = LenientJsonParser.extractString(item, "headline", "title")
                            val b = LenientJsonParser.extractString(item, "body", "content")
                            val sug = LenientJsonParser.extractString(item, "interactionSuggestion", "suggestion", "sticker")
                            val cta = LenientJsonParser.extractString(item, "CTA", "cta")
                            InstagramStory(sequenceNumber = idx + 1, headline = h, body = b, interactionSuggestion = sug, CTA = cta)
                        } else null
                    } ?: emptyList()

                    if (title.isNotBlank() || stories.isNotEmpty()) {
                        return InstagramStorySequence(
                            title = title.ifBlank { "Instagram Story Sequence" },
                            stories = stories
                        )
                    }
                }
            } catch (_: Exception) {}

            return null
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
            val sanitized = LenientJsonParser.sanitize(rawJson)
            try {
                return LenientJsonParser.lenientJson.decodeFromString<InstagramQuotePost>(sanitized)
            } catch (_: Exception) {}

            try {
                val elem = LenientJsonParser.lenientJson.parseToJsonElement(sanitized)
                if (elem is JsonObject) {
                    val quote = LenientJsonParser.extractString(elem, "quote", "text", "statement")
                    val context = LenientJsonParser.extractString(elem, "context", "background")
                    val caption = LenientJsonParser.extractString(elem, "caption", "body")
                    val cta = LenientJsonParser.extractString(elem, "CTA", "cta")

                    if (quote.isNotBlank() || caption.isNotBlank()) {
                        return InstagramQuotePost(
                            quote = quote,
                            context = context,
                            caption = caption,
                            CTA = cta
                        )
                    }
                }
            } catch (_: Exception) {}

            return null
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
            val sanitized = LenientJsonParser.sanitize(rawJson)
            try {
                return LenientJsonParser.lenientJson.decodeFromString<InstagramCaption>(sanitized)
            } catch (_: Exception) {}

            try {
                val elem = LenientJsonParser.lenientJson.parseToJsonElement(sanitized)
                if (elem is JsonObject) {
                    val cap = LenientJsonParser.extractString(elem, "caption", "text", "body")
                    val cta = LenientJsonParser.extractString(elem, "CTA", "cta")
                    val tags = LenientJsonParser.extractStringList(elem, "hashtags", "tags")

                    if (cap.isNotBlank()) {
                        return InstagramCaption(
                            caption = cap,
                            CTA = cta,
                            hashtags = tags
                        )
                    }
                }
            } catch (_: Exception) {}

            return null
        }
    }
}
