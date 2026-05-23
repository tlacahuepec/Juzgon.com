package com.juzgon.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.juzgon.data.local.JuzgonDatabase
import com.juzgon.domain.AppClock
import com.juzgon.domain.Attribute
import com.juzgon.domain.AttributeType
import com.juzgon.domain.Category
import com.juzgon.domain.DateScoreCalculator
import com.juzgon.domain.ItemAttributeValue
import com.juzgon.domain.RankedRatedItem
import com.juzgon.domain.RatedItem
import com.juzgon.domain.ScoreEntry
import com.juzgon.domain.ScoringDirection
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
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RoomRatingRepositoryRankingTest {
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
    fun observeRankedItems_emitsDeterministicOrderAndUpdatesAfterScoreChange() =
        runTest {
            categoryRepository.saveCategory(foodCategory())

            ratedItemRepository.observeRankedItems(FOOD_CATEGORY).test {
                assertEquals(emptyList<RankedRatedItem>(), awaitItem())

                ratedItemRepository.saveRatedItem(foodItem(id = "item-b", score = 8))
                assertRankingEquals(listOf("item-b:8.0"), awaitItem())

                ratedItemRepository.saveRatedItem(foodItem(id = "item-a", score = 8))
                assertRankingEquals(listOf("item-a:8.0", "item-b:8.0"), awaitItem())

                ratedItemRepository.saveRatedItem(foodItem(id = "item-b", score = 9))
                assertRankingEquals(listOf("item-b:9.0", "item-a:8.0"), awaitItem())
            }
        }

    @Test
    fun observeRankedItems_includesImageValuesForCategoryItems() =
        runTest {
            val photoAttribute = Attribute(PHOTO, type = AttributeType.IMAGE, isRequired = false)
            categoryRepository.saveCategory(
                Category(
                    name = FOOD_CATEGORY,
                    attributes = listOf(Attribute(TASTE), Attribute(SERVICE), photoAttribute),
                ),
            )
            ratedItemRepository.saveRatedItem(
                foodItem(id = "item-a", score = 8).copy(
                    values = listOf(ItemAttributeValue(photoAttribute, "content://images/item-a")),
                ),
            )

            val rankedItems = ratedItemRepository.observeRankedItems(FOOD_CATEGORY).first { it.isNotEmpty() }

            assertEquals(
                listOf(ItemAttributeValue(photoAttribute, "content://images/item-a")),
                rankedItems.single().item.values,
            )
        }

    private fun assertRankingEquals(
        expected: List<String>,
        actual: List<RankedRatedItem>,
    ) {
        assertEquals(expected, actual.map { "${it.item.id}:${it.aggregateScore}" })
    }

    private fun foodCategory(): Category =
        Category(
            name = FOOD_CATEGORY,
            attributes = listOf(Attribute(TASTE), Attribute(SERVICE)),
        )

    private fun foodItem(
        id: String,
        score: Int,
    ): RatedItem =
        RatedItem(
            id = id,
            scores =
                listOf(
                    ScoreEntry(attribute = Attribute(id = TASTE), score = score),
                    ScoreEntry(attribute = Attribute(id = SERVICE), score = score),
                ),
        )

    @Test
    fun observeRankedItems_includesDateScoresInAggregate() =
        runTest {
            val fixedToday = LocalDate.of(2026, 5, 23)
            val clock = AppClock { fixedToday }
            val dateCalculator = DateScoreCalculator(clock)
            val repoWithClock = RoomRatedItemRepository(database, dateCalculator)

            val releasedAttr =
                Attribute(
                    id = "released",
                    type = AttributeType.DATE,
                    scoringDirection = ScoringDirection.NEWER_IS_BETTER,
                )
            categoryRepository.saveCategory(
                Category(
                    name = FOOD_CATEGORY,
                    attributes = listOf(Attribute(TASTE), releasedAttr),
                ),
            )

            val newItem =
                RatedItem(
                    id = "item-new",
                    scores = listOf(ScoreEntry(attribute = Attribute(id = TASTE), score = 8)),
                    values = listOf(ItemAttributeValue(releasedAttr, "2026-05-23")),
                )
            val oldItem =
                RatedItem(
                    id = "item-old",
                    scores = listOf(ScoreEntry(attribute = Attribute(id = TASTE), score = 8)),
                    values = listOf(ItemAttributeValue(releasedAttr, "2006-05-23")),
                )
            ratedItemRepository.saveRatedItem(newItem)
            ratedItemRepository.saveRatedItem(oldItem)

            val rankedItems = repoWithClock.observeRankedItems(FOOD_CATEGORY).first { it.size == 2 }

            assertEquals("item-new", rankedItems[0].item.id)
            assertEquals("item-old", rankedItems[1].item.id)
            assert(rankedItems[0].aggregateScore > rankedItems[1].aggregateScore)
        }

    private companion object {
        const val FOOD_CATEGORY = "Food"
        const val TASTE = "taste"
        const val SERVICE = "service"
        const val PHOTO = "photo"
    }
}
