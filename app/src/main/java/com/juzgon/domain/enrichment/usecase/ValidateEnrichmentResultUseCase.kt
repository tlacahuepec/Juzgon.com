@file:Suppress("ReturnCount")

package com.juzgon.domain.enrichment.usecase

import com.juzgon.domain.AttributeType
import com.juzgon.domain.enrichment.AttributeEnrichmentResult
import com.juzgon.domain.enrichment.EnrichmentConfidence
import com.juzgon.domain.enrichment.EnrichmentFailureCode
import com.juzgon.domain.enrichment.EnrichmentStatus
import java.time.LocalDate
import java.time.format.DateTimeParseException
import javax.inject.Inject

class ValidateEnrichmentResultUseCase
    @Inject
    constructor() {
        operator fun invoke(
            result: AttributeEnrichmentResult,
            attributeType: AttributeType,
        ): AttributeEnrichmentResult {
            if (result.status != EnrichmentStatus.FOUND) return result
            if (result.confidence == EnrichmentConfidence.LOW) {
                return result.copy(
                    status = EnrichmentStatus.ERROR,
                    failureCode = EnrichmentFailureCode.VALIDATION_FAILED,
                )
            }
            return when (attributeType) {
                AttributeType.DATE -> validateDate(result)
                else -> result
            }
        }

        private fun validateDate(result: AttributeEnrichmentResult): AttributeEnrichmentResult {
            val value = result.suggestedValue ?: return invalidResult(result)
            val date =
                try {
                    LocalDate.parse(value)
                } catch (_: DateTimeParseException) {
                    return invalidResult(result)
                }
            if (date.isAfter(LocalDate.now())) return invalidResult(result)
            return result
        }

        private fun invalidResult(result: AttributeEnrichmentResult) =
            result.copy(
                status = EnrichmentStatus.ERROR,
                failureCode = EnrichmentFailureCode.VALIDATION_FAILED,
            )
    }
