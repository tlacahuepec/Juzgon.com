package com.juzgon.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.juzgon.data.local.entity.AttributeEntity
import com.juzgon.data.local.entity.CategoryEntity
import com.juzgon.data.local.entity.ItemEntity
import com.juzgon.data.local.entity.ItemValueEntity
import com.juzgon.data.local.entity.RatingEntity
import com.juzgon.data.local.entity.ScoreProfileAttributeEntity
import com.juzgon.data.local.entity.ScoreProfileEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DatabaseIntegrityRepositoryTest {
    private lateinit var database: JuzgonDatabase
    private lateinit var diagnostics: DatabaseIntegrityRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database =
            Room
                .inMemoryDatabaseBuilder(context, JuzgonDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        diagnostics =
            DatabaseIntegrityRepository(
                dao = database.databaseIntegrityDao(),
                sampleLimit = 2,
            )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun diagnose_reportsOrphanRatingsWithCappedSamples() =
        runTest {
            seedCategoryWithAttribute()
            database.itemDao().upsertItem(ItemEntity(id = "item-a"))
            database.itemDao().upsertItem(ItemEntity(id = "item-b"))
            database.itemDao().upsertItem(ItemEntity(id = "item-c"))
            database.itemDao().upsertRatings(
                listOf(
                    RatingEntity(itemId = "item-a", attributeId = "missing-a", score = 7),
                    RatingEntity(itemId = "item-b", attributeId = ATTRIBUTE_ID, score = 8),
                    RatingEntity(itemId = "item-b", attributeId = "missing-b", score = 9),
                    RatingEntity(itemId = "item-c", attributeId = "missing-c", score = 10),
                ),
            )

            val report = diagnostics.diagnose()

            assertEquals(3, report.orphanRatings.count)
            assertEquals(
                listOf("item-a:missing-a", "item-b:missing-b"),
                report.orphanRatings.sampleIds,
            )
        }

    @Test
    fun diagnose_reportsOrphanActiveItemValuesWithoutPayloads() =
        runTest {
            seedCategoryWithAttribute()
            database.itemDao().upsertItem(ItemEntity(id = "item-a"))
            database.itemDao().upsertItemValues(
                listOf(
                    ItemValueEntity(itemId = "item-a", attributeId = ATTRIBUTE_ID, valueText = "kept"),
                    ItemValueEntity(itemId = "item-a", attributeId = "missing", valueText = "private"),
                ),
            )

            val report = diagnostics.diagnose()

            assertEquals(1, report.orphanActiveItemValues.count)
            assertEquals(listOf("item-a:missing"), report.orphanActiveItemValues.sampleIds)
        }

    @Test
    fun diagnose_reportsOrphanSoftDeletedItemValuesWithoutPayloads() =
        runTest {
            seedCategoryWithAttribute()
            database.itemDao().upsertItem(ItemEntity(id = "item-a"))
            database.itemDao().upsertItemValues(
                listOf(
                    ItemValueEntity(
                        itemId = "item-a",
                        attributeId = "missing-a",
                        valueText = "private",
                        deletedAt = 1L,
                    ),
                    ItemValueEntity(
                        itemId = "item-a",
                        attributeId = "missing-b",
                        valueText = "private",
                        deletedAt = 2L,
                    ),
                    ItemValueEntity(
                        itemId = "item-a",
                        attributeId = "missing-c",
                        valueText = "private",
                        deletedAt = 3L,
                    ),
                ),
            )

            val report = diagnostics.diagnose()

            assertEquals(3, report.orphanSoftDeletedItemValues.count)
            assertEquals(
                listOf("item-a:missing-a", "item-a:missing-b"),
                report.orphanSoftDeletedItemValues.sampleIds,
            )
        }

    @Test
    fun diagnose_reportsScoreProfilesWithoutAttributes() =
        runTest {
            seedCategoryWithAttribute()
            database
                .scoreProfileDao()
                .upsertProfile(
                    ScoreProfileEntity(
                        id = "profile-empty",
                        categoryName = CATEGORY_NAME,
                        name = "Empty",
                    ),
                )

            val report = diagnostics.diagnose()

            assertEquals(1, report.scoreProfilesWithoutAttributes.count)
            assertEquals(listOf("profile-empty"), report.scoreProfilesWithoutAttributes.sampleIds)
        }

    @Test
    fun diagnose_reportsScoreProfileAttributesMissingAttributes() =
        runTest {
            seedCategoryWithAttribute()
            database
                .scoreProfileDao()
                .saveProfileWithAttributes(
                    profile =
                        ScoreProfileEntity(
                            id = "profile-a",
                            categoryName = CATEGORY_NAME,
                            name = "Broken",
                        ),
                    attributes = listOf(ScoreProfileAttributeEntity("profile-a", ATTRIBUTE_ID)),
                )
            insertScoreProfileAttributeWithForeignKeysDisabled("profile-a", "missing")

            val report = diagnostics.diagnose()

            assertEquals(1, report.scoreProfileAttributesMissingAttributes.count)
            assertEquals(
                listOf("profile-a:missing"),
                report.scoreProfileAttributesMissingAttributes.sampleIds,
            )
        }

    @Test
    fun diagnose_reportsCategoriesWithZeroRankableWeight() =
        runTest {
            database.categoryDao().upsertCategory(CategoryEntity(name = "NonZero"))
            database.categoryDao().upsertCategory(CategoryEntity(name = "Zero"))
            database.categoryDao().upsertAttributes(
                listOf(
                    AttributeEntity(id = "non-zero", categoryName = "NonZero", weight = 1.0),
                    AttributeEntity(id = "zero-number", categoryName = "Zero", weight = 0.0),
                    AttributeEntity(
                        id = "zero-date",
                        categoryName = "Zero",
                        weight = 0.0,
                        type = "DATE",
                        scoringDirection = "NEWER_IS_BETTER",
                    ),
                ),
            )

            val report = diagnostics.diagnose()

            assertEquals(1, report.categoriesWithZeroRankableWeight.count)
            assertEquals(listOf("Zero"), report.categoriesWithZeroRankableWeight.sampleIds)
        }

    private suspend fun seedCategoryWithAttribute() {
        database.categoryDao().upsertCategory(CategoryEntity(name = CATEGORY_NAME))
        database
            .categoryDao()
            .upsertAttributes(listOf(AttributeEntity(id = ATTRIBUTE_ID, categoryName = CATEGORY_NAME)))
    }

    private fun insertScoreProfileAttributeWithForeignKeysDisabled(
        profileId: String,
        attributeId: String,
    ) {
        val writableDatabase = database.openHelper.writableDatabase
        writableDatabase.execSQL("PRAGMA foreign_keys=OFF")
        writableDatabase.execSQL(
            "INSERT INTO score_profile_attributes (profile_id, attribute_id, position) " +
                "VALUES ('$profileId', '$attributeId', 1)",
        )
        writableDatabase.execSQL("PRAGMA foreign_keys=ON")
    }

    private companion object {
        const val ATTRIBUTE_ID = "Food/Taste"
        const val CATEGORY_NAME = "Food"
    }
}
