package com.juzgon.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class ScorePresentationAdapterTest {
    private val adapter = ScorePresentationAdapter()

    // --- 0-10 scale (default) ---

    @Test
    fun formatScore_minValue_returnsOneOnTen() {
        assertEquals("1", adapter.format(1))
    }

    @Test
    fun formatScore_maxValue_returnsTenOnTen() {
        assertEquals("10", adapter.format(10))
    }

    @Test
    fun formatScore_midValue_returnsFiveOnTen() {
        assertEquals("5", adapter.format(5))
    }

    // --- 0-100 scale ---

    @Test
    fun toHundredScale_minScore_returnsTen() {
        assertEquals(10, adapter.toHundredScale(1))
    }

    @Test
    fun toHundredScale_maxScore_returnsHundred() {
        assertEquals(100, adapter.toHundredScale(10))
    }

    @Test
    fun toHundredScale_midScore_returnsExpectedMapping() {
        assertEquals(50, adapter.toHundredScale(5))
    }

    @Test
    fun toHundredScale_score3_returnsThirty() {
        assertEquals(30, adapter.toHundredScale(3))
    }

    @Test
    fun formatHundredScale_minScore_returnsTen() {
        assertEquals("10", adapter.formatHundredScale(1))
    }

    @Test
    fun formatHundredScale_maxScore_returnsHundred() {
        assertEquals("100", adapter.formatHundredScale(10))
    }

    // --- double average formatting ---

    @Test
    fun formatAverage_wholeNumber_returnsOneDecimal() {
        assertEquals("7.0", adapter.formatAverage(7.0))
    }

    @Test
    fun formatAverage_roundsHalfUp() {
        assertEquals("7.5", adapter.formatAverage(7.45))
    }

    @Test
    fun formatAverage_truncatesLongDecimal() {
        assertEquals("6.7", adapter.formatAverage(6.666))
    }

    @Test
    fun formatAverage_minBoundary_returnsOneDecimal() {
        assertEquals("1.0", adapter.formatAverage(1.0))
    }

    @Test
    fun formatAverage_maxBoundary_returnsTenDecimal() {
        assertEquals("10.0", adapter.formatAverage(10.0))
    }
}
