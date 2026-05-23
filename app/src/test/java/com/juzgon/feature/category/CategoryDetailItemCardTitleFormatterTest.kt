package com.juzgon.feature.category

import org.junit.Assert.assertEquals
import org.junit.Test

class CategoryDetailItemCardTitleFormatterTest {
    @Test
    fun splitItemCardTitle_returnsFirstWordAndRemainingText() {
        val parts = CategoryDetailItemCardTitleFormatter.split("Grand Touring Sedan")

        assertEquals("Grand", parts.primaryWord)
        assertEquals("Touring Sedan", parts.remainingTitle)
    }

    @Test
    fun splitItemCardTitle_handlesSingleWordTitle() {
        val parts = CategoryDetailItemCardTitleFormatter.split("Roadster")

        assertEquals("Roadster", parts.primaryWord)
        assertEquals(null, parts.remainingTitle)
    }

    @Test
    fun splitItemCardTitle_trimsExtraSpaces() {
        val parts = CategoryDetailItemCardTitleFormatter.split("  Alpha    Beta   Gamma  ")

        assertEquals("Alpha", parts.primaryWord)
        assertEquals("Beta Gamma", parts.remainingTitle)
    }
}
