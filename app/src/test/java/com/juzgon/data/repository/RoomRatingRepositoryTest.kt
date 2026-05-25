package com.juzgon.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.juzgon.data.local.JuzgonDatabase
import com.juzgon.domain.Attribute
import com.juzgon.domain.AttributeType
import com.juzgon.domain.Category
import com.juzgon.domain.ItemAttributeValue
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
    fun renameCategory_attributeRenamePreservesRatingsValuesAndSnapshots() =
        runTest {
            val speed = Attribute("Speed")
            val notes = Attribute("Notes", type = AttributeType.NOTES, isRequired = false)
            categoryRepository.saveCategory(Category("Cars", attributes = listOf(speed, notes)))
            ratedItemRepository.saveRatedItem(
                RatedItem(
                    id = "Jetta",
                    scores = listOf(ScoreEntry(speed, 8)),
                    values = listOf(ItemAttributeValue(notes, "Great handling")),
                ),
            )

            categoryRepository.renameCategory(
                originalName = "Cars",
                category =
                    Category(
                        name = "Cars",
                        attributes =
                            listOf(
                                Attribute("Pace"),
                                Attribute("Review", type = AttributeType.NOTES, isRequired = false),
                            ),
                    ),
                renamedAttributeIds =
                    mapOf(
                        "Speed" to "Pace",
                        "Notes" to "Review",
                    ),
            )

            val renamedItem = ratedItemRepository.observeRatedItem("Jetta").first()
            assertEquals(listOf("Pace:8:1.0"), renamedItem?.toScorePairs())
            assertEquals(listOf("Review:Great handling"), renamedItem?.toValuePairs())
            assertEquals(listOf("Jetta"), ratedItemRepository.observeRankedItems("Cars").first().map { it.item.id })

            val snapshots = RoomAttributeRankSnapshotRepository(database).observeSnapshotsForItem("Jetta").first()
            assertEquals(listOf("Pace"), snapshots.map { it.attributeId })
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
    fun renameRatedItemMovesItemAndPreservesRatingsValuesAndSnapshots() =
        runTest {
            val speed = Attribute("Speed")
            val photo = Attribute("Photo", type = AttributeType.IMAGE, isRequired = false)
            categoryRepository.saveCategory(Category("Cars", attributes = listOf(speed, photo)))
            currentTime = 1_500L
            ratedItemRepository.saveRatedItem(
                RatedItem(
                    id = "Jetta",
                    notes = "daily driver",
                    scores = listOf(ScoreEntry(speed, 8)),
                    values = listOf(ItemAttributeValue(photo, "content://images/jetta")),
                ),
            )

            currentTime = 2_500L
            ratedItemRepository.renameRatedItem(
                originalId = "Jetta",
                ratedItem =
                    RatedItem(
                        id = "GLI",
                        notes = "daily driver",
                        scores = listOf(ScoreEntry(speed, 8)),
                        values = listOf(ItemAttributeValue(photo, "content://images/jetta")),
                    ),
            )

            assertEquals(null, ratedItemRepository.observeRatedItem("Jetta").first())
            val renamedItem = ratedItemRepository.observeRatedItem("GLI").first()
            assertEquals("GLI", renamedItem?.id)
            assertEquals("daily driver", renamedItem?.notes)
            assertEquals(listOf("Speed:8:1.0"), renamedItem?.toScorePairs())
            assertEquals(listOf("Photo:content://images/jetta"), renamedItem?.toValuePairs())
            assertEquals(listOf("GLI"), ratedItemRepository.observeRankedItems("Cars").first().map { it.item.id })

            val snapshotRepository = RoomAttributeRankSnapshotRepository(database)
            assertEquals(emptyList<Any>(), snapshotRepository.observeSnapshotsForItem("Jetta").first())
            val renamedSnapshots = snapshotRepository.observeSnapshotsForItem("GLI").first()
            assertEquals(listOf("Speed"), renamedSnapshots.map { it.attributeId })
            assertEquals(listOf("GLI"), renamedSnapshots.map { it.itemId })
        }

    @Test
    fun renameRatedItemToDuplicateNameRollsBackWithoutDataChanges() =
        runTest {
            val speed = Attribute("Speed")
            categoryRepository.saveCategory(Category("Cars", attributes = listOf(speed)))
            val jetta = RatedItem(id = "Jetta", scores = listOf(ScoreEntry(speed, 7)), notes = "original")
            val passat = RatedItem(id = "Passat", scores = listOf(ScoreEntry(speed, 9)), notes = "existing")
            ratedItemRepository.saveRatedItem(jetta)
            ratedItemRepository.saveRatedItem(passat)

            val result =
                runCatching {
                    ratedItemRepository.renameRatedItem(
                        originalId = "Jetta",
                        ratedItem = RatedItem(id = "Passat", scores = listOf(ScoreEntry(speed, 8)), notes = "renamed"),
                    )
                }

            assertEquals(true, result.isFailure)
            assertEquals("Item already exists", result.exceptionOrNull()?.message)
            assertRatedItemEquals(jetta, ratedItemRepository.observeRatedItem("Jetta").first())
            assertRatedItemEquals(passat, ratedItemRepository.observeRatedItem("Passat").first())
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

    private fun RatedItem.toValuePairs(): List<String> =
        values
            .map { value -> "${value.attribute.id}:${value.value}" }
            .sorted()

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

    @Test
    fun saveRatedItemWithoutValuePreservesExistingValueViaSoftDelete() =
        runTest {
            val score = Attribute("Score")
            val nationality = Attribute("Nationality", type = AttributeType.NATIONALITY, isRequired = false)
            categoryRepository.saveCategory(Category("People", attributes = listOf(score, nationality)))

            ratedItemRepository.saveRatedItem(
                RatedItem(
                    id = "Alice",
                    scores = listOf(ScoreEntry(score, 9)),
                    values = listOf(ItemAttributeValue(nationality, "US")),
                ),
            )

            val savedItem = ratedItemRepository.observeRatedItem("Alice").first()
            assertEquals("US", savedItem?.values?.first()?.value)

            ratedItemRepository.saveRatedItem(
                RatedItem(
                    id = "Alice",
                    scores = listOf(ScoreEntry(score, 10)),
                    values = emptyList(),
                ),
            )

            val updatedItem = ratedItemRepository.observeRatedItem("Alice").first()
            assertEquals(10, updatedItem?.scores?.first()?.score)
            assertEquals("US", updatedItem?.values?.first()?.value)
        }

    @Test
    fun observeRatedItemsReturnsItemsWithOnlyValues() =
        runTest {
            val nationality = Attribute("Nationality", type = AttributeType.NATIONALITY, isRequired = false)
            categoryRepository.saveCategory(Category("People", attributes = listOf(nationality)))

            ratedItemRepository.saveRatedItem(
                RatedItem(
                    id = "Bob",
                    scores = emptyList(),
                    values = listOf(ItemAttributeValue(nationality, "UK")),
                ),
            )

            val items = ratedItemRepository.observeRatedItems().first()
            assertEquals(1, items.size)
            assertEquals("Bob", items.first().id)
            assertEquals(
                "UK",
                items
                    .first()
                    .values
                    .first()
                    .value,
            )
        }

    @Test
    fun deleteCategory_purgesOrphanedRatingsFromRankedResults() =
        runTest {
            val speed = Attribute("Speed")
            val handling = Attribute("Handling")
            categoryRepository.saveCategory(Category("Cars", attributes = listOf(speed, handling)))
            ratedItemRepository.saveRatedItem(
                RatedItem(id = "Jetta", scores = listOf(ScoreEntry(speed, 8), ScoreEntry(handling, 7))),
            )

            categoryRepository.deleteCategory("Cars")

            val rawRatings = database.itemDao().getRatingsForItem("Jetta")
            assertEquals(emptyList<Any>(), rawRatings)
        }

    @Test
    fun saveCategory_removingAttributePurgesStaleRatingsFromResults() =
        runTest {
            val speed = Attribute("Speed")
            val handling = Attribute("Handling")
            categoryRepository.saveCategory(Category("Cars", attributes = listOf(speed, handling)))
            ratedItemRepository.saveRatedItem(
                RatedItem(id = "Jetta", scores = listOf(ScoreEntry(speed, 8), ScoreEntry(handling, 7))),
            )

            categoryRepository.renameCategory(
                originalName = "Cars",
                category = Category("Cars", attributes = listOf(Attribute("Speed"))),
                renamedAttributeIds = emptyMap(),
            )

            val rawRatings = database.itemDao().getRatingsForItem("Jetta")
            assertEquals(listOf("Speed"), rawRatings.map { it.attributeId })
        }

    @Test
    fun deleteCategory_preservesActiveItemValuesForValueOnlyItems() =
        runTest {
            val nationality = Attribute("Nationality", type = AttributeType.NATIONALITY, isRequired = false)
            categoryRepository.saveCategory(Category("People", attributes = listOf(nationality)))
            ratedItemRepository.saveRatedItem(
                RatedItem(id = "Alice", scores = emptyList(), values = listOf(ItemAttributeValue(nationality, "US"))),
            )

            categoryRepository.deleteCategory("People")

            val item = ratedItemRepository.observeRatedItem("Alice").first()
            assertEquals("Alice", item?.id)
            val rawItem = database.itemDao().getItemWithRatings("Alice")
            assertEquals(1, rawItem?.values?.filter { it.deletedAt == null }?.size)
            assertEquals("US", rawItem?.values?.first()?.valueText)
        }

    private companion object {
        const val CATEGORY_NAME = "Food"
        const val RENAMED_CATEGORY_NAME = "Dining"
        const val ITEM_ID = "item-1"
    }
}
