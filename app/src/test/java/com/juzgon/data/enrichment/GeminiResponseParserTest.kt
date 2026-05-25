package com.juzgon.data.enrichment

import com.juzgon.domain.enrichment.EnrichmentConfidence
import com.juzgon.domain.enrichment.EnrichmentFailureCode
import com.juzgon.domain.enrichment.EnrichmentStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GeminiResponseParserTest {
    private val parser = GeminiResponseParser()

    @Test
    fun parse_validFoundJson_returnsFoundResult() {
        val json =
            """
            {
              "status": "FOUND",
              "value": "1987-06-24",
              "displayValue": "June 24, 1987",
              "confidence": "HIGH",
              "reason": "Multiple reliable sources agree.",
              "sources": [
                {"title": "Wikipedia", "url": "https://en.wikipedia.org/wiki/Messi", "snippet": "Born June 24, 1987"}
              ]
            }
            """.trimIndent()

        val result = parser.parse(json)

        assertEquals(EnrichmentStatus.FOUND, result.status)
        assertEquals("1987-06-24", result.suggestedValue)
        assertEquals("June 24, 1987", result.displayValue)
        assertEquals(EnrichmentConfidence.HIGH, result.confidence)
        assertEquals("Multiple reliable sources agree.", result.reason)
        assertEquals(1, result.sources.size)
        assertEquals("Wikipedia", result.sources[0].title)
        assertEquals("https://en.wikipedia.org/wiki/Messi", result.sources[0].url)
        assertEquals("Born June 24, 1987", result.sources[0].snippet)
    }

    @Test
    fun parse_validNotFoundJson_returnsNotFoundResult() {
        val json =
            """
            {
              "status": "NOT_FOUND",
              "value": null,
              "displayValue": null,
              "confidence": "LOW",
              "reason": "No reliable source was found.",
              "sources": []
            }
            """.trimIndent()

        val result = parser.parse(json)

        assertEquals(EnrichmentStatus.NOT_FOUND, result.status)
        assertNull(result.suggestedValue)
        assertNull(result.displayValue)
        assertEquals(EnrichmentConfidence.LOW, result.confidence)
        assertEquals("No reliable source was found.", result.reason)
        assertEquals(0, result.sources.size)
    }

    @Test
    fun parse_validConflictJson_returnsConflictWithSources() {
        val json =
            """
            {
              "status": "CONFLICT",
              "value": null,
              "confidence": "LOW",
              "reason": "Conflicting birth dates found.",
              "sources": [
                {"title": "Source A", "url": "https://a.com"},
                {"title": "Source B", "url": "https://b.com"}
              ]
            }
            """.trimIndent()

        val result = parser.parse(json)

        assertEquals(EnrichmentStatus.CONFLICT, result.status)
        assertEquals(2, result.sources.size)
        assertEquals("Source A", result.sources[0].title)
        assertEquals("Source B", result.sources[1].title)
    }

    @Test
    fun parse_missingOptionalFields_usesDefaults() {
        val json =
            """
            {
              "status": "FOUND",
              "value": "1987-06-24",
              "confidence": "HIGH"
            }
            """.trimIndent()

        val result = parser.parse(json)

        assertEquals(EnrichmentStatus.FOUND, result.status)
        assertEquals("1987-06-24", result.suggestedValue)
        assertNull(result.displayValue)
        assertNull(result.reason)
        assertEquals(0, result.sources.size)
    }

    @Test
    fun parse_malformedJson_returnsInvalidResponseFormat() {
        val result = parser.parse("not json at all {{{")

        assertEquals(EnrichmentStatus.ERROR, result.status)
        assertEquals(EnrichmentFailureCode.INVALID_RESPONSE_FORMAT, result.failureCode)
    }

    @Test
    fun parse_emptyString_returnsInvalidResponseFormat() {
        val result = parser.parse("")

        assertEquals(EnrichmentStatus.ERROR, result.status)
        assertEquals(EnrichmentFailureCode.INVALID_RESPONSE_FORMAT, result.failureCode)
    }

    @Test
    fun parse_blankString_returnsInvalidResponseFormat() {
        val result = parser.parse("   ")

        assertEquals(EnrichmentStatus.ERROR, result.status)
        assertEquals(EnrichmentFailureCode.INVALID_RESPONSE_FORMAT, result.failureCode)
    }

    @Test
    fun parse_unknownStatus_returnsProviderError() {
        val json =
            """
            {
              "status": "UNKNOWN_STATUS",
              "value": "something"
            }
            """.trimIndent()

        val result = parser.parse(json)

        assertEquals(EnrichmentStatus.ERROR, result.status)
        assertEquals(EnrichmentFailureCode.PROVIDER_ERROR, result.failureCode)
    }

    @Test
    fun parse_mediumConfidence_mapCorrectly() {
        val json =
            """
            {
              "status": "FOUND",
              "value": "1990-01-01",
              "confidence": "MEDIUM"
            }
            """.trimIndent()

        val result = parser.parse(json)

        assertEquals(EnrichmentConfidence.MEDIUM, result.confidence)
    }

    @Test
    fun parse_sourcesWithMissingFields_handlesGracefully() {
        val json =
            """
            {
              "status": "FOUND",
              "value": "1987-06-24",
              "confidence": "HIGH",
              "sources": [{"title": "Only Title"}]
            }
            """.trimIndent()

        val result = parser.parse(json)

        assertEquals(1, result.sources.size)
        assertEquals("Only Title", result.sources[0].title)
        assertNull(result.sources[0].url)
        assertNull(result.sources[0].snippet)
    }
}
