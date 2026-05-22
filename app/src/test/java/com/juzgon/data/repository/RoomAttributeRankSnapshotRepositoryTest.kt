package com.juzgon.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.juzgon.data.local.JuzgonDatabase
import com.juzgon.domain.Attribute
import com.juzgon.domain.AttributeRankSnapshot
import com.juzgon.domain.Category
import com.juzgon.domain.RatedItem
import com.juzgon.domain.ScoreEntry
import com.juzgon.domain.repository.AttributeRankSnapshotRepository
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
class RoomAttributeRankSnapshotRepositoryTest {
    private lateinit var database: JuzgonDatabase
    private lateinit var categoryRepository: CategoryRepository
    private lateinit var ratedItemRepository: RatedItemRepository
    private lateinit var snapshotRepository: AttributeRankSnapshotRepository
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
        snapshotRepository = RoomAttributeRankSnapshotRepository(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun observeSnapshotsForItemReturnsEmptyListWhenHistoryIsMissing() =
        runTest {
            assertEquals(
                emptyList<AttributeRankSnapshot>(),
                snapshotRepository.observeSnapshotsForItem(ITEM_ID).first(),
            )
        }

    @Test
    fun saveRatedItemPersistsRankSnapshotsForNumericScores() =
        runTest {
            categoryRepository.saveCategory(foodCategory())

            currentTime = FIRST_CAPTURED_AT
            ratedItemRepository.saveRatedItem(foodItem(taste = 8, service = 6))

            assertSnapshotRows(
                expected = listOf("item-1:1500:taste:8:1", "item-1:1500:service:6:2"),
                actual = snapshotRepository.observeSnapshotsForItem(ITEM_ID).first(),
            )
        }

    @Test
    fun saveRatedItemPersistsNewSnapshotWhenNumericScoresChange() =
        runTest {
            categoryRepository.saveCategory(foodCategory())

            currentTime = FIRST_CAPTURED_AT
            ratedItemRepository.saveRatedItem(foodItem(taste = 8, service = 6))
            currentTime = SECOND_CAPTURED_AT
            ratedItemRepository.saveRatedItem(foodItem(taste = 7, service = 9))

            assertSnapshotRows(
                expected =
                    listOf(
                        "item-1:2500:service:9:1",
                        "item-1:2500:taste:7:2",
                        "item-1:1500:taste:8:1",
                        "item-1:1500:service:6:2",
                    ),
                actual = snapshotRepository.observeSnapshotsForItem(ITEM_ID).first(),
            )
        }

    @Test
    fun saveRatedItemSkipsDuplicateSnapshotWhenNumericScoresAreUnchanged() =
        runTest {
            categoryRepository.saveCategory(foodCategory())

            currentTime = FIRST_CAPTURED_AT
            ratedItemRepository.saveRatedItem(foodItem(taste = 8, service = 6))
            currentTime = SECOND_CAPTURED_AT
            ratedItemRepository.saveRatedItem(foodItem(taste = 8, service = 6, notes = "updated notes"))

            assertSnapshotRows(
                expected = listOf("item-1:1500:taste:8:1", "item-1:1500:service:6:2"),
                actual = snapshotRepository.observeSnapshotsForItem(ITEM_ID).first(),
            )
        }

    private fun assertSnapshotRows(
        expected: List<String>,
        actual: List<AttributeRankSnapshot>,
    ) {
        assertEquals(
            expected,
            actual.map { snapshot ->
                "${snapshot.itemId}:${snapshot.capturedAt}:${snapshot.attributeId}:${snapshot.value}:${snapshot.rank}"
            },
        )
    }

    private fun foodCategory(): Category =
        Category(
            name = FOOD_CATEGORY,
            attributes = listOf(Attribute(TASTE), Attribute(SERVICE)),
        )

    private fun foodItem(
        taste: Int,
        service: Int,
        notes: String = "",
    ): RatedItem =
        RatedItem(
            id = ITEM_ID,
            notes = notes,
            scores =
                listOf(
                    ScoreEntry(attribute = Attribute(TASTE), score = taste),
                    ScoreEntry(attribute = Attribute(SERVICE), score = service),
                ),
        )

    private companion object {
        const val FOOD_CATEGORY = "Food"
        const val TASTE = "taste"
        const val SERVICE = "service"
        const val ITEM_ID = "item-1"
        const val FIRST_CAPTURED_AT = 1_500L
        const val SECOND_CAPTURED_AT = 2_500L
    }
}
