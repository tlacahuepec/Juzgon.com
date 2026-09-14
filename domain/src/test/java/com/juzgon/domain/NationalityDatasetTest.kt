package com.juzgon.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NationalityDatasetTest {
    @Test
    fun `all contains at least 100 entries`() {
        assertTrue(
            "Expected at least 100 nationalities but got ${NationalityDataset.all.size}",
            NationalityDataset.all.size >= 100,
        )
    }

    @Test
    fun `all codes are unique`() {
        val codes = NationalityDataset.all.map { it.code }
        assertEquals(codes.size, codes.toSet().size)
    }

    @Test
    fun `each entry has non-blank code nationality country and flagEmoji`() {
        NationalityDataset.all.forEach { option ->
            assertTrue("code is blank for $option", option.code.isNotBlank())
            assertTrue("nationality is blank for $option", option.nationality.isNotBlank())
            assertTrue("country is blank for $option", option.country.isNotBlank())
            assertTrue("flagEmoji is blank for $option", option.flagEmoji.isNotBlank())
        }
    }

    @Test
    fun `findByCode returns matching option for valid code`() {
        val result = NationalityDataset.findByCode("MX")
        assertNotNull(result)
        assertEquals("Mexican", result!!.nationality)
        assertEquals("Mexico", result.country)
    }

    @Test
    fun `findByCode returns null for unknown code`() {
        assertNull(NationalityDataset.findByCode("ZZZZZ"))
    }

    @Test
    fun `findByCode is case insensitive`() {
        val result = NationalityDataset.findByCode("mx")
        assertNotNull(result)
        assertEquals("Mexican", result!!.nationality)
    }

    @Test
    fun `search filters by nationality prefix`() {
        val results = NationalityDataset.search("Mexi")
        assertTrue(results.any { it.code == "MX" })
    }

    @Test
    fun `search filters by country prefix`() {
        val results = NationalityDataset.search("Mexi")
        assertTrue(results.any { it.country == "Mexico" })
    }

    @Test
    fun `search filters by alias`() {
        val results = NationalityDataset.search("USA")
        assertTrue(results.any { it.code == "US" })
    }

    @Test
    fun `search is case insensitive`() {
        val results = NationalityDataset.search("mexi")
        assertTrue(results.any { it.code == "MX" })
    }

    @Test
    fun `search with empty query returns all`() {
        val results = NationalityDataset.search("")
        assertEquals(NationalityDataset.all.size, results.size)
    }

    @Test
    fun `search with blank query returns all`() {
        val results = NationalityDataset.search("   ")
        assertEquals(NationalityDataset.all.size, results.size)
    }
}
