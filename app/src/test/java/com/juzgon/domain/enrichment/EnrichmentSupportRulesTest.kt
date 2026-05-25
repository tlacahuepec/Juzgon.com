package com.juzgon.domain.enrichment

import com.juzgon.domain.Attribute
import com.juzgon.domain.AttributeType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EnrichmentSupportRulesTest {
    @Test
    fun birthDateAttribute_isSupported() {
        val attribute = Attribute(id = "cat/Birth Date", type = AttributeType.DATE)
        assertTrue(EnrichmentSupportRules.isSupported(attribute))
    }

    @Test
    fun birthDateCaseInsensitive_isSupported() {
        val attribute = Attribute(id = "cat/birth date", type = AttributeType.DATE)
        assertTrue(EnrichmentSupportRules.isSupported(attribute))
    }

    @Test
    fun numberAttribute_isNotSupported() {
        val attribute = Attribute(id = "cat/Score", type = AttributeType.NUMBER)
        assertFalse(EnrichmentSupportRules.isSupported(attribute))
    }

    @Test
    fun dateAttributeNotBirthDate_isNotSupported() {
        val attribute = Attribute(id = "cat/Release Date", type = AttributeType.DATE)
        assertFalse(EnrichmentSupportRules.isSupported(attribute))
    }

    @Test
    fun booleanAttribute_isNotSupported() {
        val attribute = Attribute(id = "cat/Active", type = AttributeType.BOOLEAN)
        assertFalse(EnrichmentSupportRules.isSupported(attribute))
    }

    @Test
    fun supportedAttributeLabel_returnsBirthDate() {
        val attribute = Attribute(id = "cat/Birth Date", type = AttributeType.DATE)
        assertTrue(EnrichmentSupportRules.isSupported(attribute))
    }
}
