package com.juzgon.domain.enrichment.usecase

import com.juzgon.data.enrichment.EnrichmentLogger
import com.juzgon.domain.enrichment.AttributeEnrichmentProvider
import com.juzgon.domain.enrichment.AttributeEnrichmentRequest
import com.juzgon.domain.enrichment.AttributeEnrichmentResult
import com.juzgon.domain.enrichment.EnrichmentFailureCode
import com.juzgon.domain.enrichment.EnrichmentStatus
import com.juzgon.domain.enrichment.SecureApiKeyStore
import javax.inject.Inject

class SuggestAttributeValueUseCase
    @Inject
    constructor(
        private val apiKeyStore: SecureApiKeyStore,
        private val provider: AttributeEnrichmentProvider,
        private val validator: ValidateEnrichmentResultUseCase,
    ) {
        suspend operator fun invoke(request: AttributeEnrichmentRequest): AttributeEnrichmentResult {
            if (!apiKeyStore.hasGeminiApiKey()) {
                return AttributeEnrichmentResult(
                    status = EnrichmentStatus.ERROR,
                    failureCode = EnrichmentFailureCode.MISSING_API_KEY,
                )
            }
            val result = provider.enrichAttribute(request)
            val validated = validator(result, request.targetAttributeType)
            if (result.status == EnrichmentStatus.FOUND &&
                validated.failureCode == EnrichmentFailureCode.VALIDATION_FAILED
            ) {
                EnrichmentLogger.rejected(
                    attributeKey = request.targetAttributeKey,
                    reason = "VALIDATION_FAILED",
                    originalStatus = result.status.name,
                    confidence = result.confidence?.name,
                )
            }
            return validated
        }
    }
