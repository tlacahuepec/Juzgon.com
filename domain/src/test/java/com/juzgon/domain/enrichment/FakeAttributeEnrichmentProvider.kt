package com.juzgon.domain.enrichment

class FakeAttributeEnrichmentProvider : AttributeEnrichmentProvider {
    var nextResult: AttributeEnrichmentResult =
        AttributeEnrichmentResult(
            status = EnrichmentStatus.NOT_FOUND,
        )
    var lastRequest: AttributeEnrichmentRequest? = null

    override suspend fun enrichAttribute(request: AttributeEnrichmentRequest): AttributeEnrichmentResult {
        lastRequest = request
        return nextResult
    }
}
