package com.juzgon.data.enrichment

import timber.log.Timber

object EnrichmentLogger {
    private const val TAG = "JuzgonEnrichment"

    fun started(provider: String) {
        Timber.tag(TAG).d(
            "Enrichment started provider=%s",
            provider,
        )
    }

    fun promptSent(
        provider: String,
        prompt: String,
    ) {
        Timber.tag(TAG).d(
            "Enrichment prompt prepared provider=%s length=%d",
            provider,
            prompt.length,
        )
    }

    fun responseReceived(
        provider: String,
        responseText: String,
    ) {
        Timber.tag(TAG).d(
            "Enrichment response received provider=%s length=%d",
            provider,
            responseText.length,
        )
    }

    fun succeeded(
        provider: String,
        confidence: String,
        sourceCount: Int,
        durationMs: Long,
    ) {
        Timber.tag(TAG).d(
            "Enrichment succeeded provider=%s confidence=%s sourceCount=%d durationMs=%d",
            provider,
            confidence,
            sourceCount,
            durationMs,
        )
    }

    fun failed(
        provider: String,
        failureCode: String,
        durationMs: Long,
    ) {
        Timber.tag(TAG).w(
            "Enrichment failed provider=%s failureCode=%s durationMs=%d",
            provider,
            failureCode,
            durationMs,
        )
    }

    fun rejected(
        reason: String,
        originalStatus: String,
        confidence: String?,
    ) {
        Timber.tag(TAG).w(
            "Enrichment rejected reason=%s status=%s confidence=%s",
            reason,
            originalStatus,
            confidence ?: "UNKNOWN",
        )
    }

    fun accepted() {
        Timber.tag(TAG).d(
            "Enrichment suggestion accepted",
        )
    }

    fun dismissed() {
        Timber.tag(TAG).d(
            "Enrichment suggestion dismissed",
        )
    }
}
