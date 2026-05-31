package com.juzgon.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.juzgon.data.local.entity.AttributeEntity
import com.juzgon.data.local.entity.CategoryEntity
import com.juzgon.data.local.entity.ItemEntity
import com.juzgon.data.local.entity.ItemValueEntity
import com.juzgon.data.local.entity.RatingEntity
import com.juzgon.data.local.entity.ScoreProfileEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DatabaseMaintenanceRunnerTest {
    private lateinit var database: JuzgonDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database =
            Room
                .inMemoryDatabaseBuilder(context, JuzgonDatabase::class.java)
                .allowMainThreadQueries()
                .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun runCleanup_purgesConservativeRowsInTransactionAndReportsCounts() =
        runTest {
            seedCategoryAndItem()
            seedOrphansAndStaleData()

            val outcome = maintenanceRunner().runCleanup()
            assertTrue(outcome.isSuccess)
            val result = outcome.getOrNull()
            assertNotNull(result)
            assertEquals(1, result!!.oldSoftDeletedValuesPurged)
            assertEquals(1, result.orphanRatingsPurged)
            assertEquals(1, result.orphanSoftDeletedValuesPurged)
            assertEquals(1, result.scoreProfilesWithoutAttributesDeleted)
            assertEquals(1, result.diagnosticsBeforeCleanup.orphanRatings.count)
            assertEquals(1, result.diagnosticsBeforeCleanup.orphanActiveItemValues.count)

            val itemValues =
                database
                    .itemDao()
                    .observeItemsWithRatings()
                    .first()
                    .single()
                    .values
                    .map { it.attributeId to it.valueText }
                    .toSet()
            val expectedValues =
                setOf(
                    ATTRIBUTE_ID_2 to "recent",
                    "missing-active" to "keep",
                )
            assertEquals(expectedValues, itemValues)
            val expectedRatings =
                listOf(
                    RatingEntity(itemId = ITEM_ID, attributeId = ATTRIBUTE_ID, score = 8),
                )
            assertEquals(
                expectedRatings,
                database.itemDao().getRatingsForItem(ITEM_ID),
            )
            assertNull(database.scoreProfileDao().observeProfile("profile-empty").first())
        }

    @Test
    fun runCleanup_isIdempotentForCleanDatabase() =
        runTest {
            seedCategoryAndItem()

            val first = maintenanceRunner().runCleanup()
            val second = maintenanceRunner().runCleanup()

            assertEquals(0, first?.getOrNull()?.oldSoftDeletedValuesPurged)
            assertEquals(0, first?.getOrNull()?.orphanRatingsPurged)
            assertEquals(0, first?.getOrNull()?.orphanSoftDeletedValuesPurged)
            assertEquals(0, first?.getOrNull()?.scoreProfilesWithoutAttributesDeleted)
            assertEquals(first, second)
        }

    private suspend fun seedCategoryAndItem() {
        database.categoryDao().upsertCategory(CategoryEntity(CATEGORY_NAME))
        database.categoryDao().upsertAttributes(
            listOf(
                AttributeEntity(id = ATTRIBUTE_ID, categoryName = CATEGORY_NAME),
                AttributeEntity(id = ATTRIBUTE_ID_2, categoryName = CATEGORY_NAME),
            ),
        )
        database.itemDao().upsertItem(ItemEntity(id = ITEM_ID))
    }

    private suspend fun seedOrphansAndStaleData() {
        database.itemDao().upsertRatings(
            listOf(
                RatingEntity(itemId = ITEM_ID, attributeId = ATTRIBUTE_ID, score = 8),
                RatingEntity(itemId = ITEM_ID, attributeId = "missing-rating", score = 9),
            ),
        )
        database.itemDao().upsertItemValues(
            listOf(
                ItemValueEntity(
                    itemId = ITEM_ID,
                    attributeId = ATTRIBUTE_ID,
                    valueText = "old",
                    deletedAt = 1L,
                ),
                ItemValueEntity(
                    itemId = ITEM_ID,
                    attributeId = ATTRIBUTE_ID_2,
                    valueText = "recent",
                    deletedAt = NOW,
                ),
                ItemValueEntity(
                    itemId = ITEM_ID,
                    attributeId = "missing-active",
                    valueText = "keep",
                ),
                ItemValueEntity(
                    itemId = ITEM_ID,
                    attributeId = "missing-soft",
                    valueText = "purge",
                    deletedAt = NOW,
                ),
            ),
        )
        database
            .scoreProfileDao()
            .upsertProfile(
                ScoreProfileEntity(
                    id = "profile-empty",
                    categoryName = CATEGORY_NAME,
                    name = "Empty",
                ),
            )
    }

    private fun maintenanceRunner(): DatabaseMaintenanceRunner =
        DatabaseMaintenanceRunner(
            database = database,
            currentTimeMillis = { NOW },
        )

    private companion object {
        const val CATEGORY_NAME = "Food"
        const val ATTRIBUTE_ID = "Food/Taste"
        const val ATTRIBUTE_ID_2 = "Food/Notes"
        const val ITEM_ID = "item-a"
        const val NOW = 3_000_000_000L
    }
}
