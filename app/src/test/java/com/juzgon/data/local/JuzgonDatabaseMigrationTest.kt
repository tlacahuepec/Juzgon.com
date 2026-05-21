package com.juzgon.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class JuzgonDatabaseMigrationTest {
    @get:Rule
    val helper =
        MigrationTestHelper(
            instrumentation = InstrumentationRegistry.getInstrumentation(),
            file = InstrumentationRegistry.getInstrumentation().targetContext.getDatabasePath("migration-test"),
            driver = AndroidSQLiteDriver(),
            databaseClass = JuzgonDatabase::class,
        )

    @Test
    fun migrate1To2_preservesCategoryRows() {
        val connection = helper.createDatabase(1)
        connection.prepare("INSERT INTO categories (name) VALUES ('$CATEGORY_NAME')").use { it.step() }
        connection.close()

        helper.runMigrationsAndValidate(2, listOf(DatabaseMigrations.MIGRATION_1_2)).use { conn ->
            conn.prepare("SELECT name FROM categories").use { stmt ->
                assertTrue(stmt.step())
                assertEquals(CATEGORY_NAME, stmt.getText(0))
            }
        }
    }

    @Test
    fun migrate1To2_validatesLatestSchema() {
        helper.createDatabase(1).close()
        helper.runMigrationsAndValidate(2, listOf(DatabaseMigrations.MIGRATION_1_2)).close()
    }

    @Test
    fun migrate2To3_preservesAttributeRowsWithDefaultPosition() {
        val connection = helper.createDatabase(2)
        val insertAttributeSql =
            "INSERT INTO attributes (id, category_name, weight) " +
                "VALUES ('$ATTRIBUTE_ID', '$CATEGORY_NAME', 1.5)"

        connection.prepare("INSERT INTO categories (name) VALUES ('$CATEGORY_NAME')").use { it.step() }
        connection.prepare(insertAttributeSql).use { it.step() }
        connection.close()

        helper.runMigrationsAndValidate(3, listOf(DatabaseMigrations.MIGRATION_2_3)).use { conn ->
            conn.prepare("SELECT id, category_name, weight, position FROM attributes").use { stmt ->
                assertTrue(stmt.step())
                assertEquals(ATTRIBUTE_ID, stmt.getText(0))
                assertEquals(CATEGORY_NAME, stmt.getText(1))
                assertEquals(1.5, stmt.getDouble(2), 0.0)
                assertEquals(0L, stmt.getLong(3))
            }
        }
    }

    @Test
    fun migrate2To3_validatesLatestSchema() {
        helper.createDatabase(2).close()
        helper.runMigrationsAndValidate(3, listOf(DatabaseMigrations.MIGRATION_2_3)).close()
    }

    @Test
    fun migrate3To4_preservesItemRowsWithDefaultBlankNotes() {
        val connection = helper.createDatabase(3)
        connection.prepare("INSERT INTO items (id) VALUES ('$ITEM_ID')").use { it.step() }
        connection.close()

        helper.runMigrationsAndValidate(4, listOf(DatabaseMigrations.MIGRATION_3_4)).use { conn ->
            conn.prepare("SELECT id, notes FROM items").use { stmt ->
                assertTrue(stmt.step())
                assertEquals(ITEM_ID, stmt.getText(0))
                assertEquals("", stmt.getText(1))
            }
        }
    }

    @Test
    fun migrate3To4_validatesLatestSchema() {
        helper.createDatabase(3).close()
        helper.runMigrationsAndValidate(4, listOf(DatabaseMigrations.MIGRATION_3_4)).close()
    }

    @Test
    fun migrate4To5_preservesItemRowsWithDefaultTimestamps() {
        val connection = helper.createDatabase(4)
        connection.prepare("INSERT INTO items (id, notes) VALUES ('$ITEM_ID', '$ITEM_NOTES')").use { it.step() }
        connection.close()

        helper.runMigrationsAndValidate(5, listOf(DatabaseMigrations.MIGRATION_4_5)).use { conn ->
            conn.prepare("SELECT id, notes, created_at, updated_at FROM items").use { stmt ->
                assertTrue(stmt.step())
                assertEquals(ITEM_ID, stmt.getText(0))
                assertEquals(ITEM_NOTES, stmt.getText(1))
                assertEquals(0L, stmt.getLong(2))
                assertEquals(0L, stmt.getLong(3))
            }
        }
    }

    @Test
    fun migrate4To5_validatesLatestSchema() {
        helper.createDatabase(4).close()
        helper.runMigrationsAndValidate(5, listOf(DatabaseMigrations.MIGRATION_4_5)).close()
    }

    @Test
    fun migrate5To6_preservesAttributeRowsWithDefaultTypeAndIsRequired() {
        val connection = helper.createDatabase(5)
        connection
            .prepare("INSERT INTO categories (name) VALUES ('$CATEGORY_NAME')")
            .use { it.step() }
        connection
            .prepare(
                "INSERT INTO attributes (id, category_name, weight, position) " +
                    "VALUES ('$ATTRIBUTE_ID', '$CATEGORY_NAME', 1.5, 0)",
            ).use { it.step() }
        connection.close()

        helper.runMigrationsAndValidate(6, listOf(DatabaseMigrations.MIGRATION_5_6)).use { conn ->
            conn.prepare("SELECT id, type, is_required FROM attributes").use { stmt ->
                assertTrue(stmt.step())
                assertEquals(ATTRIBUTE_ID, stmt.getText(0))
                assertEquals("NUMBER", stmt.getText(1))
                assertEquals(1L, stmt.getLong(2))
            }
        }
    }

    @Test
    fun migrate5To6_validatesLatestSchema() {
        helper.createDatabase(5).close()
        helper.runMigrationsAndValidate(6, listOf(DatabaseMigrations.MIGRATION_5_6)).close()
    }

    private companion object {
        const val ATTRIBUTE_ID = "taste"
        const val CATEGORY_NAME = "Coffee"
        const val ITEM_ID = "espresso"
        const val ITEM_NOTES = "rich"
    }
}
