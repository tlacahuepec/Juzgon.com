@file:Suppress("FunctionName")

package com.juzgon.data.enrichment

import android.util.Log
import com.juzgon.testutil.CapturingTimberTree
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import timber.log.Timber

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class EnrichmentLoggerTest {
    private lateinit var tree: CapturingTimberTree

    @Before
    fun setUp() {
        tree = CapturingTimberTree()
        Timber.plant(tree)
    }

    @After
    fun tearDown() {
        Timber.uprootAll()
    }

    @Test
    fun rejected_logsWarningWithCorrectFormat() {
        EnrichmentLogger.rejected(
            attributeKey = "birthDate",
            reason = "VALIDATION_FAILED",
            originalStatus = "FOUND",
            confidence = "LOW",
        )

        val entry = tree.logs.single()
        assertEquals(Log.WARN, entry.priority)
        assertEquals("JuzgonEnrichment", entry.tag)
        assertTrue(entry.message.contains("attribute=birthDate"))
        assertTrue(entry.message.contains("reason=VALIDATION_FAILED"))
        assertTrue(entry.message.contains("status=FOUND"))
        assertTrue(entry.message.contains("confidence=LOW"))
    }

    @Test
    fun accepted_logsDebugWithCorrectFormat() {
        EnrichmentLogger.accepted(
            attributeKey = "birthDate",
            itemId = "item-123",
            suggestedValue = "1987-06-24",
        )

        val entry = tree.logs.single()
        assertEquals(Log.DEBUG, entry.priority)
        assertEquals("JuzgonEnrichment", entry.tag)
        assertTrue(entry.message.contains("attribute=birthDate"))
        assertTrue(entry.message.contains("itemId=item-123"))
        assertTrue(entry.message.contains("suggestedValue=1987-06-24"))
    }

    @Test
    fun dismissed_logsDebugWithCorrectFormat() {
        EnrichmentLogger.dismissed(
            attributeKey = "birthDate",
            itemId = "item-123",
        )

        val entry = tree.logs.single()
        assertEquals(Log.DEBUG, entry.priority)
        assertEquals("JuzgonEnrichment", entry.tag)
        assertTrue(entry.message.contains("attribute=birthDate"))
        assertTrue(entry.message.contains("itemId=item-123"))
    }

    @Test
    fun noLogContainsApiKeyPattern() {
        EnrichmentLogger.started(
            provider = "Gemini",
            attributeKey = "birthDate",
            catalogType = "PERSON",
        )
        EnrichmentLogger.succeeded(
            provider = "Gemini",
            attributeKey = "birthDate",
            confidence = "HIGH",
            sourceCount = 2,
            durationMs = 1234L,
        )
        EnrichmentLogger.failed(
            provider = "Gemini",
            attributeKey = "birthDate",
            failureCode = "RATE_LIMITED",
            durationMs = 1234L,
        )
        EnrichmentLogger.rejected(
            attributeKey = "birthDate",
            reason = "VALIDATION_FAILED",
            originalStatus = "FOUND",
            confidence = "LOW",
        )
        EnrichmentLogger.accepted(
            attributeKey = "birthDate",
            itemId = "item-123",
            suggestedValue = "1987-06-24",
        )
        EnrichmentLogger.dismissed(
            attributeKey = "birthDate",
            itemId = "item-123",
        )

        val apiKeyPattern = Regex("AIza[0-9A-Za-z_-]{35}")
        tree.logs.forEach { entry ->
            assertTrue(
                "Log should not contain API key pattern: ${entry.message}",
                !apiKeyPattern.containsMatchIn(entry.message),
            )
        }
    }
}
