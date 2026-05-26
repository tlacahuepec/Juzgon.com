package com.juzgon.data.enrichment

import org.junit.Assert.assertEquals
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
}
