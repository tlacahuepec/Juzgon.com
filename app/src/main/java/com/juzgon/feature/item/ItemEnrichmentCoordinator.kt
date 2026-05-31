package com.juzgon.feature.item

import com.juzgon.domain.Category
import com.juzgon.domain.enrichment.AttributeEnrichmentRequest
import com.juzgon.domain.enrichment.AttributeEnrichmentResult
import com.juzgon.domain.enrichment.EnrichmentEventLogger
import com.juzgon.domain.enrichment.EnrichmentFailureCode
import com.juzgon.domain.enrichment.EnrichmentStatus
import com.juzgon.domain.enrichment.usecase.SuggestAttributeValueUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Extracted coordinator for the enrichment suggestion flow.
 * This removes a significant number of responsibilities (and methods) from ItemFormViewModel.
 */
class ItemEnrichmentCoordinator(
    private val suggestAttributeValueUseCase: SuggestAttributeValueUseCase,
    private val eventLogger: EnrichmentEventLogger,
    private val scope: CoroutineScope,
    private val maxRetryAttempts: Int = 2,
) {
    var lastRequest: AttributeEnrichmentRequest? = null
        private set

    var retryAttemptsUsed: Int = 0
        private set

    fun requestSuggestion(
        category: Category,
        currentState: ItemFormUiState,
        targetAttributeId: String,
        onResult: (EnrichmentSheetState) -> Unit,
    ) {
        val targetAttribute =
            currentState.values
                .firstOrNull { it.attribute.id == targetAttributeId }
                ?.attribute ?: return

        val request =
            AttributeEnrichmentRequest(
                catalogId = category.name,
                catalogDescription = category.description,
                catalogType = category.type,
                itemId = currentState.originalItemId ?: currentState.title.trim(),
                itemName = currentState.title.trim(),
                existingAttributes =
                    currentState.values
                        .filter { it.valueText.isNotBlank() && it.attribute.id != targetAttributeId }
                        .associate { it.attribute.displayName to it.valueText },
                targetAttributeKey = targetAttributeId,
                targetAttributeLabel = targetAttribute.displayName,
                targetAttributeType = targetAttribute.type,
            )

        lastRequest = request
        retryAttemptsUsed = 0

        onResult(EnrichmentSheetState.Loading)

        scope.launch {
            val result = suggestAttributeValueUseCase(request)
            onResult(result.toSheetState(targetAttributeId, targetAttribute.displayName))
        }
    }

    fun acceptSuggestion(
        currentSheet: EnrichmentSheetState,
        onValueChanged: (String, String) -> Unit,
        onSheetHidden: () -> Unit,
    ) {
        if (currentSheet is EnrichmentSheetState.Found) {
            eventLogger.accepted(
                attributeKey = currentSheet.attributeId,
                itemId = lastRequest?.itemId ?: "",
                suggestedValue = currentSheet.suggestedValue,
            )
            onValueChanged(currentSheet.attributeId, currentSheet.suggestedValue)
            onSheetHidden()
        }
    }

    fun dismissSuggestion(currentSheet: EnrichmentSheetState) {
        if (currentSheet is EnrichmentSheetState.Found) {
            eventLogger.dismissed(
                attributeKey = currentSheet.attributeId,
                itemId = lastRequest?.itemId ?: "",
            )
        }
    }

    fun retry(
        currentState: ItemFormUiState,
        onResult: (EnrichmentSheetState) -> Unit,
    ) {
        val request = lastRequest ?: return
        val currentSheet = currentState.enrichmentSheet

        if (!currentSheet.canRetry(retryAttemptsUsed, maxRetryAttempts)) {
            return
        }

        onResult(EnrichmentSheetState.Loading)

        scope.launch {
            val result = suggestAttributeValueUseCase(request, bypassCache = true)
            retryAttemptsUsed += 1
            onResult(result.toSheetState(request.targetAttributeKey, request.targetAttributeLabel))
        }
    }

    fun reset() {
        lastRequest = null
        retryAttemptsUsed = 0
    }

    private fun AttributeEnrichmentResult.toSheetState(
        attributeId: String,
        attributeLabel: String,
    ): EnrichmentSheetState =
        when {
            status == EnrichmentStatus.ERROR &&
                failureCode == EnrichmentFailureCode.MISSING_API_KEY ->
                EnrichmentSheetState.NoKey

            status == EnrichmentStatus.FOUND ->
                EnrichmentSheetState.Found(
                    attributeId = attributeId,
                    attributeLabel = attributeLabel,
                    suggestedValue = suggestedValue.orEmpty(),
                    displayValue = displayValue,
                    confidence = confidence,
                    sources = sources,
                )

            status == EnrichmentStatus.NOT_FOUND ->
                EnrichmentSheetState.NotFound(reason)

            status == EnrichmentStatus.CONFLICT ->
                EnrichmentSheetState.Conflict(reason, sources)

            else ->
                EnrichmentSheetState.Error(failureCode, reason)
        }
}
