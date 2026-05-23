package com.juzgon.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class DateScoreCalculatorTest {
    private val fixedToday = LocalDate.of(2026, 5, 23)
    private val clock = AppClock { fixedToday }
    private val calculator = DateScoreCalculator(clock)

    @Test
    fun `today scores 10 for NEWER_IS_BETTER`() {
        assertEquals(10, calculator.calculate("2026-05-23", ScoringDirection.NEWER_IS_BETTER))
    }

    @Test
    fun `1 year ago scores 10 for NEWER_IS_BETTER`() {
        assertEquals(10, calculator.calculate("2025-05-23", ScoringDirection.NEWER_IS_BETTER))
    }

    @Test
    fun `5 years ago scores 9 for NEWER_IS_BETTER`() {
        assertEquals(9, calculator.calculate("2021-05-23", ScoringDirection.NEWER_IS_BETTER))
    }

    @Test
    fun `10 years ago scores 8 for NEWER_IS_BETTER`() {
        assertEquals(8, calculator.calculate("2016-05-23", ScoringDirection.NEWER_IS_BETTER))
    }

    @Test
    fun `45 years ago scores 1 for NEWER_IS_BETTER clamped`() {
        assertEquals(1, calculator.calculate("1981-05-23", ScoringDirection.NEWER_IS_BETTER))
    }

    @Test
    fun `50 years ago scores 1 for NEWER_IS_BETTER clamped`() {
        assertEquals(1, calculator.calculate("1976-05-23", ScoringDirection.NEWER_IS_BETTER))
    }

    @Test
    fun `today scores 1 for OLDER_IS_BETTER`() {
        assertEquals(1, calculator.calculate("2026-05-23", ScoringDirection.OLDER_IS_BETTER))
    }

    @Test
    fun `5 years ago scores 2 for OLDER_IS_BETTER`() {
        assertEquals(2, calculator.calculate("2021-05-23", ScoringDirection.OLDER_IS_BETTER))
    }

    @Test
    fun `10 years ago scores 3 for OLDER_IS_BETTER`() {
        assertEquals(3, calculator.calculate("2016-05-23", ScoringDirection.OLDER_IS_BETTER))
    }

    @Test
    fun `45 years ago scores 10 for OLDER_IS_BETTER clamped`() {
        assertEquals(10, calculator.calculate("1981-05-23", ScoringDirection.OLDER_IS_BETTER))
    }

    @Test
    fun `50 years ago scores 10 for OLDER_IS_BETTER clamped`() {
        assertEquals(10, calculator.calculate("1976-05-23", ScoringDirection.OLDER_IS_BETTER))
    }

    @Test
    fun `future date scores 10 for NEWER_IS_BETTER`() {
        assertEquals(10, calculator.calculate("2027-01-01", ScoringDirection.NEWER_IS_BETTER))
    }

    @Test
    fun `future date scores 1 for OLDER_IS_BETTER`() {
        assertEquals(1, calculator.calculate("2027-01-01", ScoringDirection.OLDER_IS_BETTER))
    }

    @Test
    fun `invalid date string returns null`() {
        assertNull(calculator.calculate("not-a-date", ScoringDirection.NEWER_IS_BETTER))
    }

    @Test
    fun `empty string returns null`() {
        assertNull(calculator.calculate("", ScoringDirection.NEWER_IS_BETTER))
    }

    @Test
    fun `partial date returns null`() {
        assertNull(calculator.calculate("2026-05", ScoringDirection.NEWER_IS_BETTER))
    }
}
