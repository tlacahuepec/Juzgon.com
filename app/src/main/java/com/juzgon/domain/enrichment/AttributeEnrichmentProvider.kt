package com.juzgon.domain.enrichment

interface AttributeEnrichmentProvider {
    suspend fun enrichAttribute(request: AttributeEnrichmentRequest): AttributeEnrichmentResult
}
