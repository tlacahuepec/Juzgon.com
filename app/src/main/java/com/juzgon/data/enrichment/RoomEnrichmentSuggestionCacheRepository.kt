package com.juzgon.data.enrichment

import com.juzgon.data.local.dao.EnrichmentSuggestionCacheDao
import com.juzgon.data.local.entity.toDomain
import com.juzgon.data.local.entity.toEntity
import com.juzgon.domain.enrichment.EnrichmentCacheKey
import com.juzgon.domain.enrichment.EnrichmentCachedResult
import com.juzgon.domain.enrichment.EnrichmentSuggestionCacheRepository
import javax.inject.Inject

class RoomEnrichmentSuggestionCacheRepository
    @Inject
    constructor(
        private val dao: EnrichmentSuggestionCacheDao,
    ) : EnrichmentSuggestionCacheRepository {
        override suspend fun get(key: EnrichmentCacheKey): EnrichmentCachedResult? =
            dao
                .get(
                    catalogId = key.catalogId,
                    itemIdentity = key.itemIdentity,
                    targetAttributeKey = key.targetAttributeKey,
                    knownAttributesFingerprint = key.knownAttributesFingerprint,
                )?.toDomain()

        override suspend fun put(result: EnrichmentCachedResult) {
            dao.put(result.toEntity())
        }

        override suspend fun clear() {
            dao.clear()
        }
    }
