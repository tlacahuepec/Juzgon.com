package com.juzgon.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

private const val DATABASE_VERSION_1 = 1
private const val DATABASE_VERSION_2 = 2
private const val DATABASE_VERSION_3 = 3
private const val DATABASE_VERSION_4 = 4

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
}
