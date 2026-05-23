package com.juzgon.data.schema

import com.juzgon.domain.schema.DataSchemaVersion
import com.juzgon.domain.schema.SchemaCompatibility
import org.junit.Assert.assertEquals
import org.junit.Test

class DefaultSchemaCompatibilityCheckerTest {
    private val checker = DefaultSchemaCompatibilityChecker()

    @Test
    fun `current version is compatible`() {
        val result = checker.check(DataSchemaVersion.CURRENT)
        assertEquals(SchemaCompatibility.Status.COMPATIBLE, result.status)
    }

    @Test
    fun `supported older version is compatible`() {
        for (version in DataSchemaVersion.SUPPORTED_READ_VERSIONS) {
            val result = checker.check(version)
            assertEquals(
                "Version $version should be compatible",
                SchemaCompatibility.Status.COMPATIBLE,
                result.status,
            )
        }
    }

    @Test
    fun `newer unsupported version returns warning`() {
        val futureVersion = DataSchemaVersion.CURRENT + 1
        val result = checker.check(futureVersion)
        assertEquals(SchemaCompatibility.Status.NEWER_DATA_WARNING, result.status)
    }

    @Test
    fun `much newer version returns warning`() {
        val futureVersion = DataSchemaVersion.CURRENT + 10
        val result = checker.check(futureVersion)
        assertEquals(SchemaCompatibility.Status.NEWER_DATA_WARNING, result.status)
    }

    @Test
    fun `zero version is compatible for empty or new state`() {
        val result = checker.check(0)
        assertEquals(SchemaCompatibility.Status.COMPATIBLE, result.status)
    }

    @Test
    fun `result contains stored version`() {
        val result = checker.check(5)
        assertEquals(5, result.storedVersion)
    }

    @Test
    fun `result contains app version`() {
        val result = checker.check(5)
        assertEquals(DataSchemaVersion.CURRENT, result.appVersion)
    }
}
