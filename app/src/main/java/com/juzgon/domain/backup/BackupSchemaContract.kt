package com.juzgon.domain.backup

/**
 * Backup Schema Contract v4
 *
 * Defines the structure and guarantees of `.juzgon.json` backup files for
 * interoperability between export and future import features.
 *
 * ## Import Conflict Resolution (for future import implementation)
 * - Empty app: restore records using their original stable IDs.
 * - ID collision with identical record: safe skip or merge.
 * - ID collision with different record: require user decision or generate new IDs
 *   with reference remapping.
 * - Name conflict with different ID: allow import, clearly show duplicates or ask
 *   user to rename.
 * - Unsupported future schema version: block with a clear message.
 * - Older supported schema version: migrate data to current schema before import.
 *
 * ## Schema Migration
 * - Each schema version bump documents what changed.
 * - Import must support all [SUPPORTED_VERSIONS] by applying incremental migrations.
 * - v2 → v3: attribute IDs scoped as "categoryName/attributeName" for global
 *   uniqueness. Import from v1/v2 auto-prefixes bare IDs with category name.
 * - v3 → v4: export only active values (excludes soft-deleted). Import creates
 *   all values as active (deletedAt = null).
 * - v4 → v5: adds optional `description` and `type` fields to categories.
 *   Import from v1-v4 defaults both to null.
 *
 * ## Image Assignments
 * - Image assignments in JSON export are metadata/references only (URI strings).
 * - Full image binary export requires ZIP format (future feature).
 * - Absent imageAssignments key is valid — it is an optional extension.
 *
 * ## Optional Fields
 * - Missing optional fields must not break validation or future import.
 * - Import should apply sensible defaults for absent optional fields.
 */
object BackupSchemaContract {
    private const val SCHEMA_VERSION_1 = 1
    private const val SCHEMA_VERSION_2 = 2
    private const val SCHEMA_VERSION_3 = 3
    private const val SCHEMA_VERSION_4 = 4
    private const val SCHEMA_VERSION_5 = 5

    const val CURRENT_VERSION = SCHEMA_VERSION_5
    val SUPPORTED_VERSIONS =
        setOf(
            SCHEMA_VERSION_1,
            SCHEMA_VERSION_2,
            SCHEMA_VERSION_3,
            SCHEMA_VERSION_4,
            SCHEMA_VERSION_5,
        )

    val REQUIRED_METADATA_FIELDS = setOf("version", "app", "exportedAt")
    val REQUIRED_ARRAYS = setOf("categories", "items", "scoreProfiles")

    const val CATEGORY_ID_FIELD = "name"
    const val ATTRIBUTE_ID_FIELD = "id"
    const val ITEM_ID_FIELD = "id"
    const val SCORE_PROFILE_ID_FIELD = "id"

    val REQUIRED_ATTRIBUTE_FIELDS = setOf("id", "weight", "position", "type")
    val OPTIONAL_ATTRIBUTE_FIELDS = setOf("isRequired", "displayInDiamond", "diamondOrder", "scoringDirection")
}
