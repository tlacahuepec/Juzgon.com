package com.juzgon.domain.schema

fun interface SchemaCompatibilityChecker {
    fun check(storedVersion: Int): SchemaCompatibility
}
