package com.juzgon.domain.schema

fun interface StoredSchemaVersionProvider {
    fun getStoredVersion(): Int
}
