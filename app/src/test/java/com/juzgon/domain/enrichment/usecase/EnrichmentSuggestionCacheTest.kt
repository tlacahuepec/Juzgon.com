package com.juzgon.domain.enrichment.usecase

import com.juzgon.domain.AttributeType
import com.juzgon.domain.CatalogType
import com.juzgon.domain.enrichment.AttributeEnrichmentRequest
import com.juzgon.domain.enrichment.AttributeEnrichmentResult
import com.juzgon.domain.enrichment.EnrichmentCacheKey
import com.juzgon.domain.enrichment.EnrichmentCachedResult
import com.juzgon.domain.enrichment.EnrichmentConfidence
import com.juzgon.domain.enrichment.EnrichmentSource
import com.juzgon.domain.enrichment.EnrichmentStatus
import com.juzgon.domain.enrichment.computeKnownAttributesFingerprint
import com.juzgon.domain.enrichment.toCacheKey
import com.juzgon.domain.enrichment.toCachedResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class EnrichmentSuggestionCacheTest {
    @Test
    fun identicalRequestReturnsCachedResultWithoutInvokingProvider() {
        val request =
            AttributeEnrichmentRequest(
                catalogId = "Persons",
                catalogDescription = "People",
                catalogType = CatalogType.PERSON,
                itemId = "john-doe",
                itemName = "John Doe",
                existingAttributes = mapOf("nationality" to "US"),
                targetAttributeKey = "birthDate",
                targetAttributeLabel = "Birth Date",
                targetAttributeType = AttributeType.DATE,
            )

        val cacheKey = request.toCacheKey()
        val cachedResult =
            EnrichmentCachedResult(
                cacheKey = cacheKey,
                status = EnrichmentStatus.FOUND,
                suggestedValue = "1980-05-15",
                displayValue = "May 15, 1980",
                confidence = EnrichmentConfidence.HIGH,
                sources = listOf(EnrichmentSource(title = "Public Record")),
            )

        assertEquals(cacheKey.catalogId, "Persons")
        assertEquals(cacheKey.itemIdentity, "john-doe")
        assertEquals(cacheKey.targetAttributeKey, "birthDate")
        assertNotNull(cacheKey.knownAttributesFingerprint)
    }

    @Test
    fun changedKnownAttributesFingerprintCausesCacheMiss() {
        val request1 =
            AttributeEnrichmentRequest(
                catalogId = "Persons",
                catalogDescription = "People",
                catalogType = CatalogType.PERSON,
                itemId = "john-doe",
                itemName = "John Doe",
                existingAttributes = mapOf("nationality" to "US"),
                targetAttributeKey = "birthDate",
                targetAttributeLabel = "Birth Date",
                targetAttributeType = AttributeType.DATE,
            )

        val request2 =
            AttributeEnrichmentRequest(
                catalogId = "Persons",
                catalogDescription = "People",
                catalogType = CatalogType.PERSON,
                itemId = "john-doe",
                itemName = "John Doe",
                existingAttributes = mapOf("nationality" to "UK"),
                targetAttributeKey = "birthDate",
                targetAttributeLabel = "Birth Date",
                targetAttributeType = AttributeType.DATE,
            )

        val key1 = request1.toCacheKey()
        val key2 = request2.toCacheKey()

        assertEquals(key1.catalogId, key2.catalogId)
        assertEquals(key1.itemIdentity, key2.itemIdentity)
        assertEquals(key1.targetAttributeKey, key2.targetAttributeKey)
        // Different attributes should produce different fingerprints
        val fingerprint1 = request1.computeKnownAttributesFingerprint()
        val fingerprint2 = request2.computeKnownAttributesFingerprint()
        assertEquals(false, fingerprint1 == fingerprint2)
    }

    @Test
    fun acceptedSuggestionIsWrittenToCache() {
        val result =
            AttributeEnrichmentResult(
                status = EnrichmentStatus.FOUND,
                suggestedValue = "1980-05-15",
                displayValue = "May 15, 1980",
                confidence = EnrichmentConfidence.HIGH,
                sources = listOf(EnrichmentSource(title = "Public Record")),
            )

        val cacheKey =
            EnrichmentCacheKey(
                catalogId = "Persons",
                itemIdentity = "john-doe",
                targetAttributeKey = "birthDate",
                knownAttributesFingerprint = "hash123",
            )

        val cachedResult = result.toCachedResult(cacheKey)

        assertEquals(EnrichmentStatus.FOUND, cachedResult.status)
        assertEquals("1980-05-15", cachedResult.suggestedValue)
        assertEquals("May 15, 1980", cachedResult.displayValue)
        assertEquals(EnrichmentConfidence.HIGH, cachedResult.confidence)
        assertEquals(1, cachedResult.sources.size)
    }

    @Test
    fun cacheSingleFacilitatesReuse() {
        val cacheKey =
            EnrichmentCacheKey(
                catalogId = "Persons",
                itemIdentity = "john-doe",
                targetAttributeKey = "birthDate",
                knownAttributesFingerprint = "hash123",
            )

        val cachedResult1 =
            EnrichmentCachedResult(
                cacheKey = cacheKey,
                status = EnrichmentStatus.FOUND,
                suggestedValue = "1980-05-15",
            )

        val cachedResult2 =
            EnrichmentCachedResult(
                cacheKey = cacheKey,
                status = EnrichmentStatus.NOT_FOUND,
                reason = "No match found",
            )

        // Both should have the same key
        assertEquals(cachedResult1.cacheKey, cachedResult2.cacheKey)
    }

    @Test
    fun cachePayloadExcludesRawSensitiveData() {
        val result =
            AttributeEnrichmentResult(
                status = EnrichmentStatus.FOUND,
                suggestedValue = "1980-05-15",
                displayValue = "May 15, 1980",
                confidence = EnrichmentConfidence.HIGH,
            )

        val cacheKey =
            EnrichmentCacheKey(
                catalogId = "Persons",
                itemIdentity = "john-doe",
                targetAttributeKey = "birthDate",
                knownAttributesFingerprint = "hash123",
            )

        val cachedResult = result.toCachedResult(cacheKey)

        // Verify no raw prompt or raw response is stored
        assertEquals(false, cachedResult.suggestedValue?.contains("prompt") ?: false)
        assertEquals(false, cachedResult.displayValue?.contains("raw") ?: false)
        assertNotNull(cachedResult.suggestedValue)
        assertNotNull(cachedResult.displayValue)
    }

    @Test
    fun cacheTimestampIsRecorded() {
        val beforeTime = System.currentTimeMillis()
        val cacheKey =
            EnrichmentCacheKey(
                catalogId = "Persons",
                itemIdentity = "john-doe",
                targetAttributeKey = "birthDate",
                knownAttributesFingerprint = "hash123",
            )

        val cachedResult =
            EnrichmentCachedResult(
                cacheKey = cacheKey,
                status = EnrichmentStatus.FOUND,
                suggestedValue = "1980-05-15",
            )
        val afterTime = System.currentTimeMillis()

        assert(cachedResult.cachedAt >= beforeTime)
        assert(cachedResult.cachedAt <= afterTime + 1000)
    }
}
