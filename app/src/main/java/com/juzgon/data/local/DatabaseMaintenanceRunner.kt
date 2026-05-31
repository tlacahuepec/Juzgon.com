package com.juzgon.data.local

import androidx.room.withTransaction
import timber.log.Timber

private const val SOFT_DELETED_VALUE_RETENTION_DAYS = 30L
private const val HOURS_PER_DAY = 24L
private const val MINUTES_PER_HOUR = 60L
private const val SECONDS_PER_MINUTE = 60L
private const val MILLIS_PER_SECOND = 1_000L

data class DatabaseMaintenanceResult(
    val oldSoftDeletedValuesPurged: Int,
    val orphanRatingsPurged: Int,
    val orphanSoftDeletedValuesPurged: Int,
    val scoreProfilesWithoutAttributesDeleted: Int,
    val diagnosticsBeforeCleanup: DatabaseIntegrityReport,
)

class DatabaseMaintenanceRunner(
    private val database: JuzgonDatabase,
    private val diagnostics: DatabaseIntegrityRepository =
        DatabaseIntegrityRepository(
            ratingIntegrityDao = database.ratingIntegrityDao(),
            itemValueIntegrityDao = database.itemValueIntegrityDao(),
            scoreProfileIntegrityDao = database.scoreProfileIntegrityDao(),
            categoryIntegrityDao = database.categoryIntegrityDao(),
        ),
    private val currentTimeMillis: () -> Long = { System.currentTimeMillis() },
) {
    suspend fun runCleanup(): Result<DatabaseMaintenanceResult> =
        runCatching {
            val diagnosticsBeforeCleanup = diagnostics.diagnose()
            val result =
                database.withTransaction {
                    val cutoff = currentTimeMillis() - retentionMillis()
                    DatabaseMaintenanceResult(
                        oldSoftDeletedValuesPurged = database.itemPurgeDao().purgeOldSoftDeletedValues(cutoff),
                        orphanRatingsPurged = database.itemPurgeDao().purgeOrphanedRatings(),
                        orphanSoftDeletedValuesPurged = database.itemPurgeDao().purgeOrphanedSoftDeletedValues(),
                        scoreProfilesWithoutAttributesDeleted = database.scoreProfileDao().deleteOrphanedProfiles(),
                        diagnosticsBeforeCleanup = diagnosticsBeforeCleanup,
                    )
                }
            Timber.i(
                "Database cleanup completed: oldSoftDeletedValues=%d, orphanRatings=%d, " +
                    "orphanSoftDeletedValues=%d, scoreProfilesWithoutAttributes=%d, diagnostics=%s",
                result.oldSoftDeletedValuesPurged,
                result.orphanRatingsPurged,
                result.orphanSoftDeletedValuesPurged,
                result.scoreProfilesWithoutAttributesDeleted,
                diagnosticsBeforeCleanup.toLogSummary(),
            )
            result
        }.onFailure { error ->
            Timber.e(error, "Database cleanup failed")
        }

    private fun retentionMillis(): Long =
        SOFT_DELETED_VALUE_RETENTION_DAYS *
            HOURS_PER_DAY *
            MINUTES_PER_HOUR *
            SECONDS_PER_MINUTE *
            MILLIS_PER_SECOND
}

private fun DatabaseIntegrityReport.toLogSummary(): String =
    listOf(
        "orphanRatings=${orphanRatings.toLogSummary()}",
        "orphanActiveItemValues=${orphanActiveItemValues.toLogSummary()}",
        "orphanSoftDeletedItemValues=${orphanSoftDeletedItemValues.toLogSummary()}",
        "scoreProfilesWithoutAttributes=${scoreProfilesWithoutAttributes.toLogSummary()}",
        "scoreProfileAttributesMissingAttributes=${scoreProfileAttributesMissingAttributes.toLogSummary()}",
        "categoriesWithZeroRankableWeight=${categoriesWithZeroRankableWeight.toLogSummary()}",
    ).joinToString(prefix = "{", postfix = "}")

private fun DatabaseIntegrityFinding.toLogSummary(): String = "{count=$count,samples=$sampleIds}"
