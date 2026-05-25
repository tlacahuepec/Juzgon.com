package com.juzgon.domain.enrichment

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EnrichmentModelsTest {
    @Test
    fun attributeEnrichmentResult_defaultValues() {
        val result = AttributeEnrichmentResult(status = EnrichmentStatus.FOUND)

        assertNull(result.suggestedValue)
        assertNull(result.displayValue)
        assertNull(result.confidence)
        assertEquals(emptyList<EnrichmentSource>(), result.sources)
        assertNull(result.reason)
        assertNull(result.failureCode)
    }

    @Test
    fun enrichmentSource_defaultValues() {
        val source = EnrichmentSource()

        assertNull(source.title)
        assertNull(source.url)
        assertNull(source.snippet)
    }

    @Test
    fun enrichmentStatus_containsAllExpectedValues() {
        val values = EnrichmentStatus.entries.map { it.name }

        assertEquals(
            listOf("FOUND", "NOT_FOUND", "CONFLICT", "ERROR"),
            values,
        )
    }

    @Test
    fun enrichmentConfidence_containsAllExpectedValues() {
        val values = EnrichmentConfidence.entries.map { it.name }

        assertEquals(
            listOf("HIGH", "MEDIUM", "LOW"),
            values,
        )
    }

    @Test
    fun enrichmentFailureCode_containsAllExpectedValues() {
        val values = EnrichmentFailureCode.entries.map { it.name }

        assertEquals(
            listOf(
                "MISSING_API_KEY",
                "INVALID_API_KEY",
                "NETWORK_ERROR",
                "RATE_LIMITED",
                "QUOTA_EXCEEDED",
                "PROVIDER_ERROR",
                "INVALID_RESPONSE_FORMAT",
                "VALIDATION_FAILED",
                "NO_RELIABLE_SOURCE",
                "CONFLICTING_SOURCES",
            ),
            values,
        )
    }
}
