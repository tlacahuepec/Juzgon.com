package com.juzgon.domain.enrichment

interface EnrichmentEventLogger {
    fun rejected(
        attributeKey: String,
        reason: String,
        originalStatus: String,
        confidence: String?,
    )

    fun accepted(
        attributeKey: String,
        itemId: String,
        suggestedValue: String,
    )

    fun dismissed(
        attributeKey: String,
        itemId: String,
    )
}
