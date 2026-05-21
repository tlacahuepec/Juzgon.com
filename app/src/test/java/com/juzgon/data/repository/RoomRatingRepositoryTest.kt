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
class RoomRatingRepositoryTest {
    private lateinit var database: JuzgonDatabase
    private lateinit var categoryRepository: CategoryRepository
    private lateinit var ratedItemRepository: RatedItemRepository
    private var currentTime = 1_000L

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database =
            Room
                .inMemoryDatabaseBuilder(context, JuzgonDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        categoryRepository = RoomCategoryRepository(database)
        ratedItemRepository = RoomRatedItemRepository(database) { currentTime }
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun observeCategories_emitsAfterSaveAndUpdate() =
        runTest {
            categoryRepository.observeCategories().test {
                assertEquals(emptyList<Category>(), awaitItem())

                categoryRepository.saveCategory(foodCategory())
                assertCategoryListEquals(listOf(foodCategory()), awaitItem())

                categoryRepository.saveCategory(updatedFoodCategory())
                assertCategoryListEquals(listOf(updatedFoodCategory()), awaitItem())
            }
        }

    @Test
    fun observeCategory_emitsNullAfterDelete() =
        runTest {
            categoryRepository.observeCategory(CATEGORY_NAME).test {
                assertEquals(null, awaitItem())

                categoryRepository.saveCategory(foodCategory())
                assertCategoryEquals(foodCategory(), awaitItem())

                categoryRepository.deleteCategory(CATEGORY_NAME)
                assertEquals(null, awaitItem())
            }
        }

    @Test
    fun renameCategory_movesCategoryWithOrderedWeightedAttributes() =
        runTest {
            categoryRepository.saveCategory(foodCategory())

            val renamedCategory =
                Category(
                    name = RENAMED_CATEGORY_NAME,
                    attributes =
                        listOf(
                            Attribute("taste", weight = 2.0),
                            Attribute("service", weight = 1.5),
                        ),
                )
            categoryRepository.renameCategory(CATEGORY_NAME, renamedCategory)

            assertEquals(null, categoryRepository.observeCategory(CATEGORY_NAME).first())
            assertCategoryEquals(renamedCategory, categoryRepository.observeCategory(RENAMED_CATEGORY_NAME).first())
        }

    @Test
    fun observeRatedItems_emitsAfterSaveAndUpdate() =
        runTest {
            categoryRepository.saveCategory(foodCategory())

            ratedItemRepository.observeRatedItems().test {
                assertEquals(emptyList<RatedItem>(), awaitItem())

                ratedItemRepository.saveRatedItem(foodItem())
                assertRatedItemListEquals(listOf(foodItem()), awaitItem())

                ratedItemRepository.saveRatedItem(updatedFoodItem())
                assertRatedItemListEquals(listOf(updatedFoodItem()), awaitItem())
            }
        }

    @Test
    fun observeRatedItem_emitsNullAfterDelete() =
        runTest {
            categoryRepository.saveCategory(foodCategory())

            ratedItemRepository.observeRatedItem(ITEM_ID).test {
                assertEquals(null, awaitItem())

                ratedItemRepository.saveRatedItem(foodItem())
                assertRatedItemEquals(foodItem(), awaitItem())

                ratedItemRepository.deleteRatedItem(ITEM_ID)
                assertEquals(null, awaitItem())
            }
        }

    @Test
    fun saveRatedItemPersistsNotesAndRatingsTogether() =
        runTest {
            categoryRepository.saveCategory(foodCategory())
            val itemWithNotes =
                RatedItem(
                    id = ITEM_ID,
                    notes = "Order the chef special",
                    scores =
                        listOf(
                            ScoreEntry(attribute = Attribute(id = "taste", weight = 1.5), score = 10),
                            ScoreEntry(attribute = Attribute(id = "service"), score = 8),
                        ),
                )

            ratedItemRepository.saveRatedItem(itemWithNotes)

            assertRatedItemEquals(itemWithNotes, ratedItemRepository.observeRatedItem(ITEM_ID).first())
        }

    @Test
    fun saveRatedItemRollsBackItemWhenRatingInsertFails() =
        runTest {
            categoryRepository.saveCategory(foodCategory())
            val itemWithUnknownAttribute =
                RatedItem(
                    id = "orphan",
                    scores = listOf(ScoreEntry(attribute = Attribute("unknown"), score = 8)),
                )

            val result = runCatching { ratedItemRepository.saveRatedItem(itemWithUnknownAttribute) }

            assertEquals(true, result.isFailure)
            assertEquals(null, ratedItemRepository.observeRatedItem("orphan").first())
        }

    @Test
    fun saveRatedItemSetsCreateAndUpdateTimestamps() =
        runTest {
            categoryRepository.saveCategory(foodCategory())

            currentTime = 1_500L
            ratedItemRepository.saveRatedItem(foodItem())

            val item = ratedItemRepository.observeRatedItem(ITEM_ID).first()
            assertEquals(1_500L, item?.createdAt)
            assertEquals(1_500L, item?.updatedAt)
        }

    @Test
    fun updateRatedItemPreservesCreatedTimestampAndChangesUpdatedTimestamp() =
        runTest {
            categoryRepository.saveCategory(foodCategory())

            currentTime = 1_500L
            ratedItemRepository.saveRatedItem(foodItem())

            currentTime = 2_500L
            ratedItemRepository.saveRatedItem(updatedFoodItem())

            val item = ratedItemRepository.observeRatedItem(ITEM_ID).first()
            assertEquals(1_500L, item?.createdAt)
            assertEquals(2_500L, item?.updatedAt)
            assertRatedItemEquals(updatedFoodItem(), item)
        }

    private fun assertCategoryListEquals(
        expected: List<Category>,
        actual: List<Category>,
    ) {
        assertEquals(expected.size, actual.size)
        expected.zip(actual).forEach { (expectedCategory, actualCategory) ->
            assertCategoryEquals(expectedCategory, actualCategory)
        }
    }

    private fun assertCategoryEquals(
        expected: Category,
        actual: Category?,
    ) {
        assertEquals(expected.name, actual?.name)
        assertEquals(expected.attributes, actual?.attributes)
    }

    private fun assertRatedItemListEquals(
        expected: List<RatedItem>,
        actual: List<RatedItem>,
    ) {
        assertEquals(expected.size, actual.size)
        expected.zip(actual).forEach { (expectedItem, actualItem) ->
            assertRatedItemEquals(expectedItem, actualItem)
        }
    }

    private fun assertRatedItemEquals(
        expected: RatedItem,
        actual: RatedItem?,
    ) {
        assertEquals(expected.id, actual?.id)
        assertEquals(expected.notes, actual?.notes)
        assertEquals(expected.toScorePairs(), actual?.toScorePairs())
    }

    private fun RatedItem.toScorePairs(): List<String> =
        scores
            .map { scoreEntry ->
                "${scoreEntry.attribute.id}:${scoreEntry.score}:${scoreEntry.attribute.weight}"
            }.sorted()

    private fun foodCategory(): Category = category(foodAttributes())

    private fun updatedFoodCategory(): Category = category(updatedFoodAttributes())

    private fun foodAttributes(): List<Attribute> =
        listOf(
            Attribute("service"),
            Attribute("taste", weight = 1.5),
        )

    private fun updatedFoodAttributes(): List<Attribute> =
        listOf(
            Attribute("ambience"),
            Attribute("taste", weight = 2.0),
        )

    private fun category(attributes: List<Attribute>): Category = Category(CATEGORY_NAME, attributes)

    private fun foodItem(): RatedItem =
        RatedItem(
            id = ITEM_ID,
            scores =
                listOf(
                    ScoreEntry(attribute = Attribute(id = "taste", weight = 1.5), score = 8),
                    ScoreEntry(attribute = Attribute(id = "service"), score = 6),
                ),
        )

    private fun updatedFoodItem(): RatedItem =
        RatedItem(
            id = ITEM_ID,
            scores =
                listOf(
                    ScoreEntry(attribute = Attribute(id = "taste", weight = 1.5), score = 9),
                    ScoreEntry(attribute = Attribute(id = "service"), score = 7),
                ),
        )

    private companion object {
        const val CATEGORY_NAME = "Food"
        const val RENAMED_CATEGORY_NAME = "Dining"
        const val ITEM_ID = "item-1"
    }
}
