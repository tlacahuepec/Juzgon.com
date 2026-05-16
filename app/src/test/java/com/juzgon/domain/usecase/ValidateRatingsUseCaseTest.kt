package com.juzgon.domain.usecase

import org.junit.Assert.assertThrows
import org.junit.Test

class ValidateRatingsUseCaseTest {
    private val useCase = ValidateRatingsUseCase()

    @Test
    fun `lower-bound fail`() {
        assertThrows(IllegalArgumentException::class.java) {
            useCase(
                scoresByAttributeId = mapOf("quality" to 0),
                requiredAttributeBounds =
                    listOf(
                        ValidateRatingsUseCase.AttributeBounds(
                            attributeId = "quality",
                            minScore = 1,
                            maxScore = 10,
                        ),
                    ),
            )
        }
    }

    @Test
    fun `upper-bound fail`() {
        assertThrows(IllegalArgumentException::class.java) {
            useCase(
                scoresByAttributeId = mapOf("quality" to 9),
                requiredAttributeBounds =
                    listOf(
                        ValidateRatingsUseCase.AttributeBounds(
                            attributeId = "quality",
                            minScore = 1,
                            maxScore = 8,
                        ),
                    ),
            )
        }
    }

    @Test
    fun `missing attribute rating fail`() {
        assertThrows(IllegalArgumentException::class.java) {
            useCase(
                scoresByAttributeId = mapOf("value" to 7),
                requiredAttributeBounds =
                    listOf(
                        ValidateRatingsUseCase.AttributeBounds(
                            attributeId = "quality",
                            minScore = 1,
                            maxScore = 10,
                        ),
                    ),
            )
        }
    }

    @Test
    fun `all valid pass`() {
        useCase(
            scoresByAttributeId = mapOf("quality" to 9, "value" to 7),
            requiredAttributeBounds =
                listOf(
                    ValidateRatingsUseCase.AttributeBounds(
                        attributeId = "quality",
                        minScore = 1,
                        maxScore = 10,
                    ),
                    ValidateRatingsUseCase.AttributeBounds(
                        attributeId = "value",
                        minScore = 5,
                        maxScore = 8,
                    ),
                ),
        )
    }
}
