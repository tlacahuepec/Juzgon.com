package com.juzgon.data.local

import androidx.room.testing.MigrationTestHelper
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
            InstrumentationRegistry.getInstrumentation(),
            JuzgonDatabase::class.java,
        )

    @Test
    fun migrate1To2_preservesCategoryRows() {
        val originalDatabase = helper.createDatabase(PRESERVE_TEST_DB, 1)
        originalDatabase.execSQL("INSERT INTO categories (name) VALUES ('$CATEGORY_NAME')")
        originalDatabase.close()

        val migratedDatabase =
            helper.runMigrationsAndValidate(
                PRESERVE_TEST_DB,
                2,
                true,
                DatabaseMigrations.MIGRATION_1_2,
            )

        val cursor = migratedDatabase.query("SELECT name FROM categories")
        cursor.use {
            assertTrue(it.moveToFirst())
            assertEquals(CATEGORY_NAME, it.getString(0))
        }
        migratedDatabase.close()
    }

    @Test
    fun migrate1To2_validatesLatestSchema() {
        helper.createDatabase(VALIDATE_TEST_DB, 1).close()

        helper
            .runMigrationsAndValidate(
                VALIDATE_TEST_DB,
                2,
                true,
                DatabaseMigrations.MIGRATION_1_2,
            ).close()
    }

    private companion object {
        const val CATEGORY_NAME = "Coffee"
        const val PRESERVE_TEST_DB = "juzgon-migration-preserve"
        const val VALIDATE_TEST_DB = "juzgon-migration-validate"
    }
}
