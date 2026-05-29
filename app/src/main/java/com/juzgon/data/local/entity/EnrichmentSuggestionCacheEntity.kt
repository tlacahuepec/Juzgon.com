@file:Suppress("MaxLineLength", "TooGenericExceptionCaught", "SwallowedException")

package com.juzgon.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.juzgon.domain.enrichment.EnrichmentCachedResult
import com.juzgon.domain.enrichment.EnrichmentConfidence
import com.juzgon.domain.enrichment.EnrichmentFailureCode
import com.juzgon.domain.enrichment.EnrichmentSource
import com.juzgon.domain.enrichment.EnrichmentStatus

@Entity(tableName = "enrichment_suggestion_cache")
data class EnrichmentSuggestionCacheEntity(
    @PrimaryKey val cacheKeyHash: String,
    val catalogId: String,
    val itemIdentity: String,
    val targetAttributeKey: String,
    val knownAttributesFingerprint: String,
    val status: String,
    val suggestedValue: String?,
    val displayValue: String?,
    val confidence: String?,
    val sources: String?, // JSON encoded
    val reason: String?,
    val failureCode: String?,
    val cachedAt: Long,
)

fun EnrichmentCachedResult.toEntity(): EnrichmentSuggestionCacheEntity {
    val cacheKeyHash =
        "${cacheKey.catalogId}|${cacheKey.itemIdentity}|${cacheKey.targetAttributeKey}|${cacheKey.knownAttributesFingerprint}"
            .hashCode()
    val sourcesJson =
        if (sources.isNotEmpty()) {
            sources.joinToString(",") { "{\"title\":\"${it.title}\",\"url\":\"${it.url}\",\"snippet\":\"${it.snippet}\"}" }
        } else {
            null
        }
    return EnrichmentSuggestionCacheEntity(
        cacheKeyHash = cacheKeyHash.toString(),
        catalogId = cacheKey.catalogId,
        itemIdentity = cacheKey.itemIdentity,
        targetAttributeKey = cacheKey.targetAttributeKey,
        knownAttributesFingerprint = cacheKey.knownAttributesFingerprint,
        status = status.name,
        suggestedValue = suggestedValue,
        displayValue = displayValue,
        confidence = confidence?.name,
        sources = sourcesJson,
        reason = reason,
        failureCode = failureCode?.name,
        cachedAt = cachedAt,
    )
}

fun EnrichmentSuggestionCacheEntity.toDomain(): EnrichmentCachedResult {
    val parsedSources =
        if (sources?.isNotEmpty() == true) {
            sources.split(",").mapNotNull { sourceJson ->
                try {
                    val titleMatch = "\"title\":\"([^\"]*)\"".toRegex().find(sourceJson)
                    val urlMatch = "\"url\":\"([^\"]*)\"".toRegex().find(sourceJson)
                    val snippetMatch = "\"snippet\":\"([^\"]*)\"".toRegex().find(sourceJson)
                    EnrichmentSource(
                        title = titleMatch?.groupValues?.get(1),
                        url = urlMatch?.groupValues?.get(1),
                        snippet = snippetMatch?.groupValues?.get(1),
                    )
                } catch (e: Exception) {
                    null
                }
            }
        } else {
            emptyList()
        }

    return EnrichmentCachedResult(
        cacheKey =
            com.juzgon.domain.enrichment.EnrichmentCacheKey(
                catalogId = catalogId,
                itemIdentity = itemIdentity,
                targetAttributeKey = targetAttributeKey,
                knownAttributesFingerprint = knownAttributesFingerprint,
            ),
        status = EnrichmentStatus.valueOf(status),
        suggestedValue = suggestedValue,
        displayValue = displayValue,
        confidence = confidence?.let { EnrichmentConfidence.valueOf(it) },
        sources = parsedSources,
        reason = reason,
        failureCode = failureCode?.let { EnrichmentFailureCode.valueOf(it) },
        cachedAt = cachedAt,
    )
}
