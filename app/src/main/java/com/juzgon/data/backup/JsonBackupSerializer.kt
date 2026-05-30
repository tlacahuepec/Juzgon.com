package com.juzgon.data.backup

import com.juzgon.data.local.dao.CategoryWithAttributes
import com.juzgon.data.local.dao.ItemWithRatings
import com.juzgon.data.local.entity.ItemValueEntity
import com.juzgon.data.local.entity.ScoreProfileAttributeEntity
import com.juzgon.data.local.entity.ScoreProfileEntity
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant

/**
 * Handles all JSON serialization for backup export.
 * Extracted from JsonBackupService to reduce TooManyFunctions.
 */
class JsonBackupSerializer {
    fun serializeExport(
        categories: List<CategoryWithAttributes>,
        items: List<ItemWithRatings>,
        profiles: List<ScoreProfileEntity>,
        profileAttributes: List<ScoreProfileAttributeEntity>,
    ): String {
        val attributeToCategory =
            categories.flatMap { cwa -> cwa.attributes.map { it.id to cwa.category.name } }.toMap()
        val profileAttributesByProfileId = profileAttributes.groupBy { it.profileId }

        return JSONObject()
            .apply {
                put("version", SCHEMA_VERSION)
                put("app", "Juzgon")
                put("exportedAt", Instant.now().toString())
                put("categories", serializeCategories(categories))
                put("items", serializeItems(items, attributeToCategory))
                put("scoreProfiles", serializeScoreProfiles(profiles, profileAttributesByProfileId))
            }.toString()
    }

    private fun serializeCategories(categories: List<CategoryWithAttributes>): JSONArray =
        JSONArray().apply {
            categories.forEach { cwa ->
                put(
                    JSONObject().apply {
                        put("name", cwa.category.name)
                        cwa.category.description?.let { put("description", it) }
                        cwa.category.type?.let { put("type", it) }
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
                        put("type", attr.type)
                        put("isRequired", attr.isRequired)
                        put("displayInDiamond", attr.displayInDiamond)
                        attr.diamondOrder?.let { put("diamondOrder", it) }
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
                        ?: iwr.values
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
                        put("values", serializeItemValues(iwr.values.filter { it.deletedAt == null }))
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

    private fun serializeItemValues(values: List<ItemValueEntity>): JSONArray =
        JSONArray().apply {
            values.forEach { itemValue ->
                put(
                    JSONObject().apply {
                        put("attributeId", itemValue.attributeId)
                        put("value", itemValue.valueText)
                    },
                )
            }
        }

    private fun serializeScoreProfiles(
        profiles: List<ScoreProfileEntity>,
        profileAttributesByProfileId: Map<String, List<ScoreProfileAttributeEntity>>,
    ): JSONArray =
        JSONArray().apply {
            profiles.forEach { profile ->
                put(
                    JSONObject().apply {
                        put("id", profile.id)
                        put("categoryName", profile.categoryName)
                        put("name", profile.name)
                        put("createdAt", profile.createdAt)
                        put("updatedAt", profile.updatedAt)
                        put(
                            "attributeIds",
                            JSONArray().apply {
                                profileAttributesByProfileId[profile.id]
                                    ?.sortedBy { it.position }
                                    ?.forEach { put(it.attributeId) }
                            },
                        )
                    },
                )
            }
        }

    companion object {
        private const val SCHEMA_VERSION = 5
    }
}
