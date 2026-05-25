package com.juzgon.domain.enrichment

class FakeAttributeEnrichmentProvider : AttributeEnrichmentProvider {
    var nextResult: AttributeEnrichmentResult =
        AttributeEnrichmentResult(
            status = EnrichmentStatus.NOT_FOUND,
        )

    override suspend fun enrichAttribute(request: AttributeEnrichmentRequest): AttributeEnrichmentResult = nextResult
}
