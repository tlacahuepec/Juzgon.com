@file:Suppress("TooGenericExceptionCaught")

package com.juzgon.data.enrichment

import com.juzgon.domain.enrichment.AttributeEnrichmentProvider
import com.juzgon.domain.enrichment.AttributeEnrichmentRequest
import com.juzgon.domain.enrichment.AttributeEnrichmentResult
import com.juzgon.domain.enrichment.EnrichmentFailureCode
import com.juzgon.domain.enrichment.EnrichmentSource
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

            EnrichmentLogger.started(provider = PROVIDER_NAME)

            val startTime = System.currentTimeMillis()
            return try {
                val prompt = promptBuilder.build(request)
                EnrichmentLogger.promptSent(PROVIDER_NAME, prompt)
                val contentResult = apiClient.generateContentWithMetadata(apiKey, prompt, useGrounding = true)
                EnrichmentLogger.responseReceived(PROVIDER_NAME, contentResult.text)
                val parsedResult = responseParser.parse(contentResult.text)
                val result = mergeGroundingSources(parsedResult, contentResult.groundingMetadata)
                logResult(result, startTime)
                result
            } catch (e: IOException) {
                timber.log.Timber.e(e, "Network error during enrichment")
                logFailure(
                    EnrichmentFailureCode.NETWORK_ERROR,
                    startTime,
                )
                errorResult(EnrichmentFailureCode.NETWORK_ERROR)
            } catch (e: GeminiApiException) {
                timber.log.Timber.e(e, "Gemini API error: HTTP ${e.httpCode}")
                val failureCode = mapHttpCode(e.httpCode)
                logFailure(
                    failureCode,
                    startTime,
                )
                errorResult(failureCode)
            } catch (e: Exception) {
                timber.log.Timber.e(e, "Unexpected enrichment error: ${e.javaClass.simpleName}")
                logFailure(
                    EnrichmentFailureCode.PROVIDER_ERROR,
                    startTime,
                )
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
            startTime: Long,
        ) {
            val durationMs = System.currentTimeMillis() - startTime
            if (result.status == EnrichmentStatus.FOUND || result.status == EnrichmentStatus.NOT_FOUND) {
                EnrichmentLogger.succeeded(
                    provider = PROVIDER_NAME,
                    confidence = result.confidence?.name ?: "UNKNOWN",
                    sourceCount = result.sources.size,
                    durationMs = durationMs,
                )
            } else {
                EnrichmentLogger.failed(
                    provider = PROVIDER_NAME,
                    failureCode = result.failureCode?.name ?: result.status.name,
                    durationMs = durationMs,
                )
            }
        }

        private fun logFailure(
            failureCode: EnrichmentFailureCode,
            startTime: Long,
        ) {
            EnrichmentLogger.failed(
                provider = PROVIDER_NAME,
                failureCode = failureCode.name,
                durationMs = System.currentTimeMillis() - startTime,
            )
        }

        private fun mergeGroundingSources(
            result: AttributeEnrichmentResult,
            metadata: GeminiGroundingMetadata?,
        ): AttributeEnrichmentResult {
            val groundingSources =
                metadata?.groundingChunks?.mapNotNull { chunk ->
                    chunk.web?.let { web ->
                        EnrichmentSource(
                            title = web.title,
                            url = web.uri,
                        )
                    }
                } ?: emptyList()
            if (groundingSources.isEmpty()) return result
            val existingUrls = result.sources.mapNotNull { it.url }.toSet()
            val newSources = groundingSources.filter { it.url !in existingUrls }
            return result.copy(sources = result.sources + newSources)
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
