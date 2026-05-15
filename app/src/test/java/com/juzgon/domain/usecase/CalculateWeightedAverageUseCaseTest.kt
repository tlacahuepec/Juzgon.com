package com.juzgon.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class CalculateWeightedAverageUseCaseTest {
    private val useCase = CalculateWeightedAverageUseCase()

    @Test
    fun `returns weighted average for happy path`() {
        val result = useCase(
            values = listOf(8.0, 6.0, 10.0),
            weights = listOf(1.0, 2.0, 3.0),
        )

        assertEquals(8.33, result, 0.0)
    }

    @Test
    fun `returns weighted average for mixed weights`() {
        val result = useCase(
            values = listOf(7.0, 9.0, 5.0),
            weights = listOf(0.0, 1.5, 0.5),
        )

        assertEquals(8.0, result, 0.0)
    }

    @Test
    fun `returns zero when total weight is zero`() {
        val result = useCase(
            values = listOf(7.0, 9.0, 5.0),
            weights = listOf(0.0, 0.0, 0.0),
        )

        assertEquals(0.0, result, 0.0)
    }

    @Test
    fun `throws when value is out of range`() {
        assertThrows(IllegalArgumentException::class.java) {
            useCase(
                values = listOf(0.5, 9.0),
                weights = listOf(1.0, 1.0),
            )
        }
    }
}
