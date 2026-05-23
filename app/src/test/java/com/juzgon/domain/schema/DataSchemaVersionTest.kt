package com.juzgon.domain.schema

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DataSchemaVersionTest {
    @Test
    fun `current version is a positive integer`() {
        assertTrue(
            "DATA_SCHEMA_VERSION must be positive",
            DataSchemaVersion.CURRENT > 0,
        )
    }

    @Test
    fun `supported read versions contains current version`() {
        assertTrue(
            "SUPPORTED_READ_VERSIONS must include CURRENT",
            DataSchemaVersion.SUPPORTED_READ_VERSIONS.contains(DataSchemaVersion.CURRENT),
        )
    }

    @Test
    fun `current version starts at 1`() {
        assertEquals(1, DataSchemaVersion.CURRENT)
    }
}
