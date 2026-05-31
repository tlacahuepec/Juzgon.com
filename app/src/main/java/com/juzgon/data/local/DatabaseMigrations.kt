package com.juzgon.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

private const val DATABASE_VERSION_1 = 1
private const val DATABASE_VERSION_2 = 2
private const val DATABASE_VERSION_3 = 3
private const val DATABASE_VERSION_4 = 4
private const val DATABASE_VERSION_5 = 5
private const val DATABASE_VERSION_6 = 6
private const val DATABASE_VERSION_7 = 7
private const val DATABASE_VERSION_8 = 8
private const val DATABASE_VERSION_9 = 9
private const val DATABASE_VERSION_10 = 10
private const val DATABASE_VERSION_11 = 11
private const val DATABASE_VERSION_12 = 12
private const val DATABASE_VERSION_13 = 13
private const val DATABASE_VERSION_14 = 14
private const val DATABASE_VERSION_15 = 15
private const val DATABASE_VERSION_16 = 16
private const val DATABASE_VERSION_17 = 17

object DatabaseMigrations {
    val MIGRATION_1_2: Migration =
        object : Migration(DATABASE_VERSION_1, DATABASE_VERSION_2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Version 2 intentionally keeps the v1 schema shape unchanged.
            }
        }

    val MIGRATION_2_3: Migration =
        object : Migration(DATABASE_VERSION_2, DATABASE_VERSION_3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE attributes ADD COLUMN position INTEGER NOT NULL DEFAULT 0")
            }
        }

    val MIGRATION_3_4: Migration =
        object : Migration(DATABASE_VERSION_3, DATABASE_VERSION_4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE items ADD COLUMN notes TEXT NOT NULL DEFAULT ''")
            }
        }

    val MIGRATION_4_5: Migration =
        object : Migration(DATABASE_VERSION_4, DATABASE_VERSION_5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE items ADD COLUMN created_at INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE items ADD COLUMN updated_at INTEGER NOT NULL DEFAULT 0")
            }
        }

    val MIGRATION_5_6: Migration =
        object : Migration(DATABASE_VERSION_5, DATABASE_VERSION_6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE attributes ADD COLUMN type TEXT NOT NULL DEFAULT 'NUMBER'")
                db.execSQL("ALTER TABLE attributes ADD COLUMN is_required INTEGER NOT NULL DEFAULT 1")
            }
        }

    val MIGRATION_6_7: Migration =
        object : Migration(DATABASE_VERSION_6, DATABASE_VERSION_7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE item_values (
                        item_id TEXT NOT NULL,
                        attribute_id TEXT NOT NULL,
                        value_text TEXT NOT NULL,
                        PRIMARY KEY (item_id, attribute_id),
                        FOREIGN KEY (item_id) REFERENCES items(id) ON DELETE CASCADE,
                        FOREIGN KEY (attribute_id) REFERENCES attributes(id) ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX index_item_values_item_id ON item_values(item_id)")
                db.execSQL("CREATE INDEX index_item_values_attribute_id ON item_values(attribute_id)")
            }
        }

    val MIGRATION_7_8: Migration =
        object : Migration(DATABASE_VERSION_7, DATABASE_VERSION_8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE attribute_rank_snapshots (
                        item_id TEXT NOT NULL,
                        captured_at INTEGER NOT NULL,
                        attribute_id TEXT NOT NULL,
                        value INTEGER NOT NULL,
                        rank INTEGER NOT NULL,
                        PRIMARY KEY (item_id, captured_at, attribute_id),
                        FOREIGN KEY (item_id) REFERENCES items(id) ON DELETE CASCADE,
                        FOREIGN KEY (attribute_id) REFERENCES attributes(id) ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX index_attribute_rank_snapshots_item_id ON attribute_rank_snapshots(item_id)")
                db.execSQL(
                    "CREATE INDEX index_attribute_rank_snapshots_attribute_id " +
                        "ON attribute_rank_snapshots(attribute_id)",
                )
            }
        }

    val MIGRATION_8_9: Migration =
        object : Migration(DATABASE_VERSION_8, DATABASE_VERSION_9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE attributes ADD COLUMN display_in_diamond INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE attributes ADD COLUMN diamond_order INTEGER")
            }
        }

    val MIGRATION_9_10: Migration =
        object : Migration(DATABASE_VERSION_9, DATABASE_VERSION_10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE attributes ADD COLUMN scoring_direction TEXT")
            }
        }

    val MIGRATION_10_11: Migration =
        object : Migration(DATABASE_VERSION_10, DATABASE_VERSION_11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE score_profiles (
                        id TEXT NOT NULL PRIMARY KEY,
                        category_name TEXT NOT NULL,
                        name TEXT NOT NULL,
                        created_at INTEGER NOT NULL DEFAULT 0,
                        updated_at INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY (category_name) REFERENCES categories(name) ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX index_score_profiles_category_name ON score_profiles(category_name)")
                db.execSQL(
                    """
                    CREATE TABLE score_profile_attributes (
                        profile_id TEXT NOT NULL,
                        attribute_id TEXT NOT NULL,
                        position INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY (profile_id, attribute_id),
                        FOREIGN KEY (profile_id) REFERENCES score_profiles(id) ON DELETE CASCADE,
                        FOREIGN KEY (attribute_id) REFERENCES attributes(id) ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE INDEX index_score_profile_attributes_profile_id " +
                        "ON score_profile_attributes(profile_id)",
                )
                db.execSQL(
                    "CREATE INDEX index_score_profile_attributes_attribute_id " +
                        "ON score_profile_attributes(attribute_id)",
                )
            }
        }

    val MIGRATION_11_12: Migration =
        object : Migration(DATABASE_VERSION_11, DATABASE_VERSION_12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.scopeAttributeForeignKeysToCategoryNamespace("ratings")
                db.scopeAttributeForeignKeysToCategoryNamespace("item_values")
                db.scopeAttributeForeignKeysToCategoryNamespace("attribute_rank_snapshots")
                db.scopeAttributeForeignKeysToCategoryNamespace("score_profile_attributes")
                db.scopeAttributePrimaryKeysToCategoryNamespace()
            }
        }

    val MIGRATION_12_13: Migration =
        object : Migration(DATABASE_VERSION_12, DATABASE_VERSION_13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE item_values_new (
                        item_id TEXT NOT NULL,
                        attribute_id TEXT NOT NULL,
                        value_text TEXT NOT NULL,
                        deleted_at INTEGER DEFAULT NULL,
                        PRIMARY KEY (item_id, attribute_id),
                        FOREIGN KEY (item_id) REFERENCES items(id) ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    INSERT INTO item_values_new (item_id, attribute_id, value_text)
                    SELECT item_id, attribute_id, value_text
                    FROM item_values
                    """.trimIndent(),
                )
                db.execSQL("DROP TABLE item_values")
                db.execSQL("ALTER TABLE item_values_new RENAME TO item_values")
                db.execSQL("CREATE INDEX index_item_values_item_id ON item_values(item_id)")
                db.execSQL("CREATE INDEX index_item_values_attribute_id ON item_values(attribute_id)")
                db.execSQL("CREATE INDEX index_item_values_deleted_at ON item_values(deleted_at)")

                db.execSQL(
                    """
                    CREATE TABLE ratings_new (
                        item_id TEXT NOT NULL,
                        attribute_id TEXT NOT NULL,
                        score INTEGER NOT NULL,
                        PRIMARY KEY (item_id, attribute_id),
                        FOREIGN KEY (item_id) REFERENCES items(id) ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    INSERT INTO ratings_new (item_id, attribute_id, score)
                    SELECT item_id, attribute_id, score
                    FROM ratings
                    """.trimIndent(),
                )
                db.execSQL("DROP TABLE ratings")
                db.execSQL("ALTER TABLE ratings_new RENAME TO ratings")
                db.execSQL("CREATE INDEX index_ratings_item_id ON ratings(item_id)")
                db.execSQL("CREATE INDEX index_ratings_attribute_id ON ratings(attribute_id)")
            }
        }

    val MIGRATION_13_14: Migration =
        object : Migration(DATABASE_VERSION_13, DATABASE_VERSION_14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    UPDATE item_values
                    SET attribute_id = (
                        SELECT a.category_name || '/' ||
                            SUBSTR(
                                item_values.attribute_id,
                                INSTR(item_values.attribute_id, '/') + 1,
                            )
                        FROM ratings r
                        INNER JOIN attributes a ON a.id = r.attribute_id
                        WHERE r.item_id = item_values.item_id
                        LIMIT 1
                    )
                    WHERE attribute_id NOT IN (SELECT id FROM attributes)
                      AND INSTR(attribute_id, '/') > 0
                    """.trimIndent(),
                )
            }
        }

    val MIGRATION_14_15: Migration =
        object : Migration(DATABASE_VERSION_14, DATABASE_VERSION_15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    UPDATE item_values
                    SET attribute_id = (
                        SELECT candidate.id
                        FROM attributes candidate
                        WHERE INSTR(candidate.id, '/') > 0
                                                    AND SUBSTR(candidate.id, INSTR(candidate.id, '/') + 1) =
                                                            SUBSTR(item_values.attribute_id, INSTR(item_values.attribute_id, '/') + 1)
                    )
                    WHERE attribute_id NOT IN (SELECT id FROM attributes)
                      AND INSTR(attribute_id, '/') > 0
                      AND (
                          SELECT COUNT(*)
                          FROM attributes candidate
                          WHERE INSTR(candidate.id, '/') > 0
                                                        AND SUBSTR(candidate.id, INSTR(candidate.id, '/') + 1) =
                                                                SUBSTR(item_values.attribute_id, INSTR(item_values.attribute_id, '/') + 1)
                      ) = 1
                      AND (
                          SELECT COUNT(*)
                          FROM item_values sibling
                          WHERE sibling.item_id = item_values.item_id
                            AND sibling.attribute_id NOT IN (SELECT id FROM attributes)
                            AND INSTR(sibling.attribute_id, '/') > 0
                                                        AND SUBSTR(sibling.attribute_id, INSTR(sibling.attribute_id, '/') + 1) =
                                                                SUBSTR(item_values.attribute_id, INSTR(item_values.attribute_id, '/') + 1)
                      ) = 1
                      AND NOT EXISTS (
                          SELECT 1
                          FROM item_values existing
                          WHERE existing.item_id = item_values.item_id
                            AND existing.attribute_id = (
                                SELECT candidate.id
                                FROM attributes candidate
                                WHERE INSTR(candidate.id, '/') > 0
                                    AND SUBSTR(candidate.id, INSTR(candidate.id, '/') + 1) =
                                      SUBSTR(item_values.attribute_id, INSTR(item_values.attribute_id, '/') + 1)
                            )
                      )
                    """.trimIndent(),
                )
            }
        }

    val MIGRATION_15_16: Migration =
        object : Migration(DATABASE_VERSION_15, DATABASE_VERSION_16) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE categories ADD COLUMN description TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE categories ADD COLUMN type TEXT DEFAULT NULL")
            }
        }

    val MIGRATION_16_17: Migration =
        object : Migration(DATABASE_VERSION_16, DATABASE_VERSION_17) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE enrichment_suggestion_cache (
                        cacheKeyHash TEXT NOT NULL PRIMARY KEY,
                        catalogId TEXT NOT NULL,
                        itemIdentity TEXT NOT NULL,
                        targetAttributeKey TEXT NOT NULL,
                        knownAttributesFingerprint TEXT NOT NULL,
                        status TEXT NOT NULL,
                        suggestedValue TEXT,
                        displayValue TEXT,
                        confidence TEXT,
                        sources TEXT,
                        reason TEXT,
                        failureCode TEXT,
                        cachedAt INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE INDEX index_enrichment_suggestion_cache_lookup " +
                        "ON enrichment_suggestion_cache(" +
                        "catalogId, itemIdentity, targetAttributeKey, knownAttributesFingerprint)",
                )
            }
        }
}

/**
 * Rewrites bare attribute_id foreign keys in the given table to the v12+ namespaced form
 * "categoryName/attributeId". This was required so that the same local attribute name
 * can exist under different categories.
 */
private fun SupportSQLiteDatabase.scopeAttributeForeignKeysToCategoryNamespace(tableName: String) {
    execSQL(
        """
        UPDATE $tableName
        SET attribute_id = (
            SELECT a.category_name || '/' || a.id
            FROM attributes a
            WHERE a.id = $tableName.attribute_id
        )
        WHERE EXISTS (
            SELECT 1
            FROM attributes a
            WHERE a.id = $tableName.attribute_id
        )
        """.trimIndent(),
    )
}

/**
 * Rewrites the attributes table primary key from the bare local id to the v12+ namespaced
 * form "categoryName/attributeId". Must run after all referencing tables have been updated.
 */
private fun SupportSQLiteDatabase.scopeAttributePrimaryKeysToCategoryNamespace() {
    execSQL("UPDATE attributes SET id = category_name || '/' || id")
}
