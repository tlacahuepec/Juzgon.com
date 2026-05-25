package com.juzgon.data.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupAttributeIdNormalizerTest {
    @Test
    fun normalize_fullIdPassesThrough() {
        val result = BackupAttributeIdNormalizer.normalize("Food/Taste", "Food")

        assertTrue(result is NormalizedAttributeId.Resolved)
        assertEquals("Food/Taste", (result as NormalizedAttributeId.Resolved).id)
    }

    @Test
    fun normalize_shortIdPrefixedWithCategory() {
        val result = BackupAttributeIdNormalizer.normalize("Taste", "Food")

        assertTrue(result is NormalizedAttributeId.Resolved)
        assertEquals("Food/Taste", (result as NormalizedAttributeId.Resolved).id)
    }

    @Test
    fun normalize_shortIdWithNullCategoryReturnsInvalid() {
        val result = BackupAttributeIdNormalizer.normalize("Taste", null)

        assertTrue(result is NormalizedAttributeId.Invalid)
    }

    @Test
    fun normalize_shortIdWithEmptyCategoryReturnsInvalid() {
        val result = BackupAttributeIdNormalizer.normalize("Taste", "")

        assertTrue(result is NormalizedAttributeId.Invalid)
    }

    @Test
    fun normalize_fullIdIgnoresCategory() {
        val result = BackupAttributeIdNormalizer.normalize("Cars/Speed", "Food")

        assertTrue(result is NormalizedAttributeId.Resolved)
        assertEquals("Cars/Speed", (result as NormalizedAttributeId.Resolved).id)
    }
}
