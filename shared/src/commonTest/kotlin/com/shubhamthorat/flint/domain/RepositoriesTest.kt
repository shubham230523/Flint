package com.shubhamthorat.flint.domain

import com.shubhamthorat.flint.data.repository.InMemoryCampaignRepository
import com.shubhamthorat.flint.data.repository.InMemoryContentRepository
import com.shubhamthorat.flint.data.repository.InMemoryCreatorDnaRepository
import com.shubhamthorat.flint.data.repository.InMemorySourceRepository
import com.shubhamthorat.flint.domain.model.Campaign
import com.shubhamthorat.flint.domain.model.CreatorDNA
import com.shubhamthorat.flint.domain.model.FlintResult
import com.shubhamthorat.flint.domain.repository.ContentAsset
import com.shubhamthorat.flint.domain.repository.ContentType
import com.shubhamthorat.flint.domain.repository.SourceItem
import com.shubhamthorat.flint.domain.repository.SourceType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RepositoriesTest {

    @Test
    fun testInMemoryContentRepositoryStartsEmptyAndSaves() = runTest {
        val repository = InMemoryContentRepository()
        assertTrue(repository.observeContentAssets().first().isEmpty())

        val asset = ContentAsset("a1", null, "Title", "Body", ContentType.LINKEDIN_POST)
        val saveResult = repository.saveContent(asset)
        assertTrue(saveResult is FlintResult.Success)

        val list = repository.observeContentAssets().first()
        assertEquals(1, list.size)
        assertEquals("Title", list[0].title)
    }

    @Test
    fun testInMemoryCampaignRepositoryStartsEmptyAndSaves() = runTest {
        val repository = InMemoryCampaignRepository()
        assertTrue(repository.observeCampaigns().first().isEmpty())

        val campaign = Campaign("c1", "Title", "Idea", emptyList())
        val saveResult = repository.saveCampaign(campaign)
        assertTrue(saveResult is FlintResult.Success)

        val list = repository.observeCampaigns().first()
        assertEquals(1, list.size)
        assertEquals("Title", list[0].title)
    }

    @Test
    fun testInMemorySourceRepositoryStartsEmptyAndSaves() = runTest {
        val repository = InMemorySourceRepository()
        assertTrue(repository.observeSources().first().isEmpty())

        val source = SourceItem("s1", "Idea 1", SourceType.IDEA, "Raw idea text")
        val addResult = repository.addSource(source)
        assertTrue(addResult is FlintResult.Success)

        val list = repository.observeSources().first()
        assertEquals(1, list.size)
        assertEquals("Idea 1", list[0].title)
    }

    @Test
    fun testInMemoryCreatorDnaRepositoryUpdates() = runTest {
        val repository = InMemoryCreatorDnaRepository()
        val initialProfile = repository.getProfile()
        assertTrue(initialProfile is FlintResult.Success)

        val updatedDna = CreatorDNA(preferredTone = "Authoritative")
        val updatedProfile = initialProfile.data.copy(dna = updatedDna)

        val updateResult = repository.updateProfile(updatedProfile)
        assertTrue(updateResult is FlintResult.Success)

        val observed = repository.observeProfile().first()
        assertEquals("Authoritative", observed?.dna?.preferredTone)
    }
}
