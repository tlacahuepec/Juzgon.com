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
}
