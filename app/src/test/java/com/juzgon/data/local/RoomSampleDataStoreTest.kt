package com.juzgon.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RoomSampleDataStoreTest {
    private lateinit var database: JuzgonDatabase
    private lateinit var sampleDataStore: RoomSampleDataStore

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database =
            Room
                .inMemoryDatabaseBuilder(context, JuzgonDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        sampleDataStore = RoomSampleDataStore(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun seed_insertsReadableSampleData() =
        runTest {
            sampleDataStore.seed()

            val category = database.categoryDao().getCategoryWithAttributes(SAMPLE_CATEGORY_NAME)
            val item = database.itemDao().getItemWithRatings(SAMPLE_ITEM_ID)

            assertNotNull(category)
            assertEquals(
                listOf("ambience", "service", "taste"),
                category?.attributes?.map { it.id }?.sorted(),
            )
            assertNotNull(item)
            assertEquals(
                listOf("ambience:8", "service:9", "taste:8"),
                item?.ratings?.map { "${it.attributeId}:${it.score}" }?.sorted(),
            )
        }

    @Test
    fun seed_isIdempotent() =
        runTest {
            sampleDataStore.seed()
            sampleDataStore.seed()

            val categories = database.categoryDao().observeCategoriesWithAttributes().first()
            val items = database.itemDao().observeItemsWithRatings().first()

            assertEquals(listOf(SAMPLE_CATEGORY_NAME), categories.map { it.category.name })
            assertEquals(listOf(3), categories.map { it.attributes.size })
            assertEquals(listOf(SAMPLE_ITEM_ID), items.map { it.item.id })
            assertEquals(listOf(3), items.map { it.ratings.size })
        }
}
