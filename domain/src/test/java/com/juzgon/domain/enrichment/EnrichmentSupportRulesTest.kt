package com.juzgon.domain.enrichment

import com.juzgon.domain.Attribute
import com.juzgon.domain.AttributeType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EnrichmentSupportRulesTest {
    @Test
    fun dateAttributes_areSupported() {
        val birthDate = Attribute(id = "cat/Birth Date", type = AttributeType.DATE)
        val releaseDate = Attribute(id = "cat/Release Date", type = AttributeType.DATE)
        assertTrue(EnrichmentSupportRules.isSupported(birthDate))
        assertTrue(EnrichmentSupportRules.isSupported(releaseDate))
    }

    @Test
    fun dropdownAttributes_areSupported() {
        val dropdown = Attribute(id = "cat/Position", type = AttributeType.DROPDOWN)
        assertTrue(EnrichmentSupportRules.isSupported(dropdown))
    }

    @Test
    fun nationalityAttributes_areSupported() {
        val nationality = Attribute(id = "cat/Nationality", type = AttributeType.NATIONALITY)
        assertTrue(EnrichmentSupportRules.isSupported(nationality))
    }

    @Test
    fun skinTypeAttributes_areSupported() {
        val skinType = Attribute(id = "cat/Skin Type", type = AttributeType.SKIN_TYPE)
        assertTrue(EnrichmentSupportRules.isSupported(skinType))
    }

    @Test
    fun numberAttribute_isNotSupported() {
        val attribute = Attribute(id = "cat/Score", type = AttributeType.NUMBER)
        assertFalse(EnrichmentSupportRules.isSupported(attribute))
    }

    @Test
    fun booleanAttribute_isNotSupported() {
        val attribute = Attribute(id = "cat/Active", type = AttributeType.BOOLEAN)
        assertFalse(EnrichmentSupportRules.isSupported(attribute))
    }

    @Test
    fun imageAttribute_isNotSupported() {
        val attribute = Attribute(id = "cat/Photo", type = AttributeType.IMAGE)
        assertFalse(EnrichmentSupportRules.isSupported(attribute))
    }
}
