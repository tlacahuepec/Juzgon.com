@file:Suppress("TooGenericExceptionCaught")

package com.juzgon.data.enrichment

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject

open class GeminiApiClient
    @Inject
    constructor() {
        private val json = Json { ignoreUnknownKeys = true }

        open suspend fun generateContent(
            apiKey: String,
            prompt: String,
            useGrounding: Boolean = true,
        ): String =
            withContext(Dispatchers.IO) {
                val url = URL("$BASE_URL/models/$MODEL:generateContent?key=$apiKey")
                val conn = (url.openConnection() as HttpURLConnection)
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true

                val body = buildRequestBody(prompt, useGrounding)
                conn.outputStream.use { it.write(body.toByteArray()) }

                val code = conn.responseCode
                if (code != HTTP_OK) {
                    val error = conn.errorStream?.bufferedReader()?.readText() ?: ""
                    throw GeminiApiException(code, error)
                }
                val responseJson = conn.inputStream.bufferedReader().readText()
                extractTextFromResponse(responseJson)
            }

        fun buildRequestBody(
            prompt: String,
            useGrounding: Boolean = true,
        ): String {
            val escapedPrompt =
                prompt
                    .replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t")
            return if (useGrounding) {
                """{"contents":[{"parts":[{"text":"$escapedPrompt"}]}],""" +
                    """"tools":[{"google_search":{}}]}"""
            } else {
                """{"contents":[{"parts":[{"text":"$escapedPrompt"}]}],""" +
                    """"generationConfig":{"responseMimeType":"application/json"}}"""
            }
        }

        fun extractTextFromResponse(responseJson: String): String {
            val response =
                try {
                    json.decodeFromString<GeminiApiResponse>(responseJson)
                } catch (e: Exception) {
                    throw GeminiApiException(0, "Invalid response format: ${e.message}", e)
                }
            val text =
                response.candidates
                    ?.firstOrNull()
                    ?.content
                    ?.parts
                    ?.firstOrNull()
                    ?.text
            if (text == null) {
                throw GeminiApiException(0, "No text in response")
            }
            return stripMarkdownFences(text)
        }

        fun extractGroundingMetadata(responseJson: String): GeminiGroundingMetadata? {
            val response =
                try {
                    json.decodeFromString<GeminiApiResponse>(responseJson)
                } catch (_: Exception) {
                    return null
                }
            return response.candidates?.firstOrNull()?.groundingMetadata
        }

        fun stripMarkdownFences(text: String): String {
            val trimmed = text.trim()
            if (!trimmed.startsWith("```")) return trimmed
            val withoutOpening = trimmed.removePrefix("```json").removePrefix("```")
            val withoutFences = withoutOpening.removeSuffix("```")
            return withoutFences.trim()
        }

        private companion object {
            const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta"
            const val MODEL = "gemini-2.5-flash"
            const val HTTP_OK = 200
        }
    }

@Serializable
internal data class GeminiApiResponse(
    val candidates: List<GeminiCandidate>? = null,
)

@Serializable
internal data class GeminiCandidate(
    val content: GeminiContent? = null,
    val groundingMetadata: GeminiGroundingMetadata? = null,
)

@Serializable
internal data class GeminiContent(
    val parts: List<GeminiPart>? = null,
)

@Serializable
internal data class GeminiPart(
    @SerialName("text") val text: String? = null,
)

@Serializable
data class GeminiGroundingMetadata(
    val webSearchQueries: List<String>? = null,
    val groundingChunks: List<GeminiGroundingChunk>? = null,
    val groundingSupports: List<GeminiGroundingSupport>? = null,
)

@Serializable
data class GeminiGroundingChunk(
    val web: GeminiWebChunk? = null,
)

@Serializable
data class GeminiWebChunk(
    val title: String? = null,
    val uri: String? = null,
)

@Serializable
data class GeminiGroundingSupport(
    val segment: GeminiSegment? = null,
    val groundingChunkIndices: List<Int>? = null,
    val confidenceScores: List<Double>? = null,
)

@Serializable
data class GeminiSegment(
    val startIndex: Int? = null,
    val endIndex: Int? = null,
)
