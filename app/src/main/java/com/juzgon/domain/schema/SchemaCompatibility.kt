package com.juzgon.domain.schema

data class SchemaCompatibility(
    val storedVersion: Int,
    val appVersion: Int,
    val status: Status,
) {
    enum class Status {
        COMPATIBLE,
        NEWER_DATA_WARNING,
    }
}
