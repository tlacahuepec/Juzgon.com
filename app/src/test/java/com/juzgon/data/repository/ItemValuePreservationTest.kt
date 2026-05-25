package com.juzgon.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
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
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ItemValuePreservationTest {
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
    fun upsertOverwritesValueForSameItemAndAttribute() =
        runTest {
            categoryRepository.saveCategory(category())
            ratedItemRepository.saveRatedItem(
                RatedItem(
                    id = ITEM_ID,
                    scores = listOf(ScoreEntry(score, 8)),
                    values = listOf(ItemAttributeValue(notes, "first")),
                ),
            )

            currentTime = 2_000L
            ratedItemRepository.saveRatedItem(
                RatedItem(
                    id = ITEM_ID,
                    scores = listOf(ScoreEntry(score, 8)),
                    values = listOf(ItemAttributeValue(notes, "second")),
                ),
            )

            val item = ratedItemRepository.observeRatedItem(ITEM_ID).first()
            assertEquals("second", item?.values?.single()?.value)
        }

    @Test
    fun softDeletedValueIsNotShownAsActive() =
        runTest {
            categoryRepository.saveCategory(category())
            val otherNotes = Attribute("Other", type = AttributeType.NOTES, isRequired = false)
            categoryRepository.saveCategory(
                Category(CATEGORY_NAME, attributes = listOf(score, notes, otherNotes)),
            )
            ratedItemRepository.saveRatedItem(
                RatedItem(
                    id = ITEM_ID,
                    scores = listOf(ScoreEntry(score, 7)),
                    values =
                        listOf(
                            ItemAttributeValue(notes, "visible"),
                            ItemAttributeValue(otherNotes, "will-be-soft-deleted"),
                        ),
                ),
            )

            currentTime = 2_000L
            ratedItemRepository.saveRatedItem(
                RatedItem(
                    id = ITEM_ID,
                    scores = listOf(ScoreEntry(score, 7)),
                    values = listOf(ItemAttributeValue(notes, "visible")),
                ),
            )

            val item = ratedItemRepository.observeRatedItem(ITEM_ID).first()
            assertNotNull(item)
            assertEquals(1, item?.values?.size)
            assertEquals("visible", item?.values?.single()?.value)
        }

    @Test
    fun savingWithoutValuesPreservesExistingActiveRow() =
        runTest {
            categoryRepository.saveCategory(category())
            ratedItemRepository.saveRatedItem(
                RatedItem(
                    id = ITEM_ID,
                    scores = listOf(ScoreEntry(score, 9)),
                    values = listOf(ItemAttributeValue(notes, "kept")),
                ),
            )

            currentTime = 2_000L
            ratedItemRepository.saveRatedItem(
                RatedItem(
                    id = ITEM_ID,
                    scores = listOf(ScoreEntry(score, 9)),
                    values = emptyList(),
                ),
            )

            val item = ratedItemRepository.observeRatedItem(ITEM_ID).first()
            assertEquals("kept", item?.values?.single()?.value)
        }

    @Test
    fun overwritingSoftDeletedValueRestoresNewContent() =
        runTest {
            val otherNotes = Attribute("Other", type = AttributeType.NOTES, isRequired = false)
            categoryRepository.saveCategory(
                Category(CATEGORY_NAME, attributes = listOf(score, notes, otherNotes)),
            )
            ratedItemRepository.saveRatedItem(
                RatedItem(
                    id = ITEM_ID,
                    scores = listOf(ScoreEntry(score, 9)),
                    values =
                        listOf(
                            ItemAttributeValue(notes, "original"),
                            ItemAttributeValue(otherNotes, "will-go"),
                        ),
                ),
            )

            currentTime = 2_000L
            ratedItemRepository.saveRatedItem(
                RatedItem(
                    id = ITEM_ID,
                    scores = listOf(ScoreEntry(score, 9)),
                    values = listOf(ItemAttributeValue(notes, "original")),
                ),
            )

            currentTime = 3_000L
            ratedItemRepository.saveRatedItem(
                RatedItem(
                    id = ITEM_ID,
                    scores = listOf(ScoreEntry(score, 9)),
                    values =
                        listOf(
                            ItemAttributeValue(notes, "original"),
                            ItemAttributeValue(otherNotes, "restored"),
                        ),
                ),
            )

            val item = ratedItemRepository.observeRatedItem(ITEM_ID).first()
            assertEquals(2, item?.values?.size)
            val restoredValue = item?.values?.first { it.attribute.id == "Other" }
            assertEquals("restored", restoredValue?.value)
        }

    @Test
    fun valueOperationsDoNotAffectScoreData() =
        runTest {
            categoryRepository.saveCategory(category())
            ratedItemRepository.saveRatedItem(
                RatedItem(
                    id = ITEM_ID,
                    scores = listOf(ScoreEntry(score, 8)),
                    values = listOf(ItemAttributeValue(notes, "initial")),
                ),
            )

            currentTime = 2_000L
            ratedItemRepository.saveRatedItem(
                RatedItem(
                    id = ITEM_ID,
                    scores = listOf(ScoreEntry(score, 8)),
                    values = listOf(ItemAttributeValue(notes, "changed")),
                ),
            )

            val item = ratedItemRepository.observeRatedItem(ITEM_ID).first()
            assertEquals(8, item?.scores?.single()?.score)
            assertEquals("changed", item?.values?.single()?.value)
        }

    @Test
    fun itemWithOnlyValuesAndNoRatingsIsPreserved() =
        runTest {
            categoryRepository.saveCategory(category())
            ratedItemRepository.saveRatedItem(
                RatedItem(
                    id = ITEM_ID,
                    scores = emptyList(),
                    values = listOf(ItemAttributeValue(notes, "value-only")),
                ),
            )

            val item = ratedItemRepository.observeRatedItem(ITEM_ID).first()
            assertNotNull(item)
            assertEquals(emptyList<ScoreEntry>(), item?.scores)
            assertEquals("value-only", item?.values?.single()?.value)
        }

    @Test
    fun softDeletedValueIsNotReturnedInObserveRatedItems() =
        runTest {
            val otherNotes = Attribute("Other", type = AttributeType.NOTES, isRequired = false)
            categoryRepository.saveCategory(
                Category(CATEGORY_NAME, attributes = listOf(score, notes, otherNotes)),
            )
            ratedItemRepository.saveRatedItem(
                RatedItem(
                    id = ITEM_ID,
                    scores = listOf(ScoreEntry(score, 5)),
                    values =
                        listOf(
                            ItemAttributeValue(notes, "keep"),
                            ItemAttributeValue(otherNotes, "remove"),
                        ),
                ),
            )

            currentTime = 2_000L
            ratedItemRepository.saveRatedItem(
                RatedItem(
                    id = ITEM_ID,
                    scores = listOf(ScoreEntry(score, 5)),
                    values = listOf(ItemAttributeValue(notes, "keep")),
                ),
            )

            val items = ratedItemRepository.observeRatedItems().first()
            assertEquals(1, items.size)
            assertEquals(1, items.single().values.size)
            assertEquals(
                "keep",
                items
                    .single()
                    .values
                    .single()
                    .value,
            )
        }

    private fun category(): Category = Category(CATEGORY_NAME, attributes = listOf(score, notes))

    private companion object {
        const val CATEGORY_NAME = "Food"
        const val ITEM_ID = "item-1"
        val score = Attribute("Score")
        val notes = Attribute("Notes", type = AttributeType.NOTES, isRequired = false)
    }
}
