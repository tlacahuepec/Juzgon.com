package com.juzgon.data.backup

import com.juzgon.data.local.dao.CategoryDao
import com.juzgon.data.local.dao.CategoryWithAttributes
import com.juzgon.data.local.dao.ItemDao
import com.juzgon.data.local.dao.ItemWithRatingsAndValues
import com.juzgon.data.local.entity.AttributeEntity
import com.juzgon.data.local.entity.CategoryEntity
import com.juzgon.data.local.entity.ItemEntity
import com.juzgon.data.local.entity.ItemValueEntity
import com.juzgon.data.local.entity.RatingEntity
import com.juzgon.domain.backup.BackupException
import com.juzgon.domain.backup.BackupService
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

private const val SCHEMA_VERSION = 2

class JsonBackupService(
    private val categoryDao: CategoryDao,
    private val itemDao: ItemDao,
) : BackupService {
    override suspend fun export(): String {
        val categories = categoryDao.observeCategoriesWithAttributes().first()
        val items = itemDao.observeItemsWithRatingsAndValues().first()
        return JSONObject()
            .apply {
                put("version", SCHEMA_VERSION)
                put("categories", serializeCategories(categories))
                put("items", serializeItems(items))
            }.toString()
    }

    private fun serializeCategories(categories: List<CategoryWithAttributes>): JSONArray =
        JSONArray().apply {
            categories.forEach { cwa ->
                put(
                    JSONObject().apply {
                        put("id", cwa.category.id)
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
                        put("name", attr.name)
                        put("type", attr.type)
                        put("isRequired", attr.isRequired)
                        put("weight", attr.weight)
                        put("position", attr.position)
                        put("options", attr.options)
                    },
                )
            }
        }

    private fun serializeItems(
        items: List<ItemWithRatingsAndValues>,
    ): JSONArray =
        JSONArray().apply {
            items.forEach { iwr ->
                put(
                    JSONObject().apply {
                        put("id", iwr.item.id)
                        put("categoryId", iwr.item.categoryId)
                        put("name", iwr.item.name)
                        put("notes", iwr.item.notes)
                        put("createdAt", iwr.item.createdAt)
                        put("updatedAt", iwr.item.updatedAt)
                        put("ratings", serializeRatings(iwr))
                        put("values", serializeValues(iwr))
                    },
                )
            }
        }

    private fun serializeRatings(iwr: ItemWithRatingsAndValues): JSONArray =
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

    private fun serializeValues(iwr: ItemWithRatingsAndValues): JSONArray =
        JSONArray().apply {
            iwr.values.forEach { value ->
                put(
                    JSONObject().apply {
                        put("attributeId", value.attributeId)
                        put("valueString", value.valueString)
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
            .forEach { categoryDao.deleteCategoryById(it.category.id) }
        itemDao
            .observeItemsWithRatingsAndValues()
            .first()
            .forEach { itemDao.deleteItemById(it.item.id) }
    }

    private suspend fun restoreCategories(categoriesArray: JSONArray) {
        repeat(categoriesArray.length()) { i ->
            val catObj = categoriesArray.getJSONObject(i)
            val id = catObj.getString("id")
            val name = catObj.getString("name")
            categoryDao.upsertCategory(CategoryEntity(id = id, name = name))
            val attrsArray = catObj.getJSONArray("attributes")
            val attrs =
                (0 until attrsArray.length()).map { j ->
                    val attr = attrsArray.getJSONObject(j)
                    AttributeEntity(
                        id = attr.getString("id"),
                        categoryId = id,
                        name = attr.getString("name"),
                        type = attr.getString("type"),
                        isRequired = attr.getBoolean("isRequired"),
                        weight = attr.getDouble("weight"),
                        position = attr.getInt("position"),
                        options = attr.optString("options", ""),
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
                    categoryId = itemObj.getString("categoryId"),
                    name = itemObj.getString("name"),
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
            
            val valuesArray = itemObj.optJSONArray("values")
            if (valuesArray != null) {
                val values =
                    (0 until valuesArray.length()).map { j ->
                        val v = valuesArray.getJSONObject(j)
                        ItemValueEntity(itemId = itemId, attributeId = v.getString("attributeId"), valueString = v.getString("valueString"))
                    }
                if (values.isNotEmpty()) itemDao.upsertValues(values)
            }
        }
    }
}
