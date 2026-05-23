package com.juzgon.data.backup

import com.juzgon.data.local.dao.CategoryDao
import com.juzgon.data.local.dao.CategoryWithAttributes
import com.juzgon.data.local.dao.ItemDao
import com.juzgon.data.local.dao.ItemWithRatings
import com.juzgon.data.local.entity.AttributeEntity
import com.juzgon.data.local.entity.CategoryEntity
import com.juzgon.data.local.entity.ItemEntity
import com.juzgon.data.local.entity.RatingEntity
import com.juzgon.domain.backup.BackupException
import com.juzgon.domain.backup.BackupService
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

private const val SCHEMA_VERSION = 1

class JsonBackupService(
    private val categoryDao: CategoryDao,
    private val itemDao: ItemDao,
) : BackupService {
    override suspend fun export(): String {
        val categories = categoryDao.observeCategoriesWithAttributes().first()
        val items = itemDao.observeItemsWithRatings().first()
        val attributeToCategory =
            categories.flatMap { cwa -> cwa.attributes.map { it.id to cwa.category.name } }.toMap()
        return JSONObject()
            .apply {
                put("version", SCHEMA_VERSION)
                put("categories", serializeCategories(categories))
                put("items", serializeItems(items, attributeToCategory))
            }.toString()
    }

    private fun serializeCategories(categories: List<CategoryWithAttributes>): JSONArray =
        JSONArray().apply {
            categories.forEach { cwa ->
                put(
                    JSONObject().apply {
                        put("name", cwa.category.name)
                        put("attributes", serializeAttributes(cwa))
                    },
                )
            }
        }

    private fun serializeAttributes(cwa: CategoryWithAttributes): JSONArray =
        JSONArray().apply {
            cwa.attributes.sortedBy { it.position }.forEach { attr ->
                put(
                    JSONObject().apply {
                        put("id", attr.id)
                        put("weight", attr.weight)
                        put("position", attr.position)
                        attr.scoringDirection?.let { put("scoringDirection", it) }
                    },
                )
            }
        }

    private fun serializeItems(
        items: List<ItemWithRatings>,
        attributeToCategory: Map<String, String>,
    ): JSONArray =
        JSONArray().apply {
            items.forEach { iwr ->
                val categoryName =
                    iwr.ratings
                        .firstOrNull()
                        ?.let { attributeToCategory[it.attributeId] }
                        .orEmpty()
                put(
                    JSONObject().apply {
                        put("id", iwr.item.id)
                        put("categoryName", categoryName)
                        put("notes", iwr.item.notes)
                        put("createdAt", iwr.item.createdAt)
                        put("updatedAt", iwr.item.updatedAt)
                        put("ratings", serializeRatings(iwr))
                    },
                )
            }
        }

    private fun serializeRatings(iwr: ItemWithRatings): JSONArray =
        JSONArray().apply {
            iwr.ratings.forEach { rating ->
                put(
                    JSONObject().apply {
                        put("attributeId", rating.attributeId)
                        put("score", rating.score)
                    },
                )
            }
        }

    override suspend fun import(json: String) {
        val root =
            try {
                JSONObject(json)
            } catch (e: JSONException) {
                throw BackupException("Invalid JSON: ${e.message}", e)
            }
        if (!root.has("version")) throw BackupException("Missing version field")
        val version = root.getInt("version")
        if (version != SCHEMA_VERSION) throw BackupException("Unsupported backup version: $version")

        clearExistingData()
        restoreCategories(root.getJSONArray("categories"))
        restoreItems(root.getJSONArray("items"))
    }

    private suspend fun clearExistingData() {
        categoryDao
            .observeCategoriesWithAttributes()
            .first()
            .forEach { categoryDao.deleteCategoryByName(it.category.name) }
        itemDao
            .observeItemsWithRatings()
            .first()
            .forEach { itemDao.deleteItemById(it.item.id) }
    }

    private suspend fun restoreCategories(categoriesArray: JSONArray) {
        repeat(categoriesArray.length()) { i ->
            val catObj = categoriesArray.getJSONObject(i)
            val name = catObj.getString("name")
            categoryDao.upsertCategory(CategoryEntity(name))
            val attrsArray = catObj.getJSONArray("attributes")
            val attrs =
                (0 until attrsArray.length()).map { j ->
                    val attr = attrsArray.getJSONObject(j)
                    AttributeEntity(
                        id = attr.getString("id"),
                        categoryName = name,
                        weight = attr.getDouble("weight"),
                        position = attr.getInt("position"),
                        scoringDirection =
                            if (attr.has("scoringDirection")) attr.getString("scoringDirection") else null,
                    )
                }
            if (attrs.isNotEmpty()) categoryDao.upsertAttributes(attrs)
        }
    }

    private suspend fun restoreItems(itemsArray: JSONArray) {
        repeat(itemsArray.length()) { i ->
            val itemObj = itemsArray.getJSONObject(i)
            val itemId = itemObj.getString("id")
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
                    RatingEntity(itemId = itemId, attributeId = r.getString("attributeId"), score = r.getInt("score"))
                }
            if (ratings.isNotEmpty()) itemDao.upsertRatings(ratings)
        }
    }
}
