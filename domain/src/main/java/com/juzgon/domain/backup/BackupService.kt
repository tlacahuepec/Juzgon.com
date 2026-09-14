package com.juzgon.domain.backup

interface BackupService {
    suspend fun export(): String

    suspend fun import(json: String)

    suspend fun exportArchive(): ByteArray = throw BackupException("Image archive export is unavailable")

    suspend fun importArchive(archive: ByteArray): Unit = throw BackupException("Image archive import is unavailable")
}

class BackupException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)
