package com.juzgon.data.enrichment

import com.juzgon.domain.enrichment.EnrichmentEventLogger
import javax.inject.Inject

class TimberEnrichmentEventLogger
    @Inject
    constructor() : EnrichmentEventLogger {
        override fun rejected(
            attributeKey: String,
            reason: String,
            originalStatus: String,
            confidence: String?,
        ) {
            EnrichmentLogger.rejected(reason, originalStatus, confidence)
        }

        override fun accepted(
            attributeKey: String,
            itemId: String,
            suggestedValue: String,
        ) {
            EnrichmentLogger.accepted()
        }

        override fun dismissed(
            attributeKey: String,
            itemId: String,
        ) {
            EnrichmentLogger.dismissed()
        }
    }
