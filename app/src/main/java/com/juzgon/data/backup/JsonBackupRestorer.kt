package com.juzgon.data.backup

import com.juzgon.data.local.dao.CategoryDao
import com.juzgon.data.local.dao.ItemDao
import com.juzgon.data.local.dao.ItemPurgeDao
import com.juzgon.data.local.dao.ScoreProfileAttributeDao
import com.juzgon.data.local.dao.ScoreProfileDao
import com.juzgon.data.local.entity.AttributeEntity
import com.juzgon.data.local.entity.CategoryEntity
import com.juzgon.data.local.entity.ItemEntity
import com.juzgon.data.local.entity.ItemValueEntity
import com.juzgon.data.local.entity.RatingEntity
import com.juzgon.data.local.entity.ScoreProfileAttributeEntity
import com.juzgon.data.local.entity.ScoreProfileEntity
import kotlinx.coroutines.flow.first
import org.json.JSONArray

/**
 * Handles restoration logic for backup import.
 * Extracted from JsonBackupService to reduce TooManyFunctions.
 */
class JsonBackupRestorer(
    private val categoryDao: CategoryDao,
    private val itemDao: ItemDao,
    @Suppress("UnusedPrivateProperty")
    private val itemPurgeDao: ItemPurgeDao,
    private val scoreProfileDao: ScoreProfileDao,
    @Suppress("UnusedPrivateProperty")
    private val scoreProfileAttributeDao: ScoreProfileAttributeDao,
) {
    suspend fun clearExistingData() {
        categoryDao
            .observeCategoriesWithAttributes()
            .first()
            .forEach { categoryDao.deleteCategoryByName(it.category.name) }
        itemDao
            .observeItemsWithRatings()
            .first()
            .forEach { itemDao.deleteItemById(it.item.id) }
        scoreProfileDao
            .observeAllProfiles()
            .first()
            .forEach { scoreProfileDao.deleteProfile(it.id) }
    }

    suspend fun restoreCategories(categoriesArray: JSONArray) {
        repeat(categoriesArray.length()) { i ->
            val catObj = categoriesArray.getJSONObject(i)
            val name = catObj.getString("name")
            val description = catObj.optString("description", null)
            val type = catObj.optString("type", null)
            categoryDao.upsertCategory(CategoryEntity(name, description, type))
            val attrsArray = catObj.getJSONArray("attributes")
            val attrs =
                (0 until attrsArray.length()).map { j ->
                    val attr = attrsArray.getJSONObject(j)
                    val rawId = attr.getString("id")
                    val id = BackupAttributeIdNormalizer.resolveOrThrow(rawId, name)
                    AttributeEntity(
                        id = id,
                        categoryName = name,
                        weight = attr.getDouble("weight"),
                        position = attr.getInt("position"),
                        type = attr.optString("type", "NUMBER"),
                        isRequired = attr.optBoolean("isRequired", true),
                        displayInDiamond = attr.optBoolean("displayInDiamond", true),
                        diamondOrder = if (attr.has("diamondOrder")) attr.getInt("diamondOrder") else null,
                        scoringDirection =
                            if (attr.has("scoringDirection")) {
                                attr.getString("scoringDirection")
                            } else {
                                null
                            },
                    )
                }
            if (attrs.isNotEmpty()) categoryDao.upsertAttributes(attrs)
        }
    }

    suspend fun restoreItems(itemsArray: JSONArray) {
        repeat(itemsArray.length()) { i ->
            val itemObj = itemsArray.getJSONObject(i)
            val itemId = itemObj.getString("id")
            val categoryName = itemObj.optString("categoryName", "")
            itemDao.upsertItem(
                ItemEntity(
                    id = itemId,
                    notes = itemObj.optString("notes", ""),
                    createdAt = itemObj.getLong("createdAt"),
                    updatedAt = itemObj.getLong("updatedAt"),
                ),
            )
            val ratingsArray = itemObj.getJSONArray("ratings")
            val ratings =
                (0 until ratingsArray.length()).map { j ->
                    val r = ratingsArray.getJSONObject(j)
                    val rawAttrId = r.getString("attributeId")
                    val attributeId = BackupAttributeIdNormalizer.resolveOrThrow(rawAttrId, categoryName)
                    RatingEntity(itemId = itemId, attributeId = attributeId, score = r.getInt("score"))
                }
            if (ratings.isNotEmpty()) itemDao.upsertRatings(ratings)

            val valuesArray = itemObj.optJSONArray("values")
            if (valuesArray != null && valuesArray.length() > 0) {
                val values =
                    (0 until valuesArray.length()).map { j ->
                        val v = valuesArray.getJSONObject(j)
                        val rawAttrId = v.getString("attributeId")
                        val attributeId = BackupAttributeIdNormalizer.resolveOrThrow(rawAttrId, categoryName)
                        ItemValueEntity(
                            itemId = itemId,
                            attributeId = attributeId,
                            valueText = v.getString("value"),
                        )
                    }
                itemDao.upsertItemValues(values)
            }
        }
    }

    suspend fun restoreScoreProfiles(profilesArray: JSONArray) {
        repeat(profilesArray.length()) { i ->
            val obj = profilesArray.getJSONObject(i)
            val profileId = obj.getString("id")
            val categoryName = obj.getString("categoryName")
            val profile =
                ScoreProfileEntity(
                    id = profileId,
                    categoryName = categoryName,
                    name = obj.getString("name"),
                    createdAt = obj.getLong("createdAt"),
                    updatedAt = obj.getLong("updatedAt"),
                )
            val attrIds = obj.getJSONArray("attributeIds")
            val attributes =
                (0 until attrIds.length()).map { j ->
                    val rawAttrId = attrIds.getString(j)
                    val attributeId = BackupAttributeIdNormalizer.resolveOrThrow(rawAttrId, categoryName)
                    ScoreProfileAttributeEntity(
                        profileId = profileId,
                        attributeId = attributeId,
                        position = j,
                    )
                }
            scoreProfileDao.upsertProfile(profile)
            scoreProfileAttributeDao.saveProfileWithAttributes(profile, attributes)
        }
    }
}
