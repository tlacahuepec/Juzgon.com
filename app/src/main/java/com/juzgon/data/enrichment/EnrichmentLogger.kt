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
    ) {
        Timber.tag(TAG).w(
            "Enrichment failed provider=%s attribute=%s failureCode=%s durationMs=%d",
            provider,
            attributeKey,
            failureCode,
            durationMs,
        )
    }
}
