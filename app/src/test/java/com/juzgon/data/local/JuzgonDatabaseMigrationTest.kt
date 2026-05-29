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

    @Test
    fun migrate6To7_createsItemValuesTableWithCorrectColumns() {
        val connection = helper.createDatabase(6)
        connection.prepare("INSERT INTO categories (name) VALUES ('$CATEGORY_NAME')").use { it.step() }
        connection
            .prepare(
                "INSERT INTO attributes (id, category_name, weight, position, type, is_required) " +
                    "VALUES ('$ATTRIBUTE_ID', '$CATEGORY_NAME', 1.0, 0, 'NUMBER', 1)",
            ).use { it.step() }
        connection
            .prepare("INSERT INTO items (id, notes, created_at, updated_at) VALUES ('$ITEM_ID', '', 0, 0)")
            .use { it.step() }
        connection.close()

        helper.runMigrationsAndValidate(7, listOf(DatabaseMigrations.MIGRATION_6_7)).use { conn ->
            val insertValueSql =
                "INSERT INTO item_values (item_id, attribute_id, value_text) " +
                    "VALUES ('$ITEM_ID', '$ATTRIBUTE_ID', 'blue')"
            conn
                .prepare(insertValueSql)
                .use { it.step() }
            conn.prepare("SELECT item_id, attribute_id, value_text FROM item_values").use { stmt ->
                assertTrue(stmt.step())
                assertEquals(ITEM_ID, stmt.getText(0))
                assertEquals(ATTRIBUTE_ID, stmt.getText(1))
                assertEquals("blue", stmt.getText(2))
            }
        }
    }

    @Test
    fun migrate6To7_validatesLatestSchema() {
        helper.createDatabase(6).close()
        helper.runMigrationsAndValidate(7, listOf(DatabaseMigrations.MIGRATION_6_7)).close()
    }

    @Test
    fun migrate7To8_createsAttributeRankSnapshotsTableWithCorrectColumns() {
        val connection = helper.createDatabase(7)
        connection.prepare("INSERT INTO categories (name) VALUES ('$CATEGORY_NAME')").use { it.step() }
        connection
            .prepare(
                "INSERT INTO attributes (id, category_name, weight, position, type, is_required) " +
                    "VALUES ('$ATTRIBUTE_ID', '$CATEGORY_NAME', 1.0, 0, 'NUMBER', 1)",
            ).use { it.step() }
        connection
            .prepare("INSERT INTO items (id, notes, created_at, updated_at) VALUES ('$ITEM_ID', '', 0, 0)")
            .use { it.step() }
        connection.close()

        helper.runMigrationsAndValidate(8, listOf(DatabaseMigrations.MIGRATION_7_8)).use { conn ->
            val insertSnapshotSql =
                "INSERT INTO attribute_rank_snapshots " +
                    "(item_id, captured_at, attribute_id, value, rank) " +
                    "VALUES ('$ITEM_ID', 1500, '$ATTRIBUTE_ID', 8, 1)"
            conn
                .prepare(insertSnapshotSql)
                .use { it.step() }
            conn
                .prepare("SELECT item_id, captured_at, attribute_id, value, rank FROM attribute_rank_snapshots")
                .use { stmt ->
                    assertTrue(stmt.step())
                    assertEquals(ITEM_ID, stmt.getText(0))
                    assertEquals(1_500L, stmt.getLong(1))
                    assertEquals(ATTRIBUTE_ID, stmt.getText(2))
                    assertEquals(8L, stmt.getLong(3))
                    assertEquals(1L, stmt.getLong(4))
                }
        }
    }

    @Test
    fun migrate7To8_validatesLatestSchema() {
        helper.createDatabase(7).close()
        helper.runMigrationsAndValidate(8, listOf(DatabaseMigrations.MIGRATION_7_8)).close()
    }

    @Test
    fun migrate8To9_addsDiamondChartAttributeColumns() {
        val connection = helper.createDatabase(8)
        connection.prepare("INSERT INTO categories (name) VALUES ('$CATEGORY_NAME')").use { it.step() }
        connection
            .prepare(
                "INSERT INTO attributes (id, category_name, weight, position, type, is_required) " +
                    "VALUES ('$ATTRIBUTE_ID', '$CATEGORY_NAME', 1.0, 0, 'NUMBER', 1)",
            ).use { it.step() }
        connection.close()

        helper.runMigrationsAndValidate(9, listOf(DatabaseMigrations.MIGRATION_8_9)).use { conn ->
            conn.prepare("SELECT display_in_diamond, diamond_order FROM attributes").use { stmt ->
                assertTrue(stmt.step())
                assertEquals(1L, stmt.getLong(0))
                assertTrue(stmt.isNull(1))
            }
        }
    }

    @Test
    fun migrate8To9_validatesLatestSchema() {
        helper.createDatabase(8).close()
        helper.runMigrationsAndValidate(9, listOf(DatabaseMigrations.MIGRATION_8_9)).close()
    }

    @Test
    fun migrate11To12_scopesAttributeIdsWithCategoryPrefix() {
        val connection = helper.createDatabase(11)
        connection.prepare("INSERT INTO categories (name) VALUES ('$CATEGORY_NAME')").use { it.step() }
        connection
            .prepare(
                "INSERT INTO attributes (id, category_name, weight, position, type, is_required, " +
                    "display_in_diamond) VALUES ('$ATTRIBUTE_ID', '$CATEGORY_NAME', 1.0, 0, " +
                    "'NUMBER', 1, 1)",
            ).use { it.step() }
        connection.close()

        helper.runMigrationsAndValidate(12, listOf(DatabaseMigrations.MIGRATION_11_12)).use { conn ->
            conn.prepare("SELECT id, category_name FROM attributes").use { stmt ->
                assertTrue(stmt.step())
                assertEquals("$CATEGORY_NAME/$ATTRIBUTE_ID", stmt.getText(0))
                assertEquals(CATEGORY_NAME, stmt.getText(1))
            }
        }
    }

    @Test
    fun migrate11To12_preservesRatingsWithUpdatedReferences() {
        val connection = helper.createDatabase(11)
        connection.prepare("INSERT INTO categories (name) VALUES ('$CATEGORY_NAME')").use { it.step() }
        connection
            .prepare(
                "INSERT INTO attributes (id, category_name, weight, position, type, is_required, " +
                    "display_in_diamond) VALUES ('$ATTRIBUTE_ID', '$CATEGORY_NAME', 1.0, 0, " +
                    "'NUMBER', 1, 1)",
            ).use { it.step() }
        connection
            .prepare("INSERT INTO items (id, notes, created_at, updated_at) VALUES ('$ITEM_ID', '', 0, 0)")
            .use { it.step() }
        connection
            .prepare(
                "INSERT INTO ratings (item_id, attribute_id, score) " +
                    "VALUES ('$ITEM_ID', '$ATTRIBUTE_ID', 9)",
            ).use { it.step() }
        connection.close()

        helper.runMigrationsAndValidate(12, listOf(DatabaseMigrations.MIGRATION_11_12)).use { conn ->
            conn.prepare("SELECT attribute_id, score FROM ratings").use { stmt ->
                assertTrue(stmt.step())
                assertEquals("$CATEGORY_NAME/$ATTRIBUTE_ID", stmt.getText(0))
                assertEquals(9L, stmt.getLong(1))
            }
        }
    }

    @Test
    fun migrate11To12_preservesItemValuesWithUpdatedReferences() {
        val connection = helper.createDatabase(11)
        connection.prepare("INSERT INTO categories (name) VALUES ('$CATEGORY_NAME')").use { it.step() }
        connection
            .prepare(
                "INSERT INTO attributes (id, category_name, weight, position, type, is_required, " +
                    "display_in_diamond) VALUES ('notes_attr', '$CATEGORY_NAME', 1.0, 1, " +
                    "'NOTES', 0, 0)",
            ).use { it.step() }
        connection
            .prepare("INSERT INTO items (id, notes, created_at, updated_at) VALUES ('$ITEM_ID', '', 0, 0)")
            .use { it.step() }
        connection
            .prepare(
                "INSERT INTO item_values (item_id, attribute_id, value_text) " +
                    "VALUES ('$ITEM_ID', 'notes_attr', 'some note')",
            ).use { it.step() }
        connection.close()

        helper.runMigrationsAndValidate(12, listOf(DatabaseMigrations.MIGRATION_11_12)).use { conn ->
            conn.prepare("SELECT attribute_id, value_text FROM item_values").use { stmt ->
                assertTrue(stmt.step())
                assertEquals("$CATEGORY_NAME/notes_attr", stmt.getText(0))
                assertEquals("some note", stmt.getText(1))
            }
        }
    }

    @Test
    fun migrate11To12_preservesScoreProfileAttributesWithUpdatedReferences() {
        val connection = helper.createDatabase(11)
        connection.prepare("INSERT INTO categories (name) VALUES ('$CATEGORY_NAME')").use { it.step() }
        connection
            .prepare(
                "INSERT INTO attributes (id, category_name, weight, position, type, is_required, " +
                    "display_in_diamond) VALUES ('$ATTRIBUTE_ID', '$CATEGORY_NAME', 1.0, 0, " +
                    "'NUMBER', 1, 1)",
            ).use { it.step() }
        connection
            .prepare(
                "INSERT INTO score_profiles (id, category_name, name, created_at, updated_at) " +
                    "VALUES ('p1', '$CATEGORY_NAME', 'Default', 0, 0)",
            ).use { it.step() }
        connection
            .prepare(
                "INSERT INTO score_profile_attributes (profile_id, attribute_id, position) " +
                    "VALUES ('p1', '$ATTRIBUTE_ID', 0)",
            ).use { it.step() }
        connection.close()

        helper.runMigrationsAndValidate(12, listOf(DatabaseMigrations.MIGRATION_11_12)).use { conn ->
            conn.prepare("SELECT attribute_id FROM score_profile_attributes").use { stmt ->
                assertTrue(stmt.step())
                assertEquals("$CATEGORY_NAME/$ATTRIBUTE_ID", stmt.getText(0))
            }
        }
    }

    @Test
    fun migrate11To12_validatesLatestSchema() {
        helper.createDatabase(11).close()
        helper.runMigrationsAndValidate(12, listOf(DatabaseMigrations.MIGRATION_11_12)).close()
    }

    @Test
    fun migrate12To13_preservesItemValuesAndAddsDeletedAtColumn() {
        val connection = helper.createDatabase(12)
        connection.prepare("INSERT INTO categories (name) VALUES ('$CATEGORY_NAME')").use { it.step() }
        connection
            .prepare(
                "INSERT INTO attributes (id, category_name, weight, position, type, is_required, " +
                    "display_in_diamond) VALUES ('$CATEGORY_NAME/$ATTRIBUTE_ID', '$CATEGORY_NAME', " +
                    "1.0, 0, 'NUMBER', 1, 1)",
            ).use { it.step() }
        connection
            .prepare("INSERT INTO items (id, notes, created_at, updated_at) VALUES ('$ITEM_ID', '', 0, 0)")
            .use { it.step() }
        connection
            .prepare(
                "INSERT INTO item_values (item_id, attribute_id, value_text) " +
                    "VALUES ('$ITEM_ID', '$CATEGORY_NAME/$ATTRIBUTE_ID', 'blue')",
            ).use { it.step() }
        connection.close()

        helper.runMigrationsAndValidate(13, listOf(DatabaseMigrations.MIGRATION_12_13)).use { conn ->
            conn.prepare("SELECT item_id, attribute_id, value_text, deleted_at FROM item_values").use { stmt ->
                assertTrue(stmt.step())
                assertEquals(ITEM_ID, stmt.getText(0))
                assertEquals("$CATEGORY_NAME/$ATTRIBUTE_ID", stmt.getText(1))
                assertEquals("blue", stmt.getText(2))
                assertTrue(stmt.isNull(3))
            }
        }
    }

    @Test
    fun migrate12To13_preservesRatingsWithoutAttributeFk() {
        val connection = helper.createDatabase(12)
        connection.prepare("INSERT INTO categories (name) VALUES ('$CATEGORY_NAME')").use { it.step() }
        connection
            .prepare(
                "INSERT INTO attributes (id, category_name, weight, position, type, is_required, " +
                    "display_in_diamond) VALUES ('$CATEGORY_NAME/$ATTRIBUTE_ID', '$CATEGORY_NAME', " +
                    "1.0, 0, 'NUMBER', 1, 1)",
            ).use { it.step() }
        connection
            .prepare("INSERT INTO items (id, notes, created_at, updated_at) VALUES ('$ITEM_ID', '', 0, 0)")
            .use { it.step() }
        connection
            .prepare(
                "INSERT INTO ratings (item_id, attribute_id, score) " +
                    "VALUES ('$ITEM_ID', '$CATEGORY_NAME/$ATTRIBUTE_ID', 9)",
            ).use { it.step() }
        connection.close()

        helper.runMigrationsAndValidate(13, listOf(DatabaseMigrations.MIGRATION_12_13)).use { conn ->
            conn.prepare("SELECT item_id, attribute_id, score FROM ratings").use { stmt ->
                assertTrue(stmt.step())
                assertEquals(ITEM_ID, stmt.getText(0))
                assertEquals("$CATEGORY_NAME/$ATTRIBUTE_ID", stmt.getText(1))
                assertEquals(9L, stmt.getLong(2))
            }
        }
    }

    @Test
    fun migrate12To13_validatesLatestSchema() {
        helper.createDatabase(12).close()
        helper.runMigrationsAndValidate(13, listOf(DatabaseMigrations.MIGRATION_12_13)).close()
    }

    @Test
    fun migrate13To14_fixesMislinkedItemValues() {
        val connection = helper.createDatabase(13)
        connection.prepare("INSERT INTO categories (name) VALUES ('$CATEGORY_NAME')").use { it.step() }
        connection.prepare("INSERT INTO categories (name) VALUES ('Tea')").use { it.step() }
        connection
            .prepare(
                "INSERT INTO attributes (id, category_name, weight, position, type, is_required, " +
                    "display_in_diamond) VALUES ('$CATEGORY_NAME/$ATTRIBUTE_ID', '$CATEGORY_NAME', " +
                    "1.0, 0, 'NUMBER', 1, 1)",
            ).use { it.step() }
        connection
            .prepare(
                "INSERT INTO attributes (id, category_name, weight, position, type, is_required, " +
                    "display_in_diamond) VALUES ('$CATEGORY_NAME/Nationality', '$CATEGORY_NAME', " +
                    "1.0, 1, 'NATIONALITY', 0, 0)",
            ).use { it.step() }
        connection
            .prepare("INSERT INTO items (id, notes, created_at, updated_at) VALUES ('$ITEM_ID', '', 0, 0)")
            .use { it.step() }
        connection
            .prepare(
                "INSERT INTO ratings (item_id, attribute_id, score) " +
                    "VALUES ('$ITEM_ID', '$CATEGORY_NAME/$ATTRIBUTE_ID', 8)",
            ).use { it.step() }
        // Simulate mislinked item_value: wrong category prefix
        connection
            .prepare(
                "INSERT INTO item_values (item_id, attribute_id, value_text) " +
                    "VALUES ('$ITEM_ID', 'Tea/Nationality', 'MX')",
            ).use { it.step() }
        connection.close()

        helper.runMigrationsAndValidate(14, listOf(DatabaseMigrations.MIGRATION_13_14)).use { conn ->
            conn.prepare("SELECT attribute_id, value_text FROM item_values").use { stmt ->
                assertTrue(stmt.step())
                assertEquals("$CATEGORY_NAME/Nationality", stmt.getText(0))
                assertEquals("MX", stmt.getText(1))
            }
        }
    }

    @Test
    fun migrate13To14_validatesLatestSchema() {
        helper.createDatabase(13).close()
        helper.runMigrationsAndValidate(14, listOf(DatabaseMigrations.MIGRATION_13_14)).close()
    }

    @Test
    fun migrate14To15_repairsValueOnlyItemWhenSuffixMatchIsUnique() {
        val connection = helper.createDatabase(14)
        insertV14Category(connection, "Coffee")
        insertV14Category(connection, "Tea")
        insertV14Attribute(connection, "Coffee/Nationality", "Coffee", type = "NATIONALITY")
        insertV14Item(connection, ITEM_ID)
        insertV14ItemValue(connection, ITEM_ID, "Tea/Nationality", "MX")
        connection.close()

        helper.runMigrationsAndValidate(15, listOf(DatabaseMigrations.MIGRATION_14_15)).use { conn ->
            conn.prepare("SELECT attribute_id, value_text FROM item_values").use { stmt ->
                assertTrue(stmt.step())
                assertEquals("Coffee/Nationality", stmt.getText(0))
                assertEquals("MX", stmt.getText(1))
            }
        }
    }

    @Test
    fun migrate14To15_leavesAmbiguousSuffixMatchesUntouched() {
        val connection = helper.createDatabase(14)
        insertV14Category(connection, "Coffee")
        insertV14Category(connection, "Tea")
        insertV14Attribute(connection, "Coffee/Nationality", "Coffee", type = "NATIONALITY")
        insertV14Attribute(connection, "Tea/Nationality", "Tea", type = "NATIONALITY")
        insertV14Item(connection, ITEM_ID)
        insertV14ItemValue(connection, ITEM_ID, "Old/Nationality", "MX")
        connection.close()

        helper.runMigrationsAndValidate(15, listOf(DatabaseMigrations.MIGRATION_14_15)).use { conn ->
            conn.prepare("SELECT attribute_id, value_text FROM item_values").use { stmt ->
                assertTrue(stmt.step())
                assertEquals("Old/Nationality", stmt.getText(0))
                assertEquals("MX", stmt.getText(1))
            }
        }
    }

    @Test
    fun migrate14To15_leavesPrimaryKeyCollisionsUntouched() {
        val connection = helper.createDatabase(14)
        insertV14Category(connection, "Coffee")
        insertV14Category(connection, "Tea")
        insertV14Attribute(connection, "Coffee/Nationality", "Coffee", type = "NATIONALITY")
        insertV14Item(connection, ITEM_ID)
        insertV14ItemValue(connection, ITEM_ID, "Coffee/Nationality", "US")
        insertV14ItemValue(connection, ITEM_ID, "Tea/Nationality", "MX")
        connection.close()

        helper.runMigrationsAndValidate(15, listOf(DatabaseMigrations.MIGRATION_14_15)).use { conn ->
            conn.prepare("SELECT attribute_id, value_text FROM item_values ORDER BY attribute_id").use { stmt ->
                assertTrue(stmt.step())
                assertEquals("Coffee/Nationality", stmt.getText(0))
                assertEquals("US", stmt.getText(1))
                assertTrue(stmt.step())
                assertEquals("Tea/Nationality", stmt.getText(0))
                assertEquals("MX", stmt.getText(1))
            }
        }
    }

    @Test
    fun migrate14To15_validatesLatestSchema() {
        helper.createDatabase(14).close()
        helper.runMigrationsAndValidate(15, listOf(DatabaseMigrations.MIGRATION_14_15)).close()
    }

    @Test
    fun migrate15To16_addsCategoryDescriptionAndTypeColumns() {
        val connection = helper.createDatabase(15)
        connection.prepare("INSERT INTO categories (name) VALUES ('$CATEGORY_NAME')").use { it.step() }
        connection.close()

        helper.runMigrationsAndValidate(16, listOf(DatabaseMigrations.MIGRATION_15_16)).use { conn ->
            conn.prepare("SELECT name, description, type FROM categories").use { stmt ->
                assertTrue(stmt.step())
                assertEquals(CATEGORY_NAME, stmt.getText(0))
                assertTrue(stmt.isNull(1))
                assertTrue(stmt.isNull(2))
            }
        }
    }

    @Test
    fun migrate15To16_validatesLatestSchema() {
        helper.createDatabase(15).close()
        helper.runMigrationsAndValidate(16, listOf(DatabaseMigrations.MIGRATION_15_16)).close()
    }

    @Test
    fun migrate16To17_createsEnrichmentSuggestionCache() {
        val connection = helper.createDatabase(16)
        connection.close()

        helper.runMigrationsAndValidate(17, listOf(DatabaseMigrations.MIGRATION_16_17)).use { conn ->
            conn
                .prepare(
                    """
                    INSERT INTO enrichment_suggestion_cache (
                        cacheKeyHash,
                        catalogId,
                        itemIdentity,
                        targetAttributeKey,
                        knownAttributesFingerprint,
                        status,
                        cachedAt
                    ) VALUES (
                        'cache-key',
                        'restaurants',
                        'Sample bistro',
                        'Restaurants/taste',
                        'known',
                        'HIT',
                        1000
                    )
                    """.trimIndent(),
                ).use { it.step() }
            conn.prepare("SELECT catalogId FROM enrichment_suggestion_cache").use { stmt ->
                assertTrue(stmt.step())
                assertEquals("restaurants", stmt.getText(0))
            }
        }
    }

    @Test
    fun migrate16To17_validatesLatestSchema() {
        helper.createDatabase(16).close()
        helper.runMigrationsAndValidate(17, listOf(DatabaseMigrations.MIGRATION_16_17)).close()
    }

    private fun insertV14Category(
        connection: androidx.sqlite.SQLiteConnection,
        name: String,
    ) {
        connection.prepare("INSERT INTO categories (name) VALUES ('$name')").use { it.step() }
    }

    private fun insertV14Attribute(
        connection: androidx.sqlite.SQLiteConnection,
        id: String,
        categoryName: String,
        type: String = "NUMBER",
    ) {
        connection
            .prepare(
                "INSERT INTO attributes (id, category_name, weight, position, type, is_required, " +
                    "display_in_diamond) VALUES ('$id', '$categoryName', 1.0, 0, '$type', 0, 0)",
            ).use { it.step() }
    }

    private fun insertV14Item(
        connection: androidx.sqlite.SQLiteConnection,
        id: String,
    ) {
        connection
            .prepare("INSERT INTO items (id, notes, created_at, updated_at) VALUES ('$id', '', 0, 0)")
            .use { it.step() }
    }

    private fun insertV14ItemValue(
        connection: androidx.sqlite.SQLiteConnection,
        itemId: String,
        attributeId: String,
        value: String,
    ) {
        connection
            .prepare(
                "INSERT INTO item_values (item_id, attribute_id, value_text) " +
                    "VALUES ('$itemId', '$attributeId', '$value')",
            ).use { it.step() }
    }

    private companion object {
        const val ATTRIBUTE_ID = "taste"
        const val CATEGORY_NAME = "Coffee"
        const val ITEM_ID = "espresso"
        const val ITEM_NOTES = "rich"
    }
}
