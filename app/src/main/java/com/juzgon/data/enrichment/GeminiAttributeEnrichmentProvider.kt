@file:Suppress("TooGenericExceptionCaught")

package com.juzgon.data.enrichment

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
        private val apiClient: GeminiApiClient,
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
                val prompt = promptBuilder.build(request)
                val responseText = apiClient.generateContent(apiKey, prompt)
                val result = responseParser.parse(responseText)
                logResult(result, request.targetAttributeKey, startTime)
                result
            } catch (e: IOException) {
                timber.log.Timber.d(e, "Network error during enrichment")
                logFailure(EnrichmentFailureCode.NETWORK_ERROR, request.targetAttributeKey, startTime)
                errorResult(EnrichmentFailureCode.NETWORK_ERROR)
            } catch (e: GeminiApiException) {
                timber.log.Timber.d(e, "Gemini API error: HTTP ${e.httpCode}")
                val failureCode = mapHttpCode(e.httpCode)
                logFailure(failureCode, request.targetAttributeKey, startTime)
                errorResult(failureCode)
            } catch (e: Exception) {
                timber.log.Timber.d(e, "Unexpected enrichment error")
                logFailure(EnrichmentFailureCode.PROVIDER_ERROR, request.targetAttributeKey, startTime)
                errorResult(EnrichmentFailureCode.PROVIDER_ERROR)
            }
        }

        private fun mapHttpCode(httpCode: Int): EnrichmentFailureCode =
            when (httpCode) {
                HTTP_UNAUTHORIZED, HTTP_FORBIDDEN -> EnrichmentFailureCode.INVALID_API_KEY
                HTTP_TOO_MANY_REQUESTS -> EnrichmentFailureCode.RATE_LIMITED
                else -> EnrichmentFailureCode.PROVIDER_ERROR
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
            const val HTTP_UNAUTHORIZED = 401
            const val HTTP_FORBIDDEN = 403
            const val HTTP_TOO_MANY_REQUESTS = 429
        }
    }
