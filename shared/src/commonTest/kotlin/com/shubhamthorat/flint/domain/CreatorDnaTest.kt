package com.shubhamthorat.flint.domain

import com.shubhamthorat.flint.domain.model.CreatorDNA
import com.shubhamthorat.flint.domain.model.CreatorProfile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CreatorDnaTest {

    @Test
    fun testDefaultCreatorDna() {
        val dna = CreatorDNA()
        assertEquals("Conversational", dna.preferredTone)
        assertEquals("Story-driven", dna.writingStyle)
        assertEquals(3, dna.humorLevel)
        assertTrue(dna.languages.contains("English"))
    }

    @Test
    fun testCustomCreatorProfile() {
        val dna = CreatorDNA(
            preferredTone = "Authoritative",
            writingStyle = "Data-backed",
            niche = "Android Multiplatform",
            languages = listOf("English", "Hindi", "Spanish"),
        )
        val profile = CreatorProfile(
            userId = "user_123",
            handle = "@shubham",
            bio = "Building Flint - One spark, endless stories.",
            dna = dna
        )

        assertEquals("user_123", profile.userId)
        assertEquals("@shubham", profile.handle)
        assertEquals("Authoritative", profile.dna.preferredTone)
        assertEquals(3, profile.dna.languages.size)
    }
}