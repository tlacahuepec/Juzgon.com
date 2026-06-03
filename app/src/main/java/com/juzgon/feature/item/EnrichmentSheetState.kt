package com.juzgon.feature.item

import com.juzgon.domain.enrichment.EnrichmentCandidateValue
import com.juzgon.domain.enrichment.EnrichmentConfidence
import com.juzgon.domain.enrichment.EnrichmentFailureCode
import com.juzgon.domain.enrichment.EnrichmentSource

sealed interface EnrichmentSheetState {
    data object Hidden : EnrichmentSheetState

    data object Loading : EnrichmentSheetState

    data object NoKey : EnrichmentSheetState

    data class Found(
        val attributeId: String,
        val attributeLabel: String,
        val suggestedValue: String,
        val displayValue: String?,
        val confidence: EnrichmentConfidence?,
        val sources: List<EnrichmentSource>,
    ) : EnrichmentSheetState

    data class NotFound(
        val reason: String?,
    ) : EnrichmentSheetState

    data class Conflict(
        val attributeId: String,
        val attributeLabel: String,
        val reason: String?,
        val sources: List<EnrichmentSource>,
        val candidateValues: List<EnrichmentCandidateValue>,
    ) : EnrichmentSheetState

    data class Error(
        val failureCode: EnrichmentFailureCode?,
        val reason: String?,
    ) : EnrichmentSheetState
}

fun EnrichmentSheetState.canRetry(
    retryAttemptsUsed: Int,
    maxRetryAttempts: Int,
): Boolean =
    when (this) {
        is EnrichmentSheetState.NotFound -> retryAttemptsUsed < maxRetryAttempts
        is EnrichmentSheetState.Conflict -> retryAttemptsUsed < maxRetryAttempts
        is EnrichmentSheetState.Error -> retryAttemptsUsed < maxRetryAttempts
        else -> false
    }
