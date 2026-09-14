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
    fun rejected_logsStructuredReasonWithoutAttributeContent() {
        EnrichmentLogger.rejected("VALIDATION_FAILED", "FOUND", "LOW")

        val entry = tree.logs.single()
        assertEquals(Log.WARN, entry.priority)
        assertEquals("JuzgonEnrichment", entry.tag)
        assertTrue(!entry.message.contains("birthDate"))
        assertTrue(entry.message.contains("reason=VALIDATION_FAILED"))
        assertTrue(entry.message.contains("status=FOUND"))
        assertTrue(entry.message.contains("confidence=LOW"))
    }

    @Test
    fun accepted_doesNotLogItemOrSuggestedValue() {
        EnrichmentLogger.accepted()

        val entry = tree.logs.single()
        assertEquals(Log.DEBUG, entry.priority)
        assertEquals("JuzgonEnrichment", entry.tag)
        assertTrue(!entry.message.contains("birthDate"))
        assertTrue(!entry.message.contains("item-123"))
        assertTrue(!entry.message.contains("1987-06-24"))
    }

    @Test
    fun dismissed_doesNotLogItemOrAttributeContent() {
        EnrichmentLogger.dismissed()

        val entry = tree.logs.single()
        assertEquals(Log.DEBUG, entry.priority)
        assertEquals("JuzgonEnrichment", entry.tag)
        assertTrue(!entry.message.contains("birthDate"))
        assertTrue(!entry.message.contains("item-123"))
    }

    @Test
    fun noLogContainsApiKeyPattern() {
        EnrichmentLogger.started("Gemini")
        EnrichmentLogger.succeeded("Gemini", "HIGH", 2, 1234L)
        EnrichmentLogger.failed("Gemini", "RATE_LIMITED", 1234L)
        EnrichmentLogger.rejected("VALIDATION_FAILED", "FOUND", "LOW")
        EnrichmentLogger.accepted()
        EnrichmentLogger.dismissed()

        val apiKeyPattern = Regex("AIza[0-9A-Za-z_-]{35}")
        tree.logs.forEach { entry ->
            assertTrue(
                "Log should not contain API key pattern: ${entry.message}",
                !apiKeyPattern.containsMatchIn(entry.message),
            )
        }
    }

    @Test
    fun promptAndResponseLogsContainOnlyPayloadLengths() {
        val privatePrompt = "Name: Jane Doe; key: AIzaSySensitive"
        val privateResponse = "Jane Doe was born 1990-01-01"

        EnrichmentLogger.promptSent("Gemini", privatePrompt)
        EnrichmentLogger.responseReceived("Gemini", privateResponse)

        tree.logs.forEach { entry ->
            assertTrue(!entry.message.contains(privatePrompt))
            assertTrue(!entry.message.contains(privateResponse))
        }
    }
}
