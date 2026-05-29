package com.juzgon.domain.enrichment

data class EnrichmentCacheKey(
    val catalogId: String,
    val itemIdentity: String,
    val targetAttributeKey: String,
    val knownAttributesFingerprint: String,
)

data class EnrichmentCachedResult(
    val cacheKey: EnrichmentCacheKey,
    val status: EnrichmentStatus,
    val suggestedValue: String? = null,
    val displayValue: String? = null,
    val confidence: EnrichmentConfidence? = null,
    val sources: List<EnrichmentSource> = emptyList(),
    val reason: String? = null,
    val failureCode: EnrichmentFailureCode? = null,
    val cachedAt: Long = System.currentTimeMillis(),
)

interface EnrichmentSuggestionCacheRepository {
    suspend fun get(key: EnrichmentCacheKey): EnrichmentCachedResult?

    suspend fun put(result: EnrichmentCachedResult)

    suspend fun clear()
}

fun AttributeEnrichmentRequest.computeKnownAttributesFingerprint(): String {
    val sorted = existingAttributes.toSortedMap()
    return sorted.entries
        .joinToString("|") { (k, v) -> "$k=$v" }
        .hashCode()
        .toString()
}

fun AttributeEnrichmentRequest.toCacheKey(): EnrichmentCacheKey =
    EnrichmentCacheKey(
        catalogId = catalogId,
        itemIdentity = itemId,
        targetAttributeKey = targetAttributeKey,
        knownAttributesFingerprint = computeKnownAttributesFingerprint(),
    )

fun AttributeEnrichmentResult.toCachedResult(key: EnrichmentCacheKey): EnrichmentCachedResult =
    EnrichmentCachedResult(
        cacheKey = key,
        status = status,
        suggestedValue = suggestedValue,
        displayValue = displayValue,
        confidence = confidence,
        sources = sources,
        reason = reason,
        failureCode = failureCode,
    )
