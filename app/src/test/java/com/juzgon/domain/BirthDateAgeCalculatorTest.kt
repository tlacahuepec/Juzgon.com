package com.juzgon.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class BirthDateAgeCalculatorTest {
    private val fixedToday = LocalDate.of(2026, 6, 1)
    private val clock = AppClock { fixedToday }
    private val calculator = BirthDateAgeCalculator(clock)

    @Test
    fun `valid birth date returns correct age`() {
        assertEquals(36, calculator.calculateAge("1990-05-27"))
    }

    @Test
    fun `birthday not yet passed this year returns previous age`() {
        assertEquals(35, calculator.calculateAge("1990-12-01"))
    }

    @Test
    fun `birthday today returns exact age`() {
        assertEquals(36, calculator.calculateAge("1990-06-01"))
    }

    @Test
    fun `date in future returns 0`() {
        assertEquals(0, calculator.calculateAge("2027-01-01"))
    }

    @Test
    fun `date today returns 0`() {
        assertEquals(0, calculator.calculateAge("2026-06-01"))
    }

    @Test
    fun `invalid date string returns null`() {
        assertNull(calculator.calculateAge("not-a-date"))
    }

    @Test
    fun `empty string returns null`() {
        assertNull(calculator.calculateAge(""))
    }

    @Test
    fun `partial date returns null`() {
        assertNull(calculator.calculateAge("1990-05"))
    }
}
