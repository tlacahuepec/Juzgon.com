package com.juzgon.data.backup

import com.juzgon.data.local.dao.CategoryDao
import com.juzgon.data.local.dao.ItemDao
import com.juzgon.data.local.dao.ItemPurgeDao
import com.juzgon.data.local.dao.ScoreProfileAttributeDao
import com.juzgon.data.local.dao.ScoreProfileDao
import com.juzgon.domain.backup.BackupException
import com.juzgon.domain.backup.BackupService
import com.juzgon.domain.backup.BackupValidator
import kotlinx.coroutines.flow.first
import org.json.JSONException
import org.json.JSONObject

class JsonBackupService(
    private val validator: BackupValidator,
    private val categoryDao: CategoryDao,
    private val itemDao: ItemDao,
    private val itemPurgeDao: ItemPurgeDao,
    private val scoreProfileDao: ScoreProfileDao,
    private val scoreProfileAttributeDao: ScoreProfileAttributeDao,
    private val runInTransaction: suspend (suspend () -> Unit) -> Unit,
    private val runPostImportMaintenance: suspend () -> Unit = {},
    private val serializer: JsonBackupSerializer = JsonBackupSerializer(),
    private val restorer: JsonBackupRestorer =
        JsonBackupRestorer(
            categoryDao,
            itemDao,
            itemPurgeDao,
            scoreProfileDao,
            scoreProfileAttributeDao,
        ),
) : BackupService {
    override suspend fun export(): String {
        val categories = categoryDao.observeCategoriesWithAttributes().first()
        val items = itemDao.observeItemsWithRatings().first()
        val profiles = scoreProfileDao.observeAllProfiles().first()
        val profileAttributes = scoreProfileAttributeDao.observeAllProfileAttributes().first()
        return serializer.serializeExport(categories, items, profiles, profileAttributes)
    }

    override suspend fun import(json: String) {
        val validationResult = validator.validate(json)
        if (!validationResult.isValid) {
            throw BackupException("Backup validation failed: ${validationResult.errors.first()}")
        }

        val root =
            try {
                JSONObject(json)
            } catch (e: JSONException) {
                throw BackupException("Invalid JSON: ${e.message}", e)
            }

        runInTransaction {
            restorer.clearExistingData()
            restorer.restoreCategories(root.getJSONArray("categories"))
            restorer.restoreItems(root.getJSONArray("items"))
            if (root.has("scoreProfiles")) {
                restorer.restoreScoreProfiles(root.getJSONArray("scoreProfiles"))
            }
        }

        runPostImportMaintenance()
    }
}
