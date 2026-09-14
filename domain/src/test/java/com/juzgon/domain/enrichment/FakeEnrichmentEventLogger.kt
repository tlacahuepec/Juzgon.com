package com.juzgon.domain.enrichment

class FakeEnrichmentEventLogger : EnrichmentEventLogger {
    data class LogEntry(
        val type: String,
        val attributeKey: String,
        val extra: Map<String, String?> = emptyMap(),
    )

    val logs = mutableListOf<LogEntry>()

    override fun rejected(
        attributeKey: String,
        reason: String,
        originalStatus: String,
        confidence: String?,
    ) {
        logs.add(
            LogEntry(
                type = "rejected",
                attributeKey = attributeKey,
                extra = mapOf("reason" to reason, "originalStatus" to originalStatus, "confidence" to confidence),
            ),
        )
    }

    override fun accepted(
        attributeKey: String,
        itemId: String,
        suggestedValue: String,
    ) {
        logs.add(
            LogEntry(
                type = "accepted",
                attributeKey = attributeKey,
                extra = mapOf("itemId" to itemId, "suggestedValue" to suggestedValue),
            ),
        )
    }

    override fun dismissed(
        attributeKey: String,
        itemId: String,
    ) {
        logs.add(
            LogEntry(
                type = "dismissed",
                attributeKey = attributeKey,
                extra = mapOf("itemId" to itemId),
            ),
        )
    }
}
