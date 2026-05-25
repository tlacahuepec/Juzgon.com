@file:Suppress("TooGenericExceptionCaught")

package com.juzgon.data.enrichment

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import com.juzgon.domain.enrichment.AttributeEnrichmentProvider
import com.juzgon.domain.enrichment.AttributeEnrichmentRequest
import com.juzgon.domain.enrichment.AttributeEnrichmentResult
import com.juzgon.domain.enrichment.EnrichmentFailureCode
import com.juzgon.domain.enrichment.EnrichmentStatus
import com.juzgon.domain.enrichment.SecureApiKeyStore
import java.io.IOException
import javax.inject.Inject

class GeminiAttributeEnrichmentProvider
    @Inject
    constructor(
        private val apiKeyStore: SecureApiKeyStore,
        private val promptBuilder: GeminiPromptBuilder,
        private val responseParser: GeminiResponseParser,
    ) : AttributeEnrichmentProvider {
        override suspend fun enrichAttribute(request: AttributeEnrichmentRequest): AttributeEnrichmentResult {
            val apiKey =
                apiKeyStore.getGeminiApiKey()
                    ?: return errorResult(EnrichmentFailureCode.MISSING_API_KEY)

            EnrichmentLogger.started(
                provider = PROVIDER_NAME,
                attributeKey = request.targetAttributeKey,
                catalogType = request.catalogType?.name,
            )

            val startTime = System.currentTimeMillis()
            return try {
                val model = createModel(apiKey)
                val prompt = promptBuilder.build(request)
                val response = model.generateContent(prompt)
                val result = responseParser.parse(response.text ?: "")
                logResult(result, request.targetAttributeKey, startTime)
                result
            } catch (e: IOException) {
                timber.log.Timber.d(e, "Network error during enrichment")
                logFailure(EnrichmentFailureCode.NETWORK_ERROR, request.targetAttributeKey, startTime)
                errorResult(EnrichmentFailureCode.NETWORK_ERROR)
            } catch (e: Exception) {
                val failureCode = mapException(e)
                logFailure(failureCode, request.targetAttributeKey, startTime)
                errorResult(failureCode)
            }
        }

        private fun createModel(apiKey: String): GenerativeModel =
            GenerativeModel(
                modelName = MODEL_NAME,
                apiKey = apiKey,
                generationConfig =
                    generationConfig {
                        responseMimeType = "application/json"
                    },
            )

        private fun mapException(e: Exception): EnrichmentFailureCode {
            val message = e.message?.lowercase() ?: ""
            return when {
                "api key" in message || "401" in message || "403" in message ->
                    EnrichmentFailureCode.INVALID_API_KEY
                "429" in message || "rate" in message ->
                    EnrichmentFailureCode.RATE_LIMITED
                "quota" in message ->
                    EnrichmentFailureCode.QUOTA_EXCEEDED
                else -> EnrichmentFailureCode.PROVIDER_ERROR
            }
        }

        private fun logResult(
            result: AttributeEnrichmentResult,
            attributeKey: String,
            startTime: Long,
        ) {
            val durationMs = System.currentTimeMillis() - startTime
            if (result.status == EnrichmentStatus.FOUND || result.status == EnrichmentStatus.NOT_FOUND) {
                EnrichmentLogger.succeeded(
                    provider = PROVIDER_NAME,
                    attributeKey = attributeKey,
                    confidence = result.confidence?.name ?: "UNKNOWN",
                    sourceCount = result.sources.size,
                    durationMs = durationMs,
                )
            } else {
                EnrichmentLogger.failed(
                    provider = PROVIDER_NAME,
                    attributeKey = attributeKey,
                    failureCode = result.failureCode?.name ?: result.status.name,
                    durationMs = durationMs,
                )
            }
        }

        private fun logFailure(
            failureCode: EnrichmentFailureCode,
            attributeKey: String,
            startTime: Long,
        ) {
            EnrichmentLogger.failed(
                provider = PROVIDER_NAME,
                attributeKey = attributeKey,
                failureCode = failureCode.name,
                durationMs = System.currentTimeMillis() - startTime,
            )
        }

        private fun errorResult(failureCode: EnrichmentFailureCode) =
            AttributeEnrichmentResult(
                status = EnrichmentStatus.ERROR,
                failureCode = failureCode,
            )

        private companion object {
            const val PROVIDER_NAME = "Gemini"
            const val MODEL_NAME = "gemini-2.0-flash"
        }
    }
