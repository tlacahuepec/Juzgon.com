package com.juzgon.data.enrichment

import timber.log.Timber

object EnrichmentLogger {
    private const val TAG = "JuzgonEnrichment"

    fun started(
        provider: String,
        attributeKey: String,
        catalogType: String?,
    ) {
        Timber.tag(TAG).d(
            "Enrichment started provider=%s attribute=%s catalogType=%s",
            provider,
            attributeKey,
            catalogType,
        )
    }

    fun succeeded(
        provider: String,
        attributeKey: String,
        confidence: String,
        sourceCount: Int,
        durationMs: Long,
    ) {
        Timber.tag(TAG).d(
            "Enrichment succeeded provider=%s attribute=%s confidence=%s sourceCount=%d durationMs=%d",
            provider,
            attributeKey,
            confidence,
            sourceCount,
            durationMs,
        )
    }

    fun failed(
        provider: String,
        attributeKey: String,
        failureCode: String,
        durationMs: Long,
        errorDetail: String? = null,
    ) {
        Timber.tag(TAG).w(
            "Enrichment failed provider=%s attribute=%s failureCode=%s durationMs=%d detail=%s",
            provider,
            attributeKey,
            failureCode,
            durationMs,
            errorDetail ?: "none",
        )
    }

    fun rejected(
        attributeKey: String,
        reason: String,
        originalStatus: String,
        confidence: String?,
    ) {
        Timber.tag(TAG).w(
            "Enrichment rejected attribute=%s reason=%s status=%s confidence=%s",
            attributeKey,
            reason,
            originalStatus,
            confidence ?: "UNKNOWN",
        )
    }

    fun accepted(
        attributeKey: String,
        itemId: String,
        suggestedValue: String,
    ) {
        Timber.tag(TAG).d(
            "Enrichment accepted attribute=%s itemId=%s suggestedValue=%s",
            attributeKey,
            itemId,
            suggestedValue,
        )
    }

    fun dismissed(
        attributeKey: String,
        itemId: String,
    ) {
        Timber.tag(TAG).d(
            "Enrichment dismissed attribute=%s itemId=%s",
            attributeKey,
            itemId,
        )
    }
}
