package com.juzgon.domain.enrichment

class FakeEnrichmentSuggestionCacheRepository : EnrichmentSuggestionCacheRepository {
    val store = mutableMapOf<EnrichmentCacheKey, EnrichmentCachedResult>()
    var lastGetKey: EnrichmentCacheKey? = null
    var lastPutResult: EnrichmentCachedResult? = null
    var clearCalled = false

    override suspend fun get(key: EnrichmentCacheKey): EnrichmentCachedResult? {
        lastGetKey = key
        return store[key]
    }

    override suspend fun put(result: EnrichmentCachedResult) {
        lastPutResult = result
        store[result.cacheKey] = result
    }

    override suspend fun clear() {
        clearCalled = true
        store.clear()
    }

    fun reset() {
        store.clear()
        lastGetKey = null
        lastPutResult = null
        clearCalled = false
    }
}
