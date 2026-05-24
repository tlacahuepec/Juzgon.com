package com.juzgon.data.backup

import com.juzgon.domain.backup.BackupValidationResult
import com.juzgon.domain.backup.BackupValidator
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

private const val MAX_SUPPORTED_VERSION = 3

class JsonBackupValidator : BackupValidator {
    @Suppress("ReturnCount")
    override fun validate(json: String): BackupValidationResult {
        val errors = mutableListOf<String>()

        val root =
            try {
                JSONObject(json)
            } catch (e: JSONException) {
                return BackupValidationResult(listOf("Invalid JSON: ${e.message}"))
            }

        validateMetadata(root, errors)
        val categories = validateRequiredArray(root, "categories", errors)
        val items = validateRequiredArray(root, "items", errors)
        val scoreProfiles = validateRequiredArray(root, "scoreProfiles", errors)

        if (errors.isNotEmpty()) return BackupValidationResult(errors)

        val categoryNames = mutableSetOf<String>()
        val attributeIndex = mutableMapOf<String, AttributeInfo>()
        validateCategories(categories!!, categoryNames, attributeIndex, errors)

        val itemIds = mutableSetOf<String>()
        validateItems(items!!, categoryNames, attributeIndex, itemIds, errors)

        validateScoreProfiles(scoreProfiles!!, categoryNames, attributeIndex, errors)

        if (root.has("imageAssignments")) {
            validateImageAssignments(root.getJSONArray("imageAssignments"), itemIds, errors)
        }

        return BackupValidationResult(errors)
    }

    private fun validateMetadata(
        root: JSONObject,
        errors: MutableList<String>,
    ) {
        if (!root.has("version")) {
            errors += "Missing required field: version"
        } else {
            val version = root.getInt("version")
            if (version > MAX_SUPPORTED_VERSION) {
                errors += "Unsupported version: $version (max supported: $MAX_SUPPORTED_VERSION)"
            }
        }
        if (!root.has("app")) errors += "Missing required field: app"
        if (!root.has("exportedAt")) errors += "Missing required field: exportedAt"
    }

    private fun validateRequiredArray(
        root: JSONObject,
        key: String,
        errors: MutableList<String>,
    ): JSONArray? =
        if (!root.has(key)) {
            errors += "Missing required array: $key"
            null
        } else {
            root.getJSONArray(key)
        }

    private fun validateCategories(
        categories: JSONArray,
        categoryNames: MutableSet<String>,
        attributeIndex: MutableMap<String, AttributeInfo>,
        errors: MutableList<String>,
    ) {
        repeat(categories.length()) { i ->
            val cat = categories.getJSONObject(i)
            if (!cat.has("name")) {
                errors += "Category at index $i missing required field: name"
                return@repeat
            }
            val name = cat.getString("name")
            if (!categoryNames.add(name)) {
                errors += "Duplicate category name: '$name'"
            }
            if (cat.has("attributes")) {
                val attrs = cat.getJSONArray("attributes")
                val attrIds = mutableSetOf<String>()
                repeat(attrs.length()) { j ->
                    val attr = attrs.getJSONObject(j)
                    if (!attr.has("id")) {
                        errors += "Attribute at index $j in category '$name' missing required field: id"
                        return@repeat
                    }
                    val attrId = attr.getString("id")
                    if (!attrIds.add(attrId)) {
                        errors += "Duplicate attribute id '$attrId' in category '$name'"
                    }
                    attributeIndex[attrId] =
                        AttributeInfo(
                            type = attr.optString("type", "NUMBER"),
                            scoringDirection =
                                if (attr.has("scoringDirection")) attr.getString("scoringDirection") else null,
                        )
                }
            }
        }
    }

    private fun validateItems(
        items: JSONArray,
        categoryNames: Set<String>,
        attributeIndex: Map<String, AttributeInfo>,
        itemIds: MutableSet<String>,
        errors: MutableList<String>,
    ) {
        repeat(items.length()) { i ->
            val item = items.getJSONObject(i)
            if (!item.has("id")) {
                errors += "Item at index $i missing required field: id"
                return@repeat
            }
            val id = item.getString("id")
            if (!itemIds.add(id)) {
                errors += "Duplicate item id: '$id'"
            }
            if (item.has("categoryName")) {
                val catName = item.getString("categoryName")
                if (catName.isNotEmpty() && catName !in categoryNames) {
                    errors += "Item '$id' references non-existent category: '$catName'"
                }
            }
            if (item.has("ratings")) {
                val ratings = item.getJSONArray("ratings")
                repeat(ratings.length()) { j ->
                    val rating = ratings.getJSONObject(j)
                    val attrId = rating.optString("attributeId", "")
                    if (attrId.isNotEmpty() && attrId !in attributeIndex) {
                        errors += "Item '$id' rating references non-existent attribute: '$attrId'"
                    }
                }
            }
            if (item.has("values")) {
                val values = item.getJSONArray("values")
                repeat(values.length()) { j ->
                    val value = values.getJSONObject(j)
                    val attrId = value.optString("attributeId", "")
                    if (attrId.isNotEmpty() && attrId !in attributeIndex) {
                        errors += "Item '$id' value references non-existent attribute: '$attrId'"
                    }
                }
            }
        }
    }

    private fun validateScoreProfiles(
        profiles: JSONArray,
        categoryNames: Set<String>,
        attributeIndex: Map<String, AttributeInfo>,
        errors: MutableList<String>,
    ) {
        repeat(profiles.length()) { i ->
            val profile = profiles.getJSONObject(i)
            val profileId = profile.optString("id", "profile[$i]")
            if (profile.has("categoryName")) {
                val catName = profile.getString("categoryName")
                if (catName !in categoryNames) {
                    errors += "Score profile '$profileId' references non-existent category: '$catName'"
                }
            }
            if (profile.has("attributeIds")) {
                val attrIds = profile.getJSONArray("attributeIds")
                repeat(attrIds.length()) { j ->
                    val attrId = attrIds.getString(j)
                    val info = attributeIndex[attrId]
                    if (info == null) {
                        errors += "Score profile '$profileId' references non-existent attribute: '$attrId'"
                    } else if (!info.isRankable) {
                        errors += "Score profile '$profileId' includes non-rankable attribute: '$attrId'"
                    }
                }
            }
        }
    }

    private fun validateImageAssignments(
        assignments: JSONArray,
        itemIds: Set<String>,
        errors: MutableList<String>,
    ) {
        repeat(assignments.length()) { i ->
            val assignment = assignments.getJSONObject(i)
            val itemId = assignment.optString("itemId", "")
            if (itemId.isNotEmpty() && itemId !in itemIds) {
                errors += "Image assignment references non-existent item: '$itemId'"
            }
        }
    }

    private data class AttributeInfo(
        val type: String,
        val scoringDirection: String?,
    ) {
        val isRankable: Boolean
            get() = type == "NUMBER" || (type == "DATE" && scoringDirection != null)
    }
}
