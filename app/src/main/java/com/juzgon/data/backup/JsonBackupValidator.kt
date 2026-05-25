package com.juzgon.data.backup

import com.juzgon.domain.AttributeType
import com.juzgon.domain.CatalogType
import com.juzgon.domain.ScoringDirection
import com.juzgon.domain.backup.BackupValidationResult
import com.juzgon.domain.backup.BackupValidator
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

private const val MAX_SUPPORTED_VERSION = 5
private val VALID_TYPES = AttributeType.entries.map { it.name }.toSet()
private val VALID_SCORING_DIRECTIONS = ScoringDirection.entries.map { it.name }.toSet()
private val VALID_CATALOG_TYPES = CatalogType.entries.map { it.name }.toSet()

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
            validateCatalogType(cat, name, errors)
            if (cat.has("attributes")) {
                validateCategoryAttributes(cat.getJSONArray("attributes"), name, attributeIndex, errors)
            }
        }
    }

    private fun validateCatalogType(
        cat: JSONObject,
        name: String,
        errors: MutableList<String>,
    ) {
        if (cat.has("type")) {
            val type = cat.getString("type")
            if (type !in VALID_CATALOG_TYPES) {
                errors += "Category '$name' has invalid type: '$type'"
            }
        }
    }

    private fun validateCategoryAttributes(
        attrs: JSONArray,
        categoryName: String,
        attributeIndex: MutableMap<String, AttributeInfo>,
        errors: MutableList<String>,
    ) {
        val attrIds = mutableSetOf<String>()
        repeat(attrs.length()) { j ->
            val attr = attrs.getJSONObject(j)
            if (!attr.has("id")) {
                errors += "Attribute at index $j in category '$categoryName' missing required field: id"
                return@repeat
            }
            val attrId = attr.getString("id")
            val normalized = BackupAttributeIdNormalizer.normalize(attrId, categoryName)
            val resolvedId =
                when (normalized) {
                    is NormalizedAttributeId.Resolved -> normalized.id
                    is NormalizedAttributeId.Invalid -> {
                        errors += normalized.reason
                        return@repeat
                    }
                }
            if (!attrIds.add(resolvedId)) {
                errors += "Duplicate attribute id '$resolvedId' in category '$categoryName'"
            }
            val type = attr.optString("type", "NUMBER")
            if (type !in VALID_TYPES) {
                errors += "Attribute '$resolvedId' in category '$categoryName' has invalid type: '$type'"
            }
            if (attr.has("scoringDirection")) {
                val direction = attr.getString("scoringDirection")
                if (direction !in VALID_SCORING_DIRECTIONS) {
                    errors +=
                        "Attribute '$resolvedId' in category '$categoryName' " +
                        "has invalid scoringDirection: '$direction'"
                }
            }
            attributeIndex[resolvedId] =
                AttributeInfo(
                    type = type,
                    scoringDirection =
                        if (attr.has("scoringDirection")) attr.getString("scoringDirection") else null,
                )
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
            val itemCategoryName = item.optString("categoryName", "")
            validateItemAttributeRefs(item, id, itemCategoryName, "ratings", attributeIndex, errors)
            validateItemAttributeRefs(item, id, itemCategoryName, "values", attributeIndex, errors)
        }
    }

    @Suppress("LongParameterList")
    private fun validateItemAttributeRefs(
        item: JSONObject,
        itemId: String,
        categoryName: String,
        arrayKey: String,
        attributeIndex: Map<String, AttributeInfo>,
        errors: MutableList<String>,
    ) {
        if (!item.has(arrayKey)) return
        val array = item.getJSONArray(arrayKey)
        repeat(array.length()) { j ->
            val obj = array.getJSONObject(j)
            val rawAttrId = obj.optString("attributeId", "")
            if (rawAttrId.isEmpty()) return@repeat
            val normalized = BackupAttributeIdNormalizer.normalize(rawAttrId, categoryName)
            when (normalized) {
                is NormalizedAttributeId.Resolved ->
                    if (normalized.id !in attributeIndex) {
                        errors += "Item '$itemId' $arrayKey references non-existent attribute: '$rawAttrId'"
                    }
                is NormalizedAttributeId.Invalid ->
                    errors += "Item '$itemId' $arrayKey: ${normalized.reason}"
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
            val profileCategoryName = profile.optString("categoryName", "")
            if (profileCategoryName.isNotEmpty() && profileCategoryName !in categoryNames) {
                errors += "Score profile '$profileId' references non-existent category: '$profileCategoryName'"
            }
            if (profile.has("attributeIds")) {
                val attrIds = profile.getJSONArray("attributeIds")
                repeat(attrIds.length()) { j ->
                    val rawAttrId = attrIds.getString(j)
                    val normalized = BackupAttributeIdNormalizer.normalize(rawAttrId, profileCategoryName)
                    when (normalized) {
                        is NormalizedAttributeId.Resolved -> {
                            val info = attributeIndex[normalized.id]
                            if (info == null) {
                                errors += "Score profile '$profileId' references non-existent attribute: '$rawAttrId'"
                            } else if (!info.isRankable) {
                                errors += "Score profile '$profileId' includes non-rankable attribute: '$rawAttrId'"
                            }
                        }
                        is NormalizedAttributeId.Invalid ->
                            errors += "Score profile '$profileId': ${normalized.reason}"
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
