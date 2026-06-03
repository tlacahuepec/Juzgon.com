package com.juzgon.data.enrichment

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GeminiApiClientTest {
    private val client = GeminiApiClient()

    @Test
    fun buildRequestBody_containsPromptText() {
        val body = client.buildRequestBody("Hello world")

        assertTrue(body.contains("Hello world"))
        assertTrue(body.contains("\"text\""))
    }

    @Test
    fun buildRequestBody_requestsJsonMimeType() {
        val body = client.buildRequestBody("test")

        assertTrue(body.contains("application/json"))
        assertTrue(body.contains("responseMimeType"))
    }

    @Test
    fun extractTextFromResponse_validResponse_returnsText() {
        val json =
            """
            {
              "candidates": [
                {
                  "content": {
                    "parts": [
                      {
                        "text": "{\"status\":\"FOUND\",\"value\":\"1987-06-24\"}"
                      }
                    ]
                  }
                }
              ]
            }
            """.trimIndent()

        val result = client.extractTextFromResponse(json)

        assertEquals("{\"status\":\"FOUND\",\"value\":\"1987-06-24\"}", result)
    }

    @Test(expected = GeminiApiException::class)
    fun extractTextFromResponse_emptyCandidates_throws() {
        val json = """{"candidates": []}"""
        client.extractTextFromResponse(json)
    }

    @Test(expected = GeminiApiException::class)
    fun extractTextFromResponse_missingText_throws() {
        val json = """{"candidates": [{"content": {"parts": []}}]}"""
        client.extractTextFromResponse(json)
    }

    @Test(expected = GeminiApiException::class)
    fun extractTextFromResponse_invalidJson_throws() {
        client.extractTextFromResponse("not json at all")
    }

    @Test
    fun stripMarkdownFences_plainJson_unchanged() {
        val json = """{"status":"FOUND","value":"1987-06-24"}"""
        assertEquals(json, client.stripMarkdownFences(json))
    }

    @Test
    fun stripMarkdownFences_jsonFence_stripped() {
        val input = "```json\n{\"status\":\"FOUND\"}\n```"
        assertEquals("{\"status\":\"FOUND\"}", client.stripMarkdownFences(input))
    }

    @Test
    fun stripMarkdownFences_plainFence_stripped() {
        val input = "```\n{\"status\":\"FOUND\"}\n```"
        assertEquals("{\"status\":\"FOUND\"}", client.stripMarkdownFences(input))
    }

    @Test
    fun extractTextFromResponse_markdownWrapped_stripsAndReturns() {
        val json =
            """
            {
              "candidates": [
                {
                  "content": {
                    "parts": [
                      {
                        "text": "```json\n{\"status\":\"FOUND\",\"value\":\"1987-06-24\"}\n```"
                      }
                    ]
                  }
                }
              ]
            }
            """.trimIndent()

        val result = client.extractTextFromResponse(json)

        assertEquals("{\"status\":\"FOUND\",\"value\":\"1987-06-24\"}", result)
    }

    @Test
    fun buildRequestBody_withGrounding_includesGoogleSearchTool() {
        val body = client.buildRequestBody("test prompt", useGrounding = true)

        assertTrue(body.contains("\"tools\""))
        assertTrue(body.contains("\"googleSearch\""))
    }

    @Test
    fun buildRequestBody_withoutGrounding_noToolsField() {
        val body = client.buildRequestBody("test prompt", useGrounding = false)

        assertFalse(body.contains("\"tools\""))
        assertFalse(body.contains("\"googleSearch\""))
    }

    @Test
    fun buildRequestBody_defaultGrounding_includesGoogleSearchTool() {
        val body = client.buildRequestBody("test prompt")

        assertTrue(body.contains("\"googleSearch\""))
    }

    @Test
    fun extractGroundingMetadata_validResponse_returnsMetadata() {
        val json =
            """
            {
              "candidates": [
                {
                  "content": {
                    "parts": [{"text": "{\"status\":\"FOUND\",\"value\":\"1987-06-24\"}"}]
                  },
                  "groundingMetadata": {
                    "webSearchQueries": ["Lionel Messi birth date"],
                    "groundingChunks": [
                      {"web": {"title": "Wikipedia", "uri": "https://en.wikipedia.org/wiki/Lionel_Messi"}}
                    ]
                  }
                }
              ]
            }
            """.trimIndent()

        val metadata = client.extractGroundingMetadata(json)

        assertNotNull(metadata)
        assertEquals(listOf("Lionel Messi birth date"), metadata!!.webSearchQueries)
        assertEquals(1, metadata.groundingChunks!!.size)
        assertEquals("Wikipedia", metadata.groundingChunks!![0].web?.title)
        assertEquals("https://en.wikipedia.org/wiki/Lionel_Messi", metadata.groundingChunks!![0].web?.uri)
    }

    @Test
    fun extractGroundingMetadata_noMetadata_returnsNull() {
        val json =
            """
            {
              "candidates": [
                {
                  "content": {
                    "parts": [{"text": "{\"status\":\"FOUND\"}"}]
                  }
                }
              ]
            }
            """.trimIndent()

        val metadata = client.extractGroundingMetadata(json)

        assertNull(metadata)
    }
}
