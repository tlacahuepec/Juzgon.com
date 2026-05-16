package com.juzgon.data.local.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.juzgon.data.local.JuzgonDatabase
import com.juzgon.data.local.entity.AttributeEntity
import com.juzgon.data.local.entity.CategoryEntity
import com.juzgon.data.local.entity.ItemEntity
import com.juzgon.data.local.entity.RatingEntity
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
class RatingDaoTest {
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
    fun getCategoryWithAttributes_returnsCompleteGraph() {
        categoryDao.insertCategory(CategoryEntity(name = CATEGORY_NAME))
        categoryDao.insertAttributes(foodAttributes())

        val result = categoryDao.getCategoryWithAttributes(CATEGORY_NAME)

        assertNotNull(result)
        assertEquals(CATEGORY_NAME, result?.category?.name)
        assertEquals(listOf("service", "taste"), result?.attributes?.map { it.id }?.sorted())
    }

    @Test
    fun getItemWithRatings_returnsCompleteGraph() {
        insertFoodCategoryWithAttributes()
        itemDao.insertItem(ItemEntity(id = ITEM_ID))
        itemDao.insertRatings(foodRatings())

        val result = itemDao.getItemWithRatings(ITEM_ID)

        assertNotNull(result)
        assertEquals(ITEM_ID, result?.item?.id)
        assertEquals(
            listOf("service:6", "taste:8"),
            result?.ratings?.map { it.toScorePair() }?.sorted(),
        )
    }

    @Test
    fun deleteCategory_cascadesToAttributes() {
        insertFoodCategoryWithAttributes()

        categoryDao.deleteCategory(CategoryEntity(name = CATEGORY_NAME))

        assertEquals(emptyList<AttributeEntity>(), categoryDao.getAttributesForCategory(CATEGORY_NAME))
    }

    @Test
    fun deleteItem_cascadesToRatings() {
        insertFoodCategoryWithAttributes()
        itemDao.insertItem(ItemEntity(id = ITEM_ID))
        itemDao.insertRatings(foodRatings())

        itemDao.deleteItem(ItemEntity(id = ITEM_ID))

        assertEquals(emptyList<RatingEntity>(), itemDao.getRatingsForItem(ITEM_ID))
    }

    private fun insertFoodCategoryWithAttributes() {
        categoryDao.insertCategory(CategoryEntity(name = CATEGORY_NAME))
        categoryDao.insertAttributes(foodAttributes())
    }

    private fun foodAttributes(): List<AttributeEntity> =
        listOf(
            AttributeEntity(id = "taste", categoryName = CATEGORY_NAME, weight = 1.5),
            AttributeEntity(id = "service", categoryName = CATEGORY_NAME, weight = 1.0),
        )

    private fun foodRatings(): List<RatingEntity> =
        listOf(
            RatingEntity(itemId = ITEM_ID, attributeId = "taste", score = 8),
            RatingEntity(itemId = ITEM_ID, attributeId = "service", score = 6),
        )

    private fun RatingEntity.toScorePair(): String = "$attributeId:$score"

    private companion object {
        const val CATEGORY_NAME = "Food"
        const val ITEM_ID = "item-1"
    }
}
