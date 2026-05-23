package com.juzgon.data.schema

import com.juzgon.domain.schema.DataSchemaVersion
import com.juzgon.domain.schema.SchemaCompatibility
import com.juzgon.domain.schema.SchemaCompatibilityChecker

class DefaultSchemaCompatibilityChecker : SchemaCompatibilityChecker {
    override fun check(storedVersion: Int): SchemaCompatibility {
        val status =
            when {
                storedVersion == 0 -> SchemaCompatibility.Status.COMPATIBLE
                storedVersion in DataSchemaVersion.SUPPORTED_READ_VERSIONS ->
                    SchemaCompatibility.Status.COMPATIBLE
                storedVersion > DataSchemaVersion.CURRENT ->
                    SchemaCompatibility.Status.NEWER_DATA_WARNING
                else -> SchemaCompatibility.Status.COMPATIBLE
            }
        return SchemaCompatibility(
            storedVersion = storedVersion,
            appVersion = DataSchemaVersion.CURRENT,
            status = status,
        )
    }
}
