package com.juzgon.domain.backup

interface BackupService {
    suspend fun export(): String

    suspend fun import(json: String)
}

class BackupException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)
