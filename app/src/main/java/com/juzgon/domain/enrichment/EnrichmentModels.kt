package com.juzgon.domain.enrichment

import com.juzgon.domain.AttributeType
import com.juzgon.domain.CatalogType

data class AttributeEnrichmentRequest(
    val catalogId: String,
    val catalogDescription: String?,
    val catalogType: CatalogType?,
    val itemId: String,
    val itemName: String,
    val existingAttributes: Map<String, String>,
    val targetAttributeKey: String,
    val targetAttributeLabel: String,
    val targetAttributeType: AttributeType,
)

data class AttributeEnrichmentResult(
    val status: EnrichmentStatus,
    val suggestedValue: String? = null,
    val displayValue: String? = null,
    val confidence: EnrichmentConfidence? = null,
    val sources: List<EnrichmentSource> = emptyList(),
    val reason: String? = null,
    val failureCode: EnrichmentFailureCode? = null,
)

data class EnrichmentSource(
    val title: String? = null,
    val url: String? = null,
    val snippet: String? = null,
)

enum class EnrichmentStatus { FOUND, NOT_FOUND, CONFLICT, ERROR }

enum class EnrichmentConfidence { HIGH, MEDIUM, LOW }

enum class EnrichmentFailureCode {
    MISSING_API_KEY,
    INVALID_API_KEY,
    NETWORK_ERROR,
    RATE_LIMITED,
    QUOTA_EXCEEDED,
    PROVIDER_ERROR,
    INVALID_RESPONSE_FORMAT,
    VALIDATION_FAILED,
    NO_RELIABLE_SOURCE,
    CONFLICTING_SOURCES,
}
