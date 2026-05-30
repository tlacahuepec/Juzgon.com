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
                val cached = cacheRepository.get(cacheKey)
                if (cached != null) {
                    return AttributeEnrichmentResult(
                        status = cached.status,
                        suggestedValue = cached.suggestedValue,
                        displayValue = cached.displayValue,
                        confidence = cached.confidence,
                        sources = cached.sources,
                        reason = cached.reason,
                        failureCode = cached.failureCode,
                    )
                }
            }

            val enriched = provider.enrichAttribute(request)
            val validated = validator(enriched, request.targetAttributeType)

            if (enriched.status == EnrichmentStatus.FOUND &&
                validated.failureCode == EnrichmentFailureCode.VALIDATION_FAILED
            ) {
                eventLogger.rejected(
                    attributeKey = request.targetAttributeKey,
                    reason = "VALIDATION_FAILED",
                    originalStatus = enriched.status.name,
                    confidence = enriched.confidence?.name,
                )
            }

            val shouldCache =
                validated.status != EnrichmentStatus.ERROR ||
                    validated.failureCode == EnrichmentFailureCode.NO_RELIABLE_SOURCE ||
                    validated.failureCode == EnrichmentFailureCode.CONFLICTING_SOURCES

            if (shouldCache) {
                cacheRepository.put(validated.toCachedResult(cacheKey))
            }

            return validated
        }
    }
