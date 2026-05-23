package com.juzgon.domain.backup

data class BackupValidationResult(
    val errors: List<String> = emptyList(),
) {
    val isValid: Boolean get() = errors.isEmpty()
}

interface BackupValidator {
    fun validate(json: String): BackupValidationResult
}
