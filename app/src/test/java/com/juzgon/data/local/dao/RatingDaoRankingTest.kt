package com.juzgon.data.local.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.juzgon.data.local.JuzgonDatabase
import com.juzgon.data.local.entity.AttributeEntity
import com.juzgon.data.local.entity.CategoryEntity
import com.juzgon.data.local.entity.ItemEntity
import com.juzgon.data.local.entity.RatingEntity
import kotlinx.coroutines.flow.first
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
class RatingDaoRankingTest {
    private lateinit var database: JuzgonDatabase
    private lateinit var categoryDao: CategoryDao
    private lateinit var itemDao: ItemDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database =
            Room
                .inMemoryDatabaseBuilder(context, JuzgonDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        categoryDao = database.categoryDao()
        itemDao = database.itemDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun observeRankedItemsForCategory_ordersByScoreThenItemIdAndExcludesOtherCategories() =
        runTest {
            insertCategoriesAndAttributes()
            itemDao.upsertItem(ItemEntity(id = "item-high"))
            itemDao.upsertItem(ItemEntity(id = "item-b"))
            itemDao.upsertItem(ItemEntity(id = "item-a"))
            itemDao.upsertItem(ItemEntity(id = "item-mixed"))
            itemDao.upsertItem(ItemEntity(id = "item-other"))
            itemDao.upsertRatings(
                listOf(
                    RatingEntity(itemId = "item-high", attributeId = TASTE, score = 10),
                    RatingEntity(itemId = "item-high", attributeId = SERVICE, score = 8),
                    RatingEntity(itemId = "item-b", attributeId = TASTE, score = 8),
                    RatingEntity(itemId = "item-b", attributeId = SERVICE, score = 8),
                    RatingEntity(itemId = "item-a", attributeId = TASTE, score = 8),
                    RatingEntity(itemId = "item-a", attributeId = SERVICE, score = 8),
                    RatingEntity(itemId = "item-mixed", attributeId = TASTE, score = 7),
                    RatingEntity(itemId = "item-mixed", attributeId = AROMA, score = 10),
                    RatingEntity(itemId = "item-other", attributeId = AROMA, score = 10),
                ),
            )

            val rankedItems = itemDao.observeRankedItemsForCategory(FOOD_CATEGORY).first()

            assertEquals(
                listOf("item-high", "item-a", "item-b", "item-mixed"),
                rankedItems.map { it.item.id },
            )
            assertEquals(listOf(9.0, 8.0, 8.0, 7.0), rankedItems.map { it.aggregateScore })
        }

    private suspend fun insertCategoriesAndAttributes() {
        categoryDao.upsertCategory(CategoryEntity(name = FOOD_CATEGORY))
        categoryDao.upsertCategory(CategoryEntity(name = COFFEE_CATEGORY))
        categoryDao.upsertAttributes(
            listOf(
                AttributeEntity(id = TASTE, categoryName = FOOD_CATEGORY),
                AttributeEntity(id = SERVICE, categoryName = FOOD_CATEGORY),
                AttributeEntity(id = AROMA, categoryName = COFFEE_CATEGORY),
            ),
        )
    }

    private companion object {
        const val FOOD_CATEGORY = "Food"
        const val COFFEE_CATEGORY = "Coffee"
        const val TASTE = "taste"
        const val SERVICE = "service"
        const val AROMA = "aroma"
    }
}
