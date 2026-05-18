package com.juzgon.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.juzgon.data.local.JuzgonDatabase
import com.juzgon.domain.Attribute
import com.juzgon.domain.Category
import com.juzgon.domain.RatedItem
import com.juzgon.domain.ScoreEntry
import com.juzgon.domain.repository.CategoryRepository
import com.juzgon.domain.repository.RatedItemRepository
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
class RoomRatingRepositoryFlowStabilityTest {
    private lateinit var database: JuzgonDatabase
    private lateinit var categoryRepository: CategoryRepository
    private lateinit var ratedItemRepository: RatedItemRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database =
            Room
                .inMemoryDatabaseBuilder(context, JuzgonDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        categoryRepository = RoomCategoryRepository(database)
        ratedItemRepository = RoomRatedItemRepository(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun observeCategories_emitsCreateUpdateDeleteWithoutNoOpDuplicates() =
        runTest {
            categoryRepository.observeCategories().test {
                assertEquals(emptyList<Category>(), awaitItem())

                categoryRepository.saveCategory(foodCategory())
                assertEquals(listOf(foodCategory()), awaitItem())

                categoryRepository.saveCategory(foodCategory())
                expectNoEvents()

                categoryRepository.saveCategory(updatedFoodCategory())
                assertEquals(listOf(updatedFoodCategory()), awaitItem())

                categoryRepository.deleteCategory(FOOD_CATEGORY)
                assertEquals(emptyList<Category>(), awaitItem())
            }
        }

    @Test
    fun observeCategory_emitsCreateDeleteWithoutNoOpDuplicates() =
        runTest {
            categoryRepository.observeCategory(FOOD_CATEGORY).test {
                assertEquals(null, awaitItem())

                categoryRepository.saveCategory(foodCategory())
                assertEquals(foodCategory(), awaitItem())

                categoryRepository.saveCategory(foodCategory())
                expectNoEvents()

                categoryRepository.deleteCategory(FOOD_CATEGORY)
                assertEquals(null, awaitItem())
            }
        }

    @Test
    fun observeRatedItems_emitsCreateUpdateDeleteWithoutNoOpDuplicates() =
        runTest {
            categoryRepository.saveCategory(foodCategory())

            ratedItemRepository.observeRatedItems().test {
                assertEquals(emptyList<RatedItem>(), awaitItem())

                ratedItemRepository.saveRatedItem(foodItem())
                assertRatedItemsEqual(listOf(foodItem()), awaitItem())

                ratedItemRepository.saveRatedItem(foodItem(scores = listOf(SERVICE to 6, TASTE to 8)))
                expectNoEvents()

                ratedItemRepository.saveRatedItem(updatedFoodItem())
                assertRatedItemsEqual(listOf(updatedFoodItem()), awaitItem())

                ratedItemRepository.deleteRatedItem(ITEM_ID)
                assertEquals(emptyList<RatedItem>(), awaitItem())
            }
        }

    @Test
    fun observeRatedItem_emitsCreateDeleteWithoutNoOpDuplicates() =
        runTest {
            categoryRepository.saveCategory(foodCategory())

            ratedItemRepository.observeRatedItem(ITEM_ID).test {
                assertEquals(null, awaitItem())

                ratedItemRepository.saveRatedItem(foodItem())
                assertRatedItemEquals(foodItem(), awaitItem())

                ratedItemRepository.saveRatedItem(foodItem(scores = listOf(SERVICE to 6, TASTE to 8)))
                expectNoEvents()

                ratedItemRepository.deleteRatedItem(ITEM_ID)
                assertEquals(null, awaitItem())
            }
        }

    private fun assertRatedItemsEqual(
        expected: List<RatedItem>,
        actual: List<RatedItem>,
    ) {
        assertEquals(expected.map { it.toScorePairs() }, actual.map { it.toScorePairs() })
    }

    private fun assertRatedItemEquals(
        expected: RatedItem,
        actual: RatedItem?,
    ) {
        assertEquals(expected.toScorePairs(), actual?.toScorePairs())
    }

    private fun foodCategory(attributes: List<Attribute> = foodAttributes()): Category =
        Category(name = FOOD_CATEGORY, attributes = attributes)

    private fun updatedFoodCategory(): Category = foodCategory(attributes = updatedFoodAttributes())

    private fun foodAttributes(): List<Attribute> =
        listOf(
            Attribute(SERVICE),
            Attribute(TASTE),
        )

    private fun updatedFoodAttributes(): List<Attribute> =
        listOf(
            Attribute(AMBIENCE),
            Attribute(TASTE),
        )

    private fun foodItem(scores: List<Pair<String, Int>> = listOf(TASTE to 8, SERVICE to 6)): RatedItem =
        RatedItem(
            id = ITEM_ID,
            scores = scores.map { (attributeId, score) -> ScoreEntry(Attribute(attributeId), score) },
        )

    private fun updatedFoodItem(): RatedItem = foodItem(scores = listOf(TASTE to 9, SERVICE to 7))

    private fun RatedItem.toScorePairs(): List<String> =
        listOf(id) +
            scores
                .map { scoreEntry ->
                    "${scoreEntry.attribute.id}:${scoreEntry.score}:${scoreEntry.attribute.weight}"
                }.sorted()

    private companion object {
        const val FOOD_CATEGORY = "Food"
        const val TASTE = "taste"
        const val SERVICE = "service"
        const val AMBIENCE = "ambience"
        const val ITEM_ID = "item-1"
    }
}
