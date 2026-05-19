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
        connection.prepare("INSERT INTO categories (name) VALUES ('$CATEGORY_NAME')").use { it.step() }
        connection
            .prepare(
                """
                INSERT INTO attributes (id, category_name, weight)
                VALUES ('$ATTRIBUTE_ID', '$CATEGORY_NAME', 1.5)
                """.trimIndent(),
            ).use { it.step() }
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

    private companion object {
        const val ATTRIBUTE_ID = "taste"
        const val CATEGORY_NAME = "Coffee"
    }
}
