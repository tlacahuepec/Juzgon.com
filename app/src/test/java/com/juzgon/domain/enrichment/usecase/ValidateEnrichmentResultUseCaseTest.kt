package com.juzgon.domain.enrichment.usecase

import com.juzgon.domain.AttributeType
import com.juzgon.domain.enrichment.AttributeEnrichmentResult
import com.juzgon.domain.enrichment.EnrichmentConfidence
import com.juzgon.domain.enrichment.EnrichmentFailureCode
import com.juzgon.domain.enrichment.EnrichmentSource
import com.juzgon.domain.enrichment.EnrichmentStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class ValidateEnrichmentResultUseCaseTest {
    private val useCase = ValidateEnrichmentResultUseCase()

    @Test
    fun validDate_passes() {
        val result = foundDateResult("2000-01-15")

        val validated = useCase(result, AttributeType.DATE)

        assertEquals(EnrichmentStatus.FOUND, validated.status)
        assertEquals("2000-01-15", validated.suggestedValue)
    }

    @Test
    fun futureDate_isRejected() {
        val result = foundDateResult("2099-12-31")

        val validated = useCase(result, AttributeType.DATE)

        assertEquals(EnrichmentStatus.ERROR, validated.status)
        assertEquals(EnrichmentFailureCode.VALIDATION_FAILED, validated.failureCode)
    }

    @Test
    fun invalidDateFormat_isRejected() {
        val result = foundDateResult("15-01-2000")

        val validated = useCase(result, AttributeType.DATE)

        assertEquals(EnrichmentStatus.ERROR, validated.status)
        assertEquals(EnrichmentFailureCode.VALIDATION_FAILED, validated.failureCode)
    }

    @Test
    fun nonDateString_isRejected() {
        val result = foundDateResult("not-a-date")

        val validated = useCase(result, AttributeType.DATE)

        assertEquals(EnrichmentStatus.ERROR, validated.status)
        assertEquals(EnrichmentFailureCode.VALIDATION_FAILED, validated.failureCode)
    }

    @Test
    fun nullSuggestedValue_isRejected() {
        val result =
            AttributeEnrichmentResult(
                status = EnrichmentStatus.FOUND,
                suggestedValue = null,
                confidence = EnrichmentConfidence.HIGH,
            )

        val validated = useCase(result, AttributeType.DATE)

        assertEquals(EnrichmentStatus.ERROR, validated.status)
        assertEquals(EnrichmentFailureCode.VALIDATION_FAILED, validated.failureCode)
    }

    @Test
    fun lowConfidence_isRejected() {
        val result =
            AttributeEnrichmentResult(
                status = EnrichmentStatus.FOUND,
                suggestedValue = "2000-01-15",
                confidence = EnrichmentConfidence.LOW,
                sources = listOf(EnrichmentSource(title = "Source")),
            )

        val validated = useCase(result, AttributeType.DATE)

        assertEquals(EnrichmentStatus.ERROR, validated.status)
        assertEquals(EnrichmentFailureCode.VALIDATION_FAILED, validated.failureCode)
    }

    @Test
    fun nonFoundStatus_isPassedThrough() {
        val result =
            AttributeEnrichmentResult(
                status = EnrichmentStatus.NOT_FOUND,
                reason = "No sources found",
            )

        val validated = useCase(result, AttributeType.DATE)

        assertEquals(result, validated)
    }

    @Test
    fun errorStatus_isPassedThrough() {
        val result =
            AttributeEnrichmentResult(
                status = EnrichmentStatus.ERROR,
                failureCode = EnrichmentFailureCode.NETWORK_ERROR,
            )

        val validated = useCase(result, AttributeType.DATE)

        assertEquals(result, validated)
    }

    @Test
    fun conflictStatus_isPassedThrough() {
        val result =
            AttributeEnrichmentResult(
                status = EnrichmentStatus.CONFLICT,
                reason = "Conflicting sources",
            )

        val validated = useCase(result, AttributeType.DATE)

        assertEquals(result, validated)
    }

    @Test
    fun nonDateAttributeType_passesWithoutDateValidation() {
        val result =
            AttributeEnrichmentResult(
                status = EnrichmentStatus.FOUND,
                suggestedValue = "some-non-date-value",
                confidence = EnrichmentConfidence.HIGH,
            )

        val validated = useCase(result, AttributeType.NUMBER)

        assertEquals(EnrichmentStatus.FOUND, validated.status)
        assertEquals("some-non-date-value", validated.suggestedValue)
    }

    @Test
    fun mediumConfidence_passes() {
        val result = foundDateResult("1990-06-15", EnrichmentConfidence.MEDIUM)

        val validated = useCase(result, AttributeType.DATE)

        assertEquals(EnrichmentStatus.FOUND, validated.status)
    }

    private fun foundDateResult(
        value: String,
        confidence: EnrichmentConfidence = EnrichmentConfidence.HIGH,
    ) = AttributeEnrichmentResult(
        status = EnrichmentStatus.FOUND,
        suggestedValue = value,
        displayValue = value,
        confidence = confidence,
        sources = listOf(EnrichmentSource(title = "Wikipedia")),
    )
}
