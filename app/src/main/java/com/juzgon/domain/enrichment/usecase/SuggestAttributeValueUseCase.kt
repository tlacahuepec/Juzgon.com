package com.juzgon.domain.enrichment.usecase

import com.juzgon.domain.enrichment.AttributeEnrichmentProvider
import com.juzgon.domain.enrichment.AttributeEnrichmentRequest
import com.juzgon.domain.enrichment.AttributeEnrichmentResult
import com.juzgon.domain.enrichment.EnrichmentEventLogger
import com.juzgon.domain.enrichment.EnrichmentFailureCode
import com.juzgon.domain.enrichment.EnrichmentStatus
import com.juzgon.domain.enrichment.EnrichmentSuggestionCacheRepository
import com.juzgon.domain.enrichment.SecureApiKeyStore
import com.juzgon.domain.enrichment.toCacheKey
import com.juzgon.domain.enrichment.toCachedResult
import javax.inject.Inject

class SuggestAttributeValueUseCase
    @Inject
    constructor(
        private val apiKeyStore: SecureApiKeyStore,
        private val provider: AttributeEnrichmentProvider,
        private val validator: ValidateEnrichmentResultUseCase,
        private val eventLogger: EnrichmentEventLogger,
        private val cacheRepository: EnrichmentSuggestionCacheRepository,
    ) {
        @Suppress("ReturnCount")
        suspend operator fun invoke(
            request: AttributeEnrichmentRequest,
            bypassCache: Boolean = false,
        ): AttributeEnrichmentResult {
            if (!apiKeyStore.hasGeminiApiKey()) {
                return AttributeEnrichmentResult(
                    status = EnrichmentStatus.ERROR,
                    failureCode = EnrichmentFailureCode.MISSING_API_KEY,
                )
            }

            val cacheKey = request.toCacheKey()

            if (!bypassCache) {
                val cachedResult = cacheRepository.get(cacheKey)
                if (cachedResult != null) {
                    return AttributeEnrichmentResult(
                        status = cachedResult.status,
                        suggestedValue = cachedResult.suggestedValue,
                        displayValue = cachedResult.displayValue,
                        confidence = cachedResult.confidence,
                        sources = cachedResult.sources,
                        reason = cachedResult.reason,
                        failureCode = cachedResult.failureCode,
                    )
                }
            }

            val result = provider.enrichAttribute(request)
            val validated = validator(result, request.targetAttributeType)

            if (result.status == EnrichmentStatus.FOUND &&
                validated.failureCode == EnrichmentFailureCode.VALIDATION_FAILED
            ) {
                eventLogger.rejected(
                    attributeKey = request.targetAttributeKey,
                    reason = "VALIDATION_FAILED",
                    originalStatus = result.status.name,
                    confidence = result.confidence?.name,
                )
            }

            // Cache the result (for accepted suggestions and recent non-error outcomes)
            if (validated.status != EnrichmentStatus.ERROR ||
                validated.failureCode == EnrichmentFailureCode.NO_RELIABLE_SOURCE ||
                validated.failureCode == EnrichmentFailureCode.CONFLICTING_SOURCES
            ) {
                cacheRepository.put(validated.toCachedResult(cacheKey))
            }

            return validated
        }
    }
