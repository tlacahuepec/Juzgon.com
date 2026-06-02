package com.juzgon.feature.item

import com.juzgon.domain.AppClock
import com.juzgon.domain.Attribute
import com.juzgon.domain.AttributeRankSnapshot
import com.juzgon.domain.AttributeType
import com.juzgon.domain.BirthDateAgeCalculator
import com.juzgon.domain.Category
import com.juzgon.domain.DateScoreCalculator
import com.juzgon.domain.ItemAttributeValue
import com.juzgon.domain.RankedRatedItem
import com.juzgon.domain.RatedItem
import com.juzgon.domain.ScoreEntry
import com.juzgon.domain.ScoreProfile
import com.juzgon.domain.ScoringDirection
import com.juzgon.domain.repository.AttributeRankSnapshotRepository
import com.juzgon.domain.repository.CategoryRepository
import com.juzgon.domain.repository.RatedItemRepository
import com.juzgon.domain.repository.ScoreProfileRepository
import com.juzgon.domain.usecase.CalculateProfileRankedItemsUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class ItemDetailContentLoaderTest {
    private val fixedToday = LocalDate.of(2026, 6, 1)
    private val clock = AppClock { fixedToday }
    private val dateProcessor = ItemDetailDateProcessor(BirthDateAgeCalculator(clock), DateScoreCalculator(clock))

    private lateinit var repository: FakeRatedItemRepository
    private lateinit var snapshotRepository: FakeSnapshotRepository
    private lateinit var categoryRepository: FakeCategoryRepository
    private lateinit var scoreProfileRepository: FakeScoreProfileRepository
    private lateinit var loader: ItemDetailContentLoader

    @Before
    fun setUp() {
        repository = FakeRatedItemRepository()
        snapshotRepository = FakeSnapshotRepository()
        categoryRepository = FakeCategoryRepository()
        scoreProfileRepository = FakeScoreProfileRepository()
        loader =
            ItemDetailContentLoader(
                ratedItemRepository = repository,
                attributeRankSnapshotRepository = snapshotRepository,
                categoryRepository = categoryRepository,
                scoreProfileRepository = scoreProfileRepository,
                calculateProfileRankedItems = CalculateProfileRankedItemsUseCase(),
                dateProcessor = dateProcessor,
            )
    }

    @Test
    fun `birth date attribute shows computed age`() =
        runTest {
            val attr = Attribute("People/Birth Date", type = AttributeType.DATE)
            repository.item.value =
                RatedItem(
                    id = "person-1",
                    scores = listOf(ScoreEntry(Attribute("People/Speed"), 8)),
                    values = listOf(ItemAttributeValue(attr, "1990-05-27")),
                )

            val state = loader.loadContent("person-1", "", null)

            assertEquals("Age: 36", state.attributeValues.single().ageText)
        }

    @Test
    fun `birthdate without space also shows age`() =
        runTest {
            val attr = Attribute("People/Birthdate", type = AttributeType.DATE)
            repository.item.value =
                RatedItem(
                    id = "person-1",
                    scores = listOf(ScoreEntry(Attribute("People/Speed"), 8)),
                    values = listOf(ItemAttributeValue(attr, "1990-05-27")),
                )

            val state = loader.loadContent("person-1", "", null)

            assertEquals("Age: 36", state.attributeValues.single().ageText)
        }

    @Test
    fun `non birth date DATE attribute does not show age`() =
        runTest {
            val attr = Attribute("Cars/Release", type = AttributeType.DATE)
            repository.item.value =
                RatedItem(
                    id = "car-1",
                    scores = listOf(ScoreEntry(Attribute("Cars/Speed"), 8)),
                    values = listOf(ItemAttributeValue(attr, "2020-01-15")),
                )

            val state = loader.loadContent("car-1", "", null)

            assertNull(state.attributeValues.single().ageText)
        }

    @Test
    fun `invalid birth date value does not show age`() =
        runTest {
            val attr = Attribute("People/Birth Date", type = AttributeType.DATE)
            repository.item.value =
                RatedItem(
                    id = "person-1",
                    scores = listOf(ScoreEntry(Attribute("People/Speed"), 8)),
                    values = listOf(ItemAttributeValue(attr, "not-a-date")),
                )

            val state = loader.loadContent("person-1", "", null)

            assertNull(state.attributeValues.single().ageText)
        }

    @Test
    fun `rankable DATE attribute contributes derived score`() =
        runTest {
            val dateAttr =
                Attribute(
                    "People/Birth Date",
                    type = AttributeType.DATE,
                    scoringDirection = ScoringDirection.OLDER_IS_BETTER,
                )
            repository.item.value =
                RatedItem(
                    id = "person-1",
                    scores = listOf(ScoreEntry(Attribute("People/Looks"), 8)),
                    values = listOf(ItemAttributeValue(dateAttr, "1990-05-27")),
                )

            val state = loader.loadContent("person-1", "", null)

            val dateScore = state.attributeScores.find { it.label == "Birth Date" }
            assertEquals(8, dateScore?.score)
        }

    @Test
    fun `date score appears in ranked attribute cards`() =
        runTest {
            val dateAttr =
                Attribute(
                    "People/Birth Date",
                    type = AttributeType.DATE,
                    scoringDirection = ScoringDirection.OLDER_IS_BETTER,
                )
            repository.item.value =
                RatedItem(
                    id = "person-1",
                    scores = listOf(ScoreEntry(Attribute("People/Looks"), 5)),
                    values = listOf(ItemAttributeValue(dateAttr, "1990-05-27")),
                )

            val state = loader.loadContent("person-1", "", null)

            val cardLabels = state.rankedAttributes.map { it.label }
            assert("Birth Date" in cardLabels)
        }

    @Test
    fun `date score contributes to overall score`() =
        runTest {
            val dateAttr =
                Attribute(
                    "People/Birth Date",
                    type = AttributeType.DATE,
                    scoringDirection = ScoringDirection.OLDER_IS_BETTER,
                    weight = 1.0,
                )
            repository.item.value =
                RatedItem(
                    id = "person-1",
                    scores = listOf(ScoreEntry(Attribute("People/Looks", weight = 1.0), 8)),
                    values = listOf(ItemAttributeValue(dateAttr, "1990-05-27")),
                )

            val state = loader.loadContent("person-1", "", null)

            // Looks=8, Birth Date(36 years, OLDER_IS_BETTER)= 1 + 36/5 = 8, avg = 8.0
            assertEquals("8.0", state.overallScoreText)
        }

    @Test
    fun `DATE attribute without scoring direction does not add score`() =
        runTest {
            val dateAttr = Attribute("People/Birth Date", type = AttributeType.DATE)
            repository.item.value =
                RatedItem(
                    id = "person-1",
                    scores = listOf(ScoreEntry(Attribute("People/Looks"), 8)),
                    values = listOf(ItemAttributeValue(dateAttr, "1990-05-27")),
                )

            val state = loader.loadContent("person-1", "", null)

            assertEquals(1, state.attributeScores.size)
            assertEquals("Looks", state.attributeScores.single().label)
        }

    @Test
    fun `invalid date with scoring direction does not add score or crash`() =
        runTest {
            val dateAttr =
                Attribute(
                    "People/Birth Date",
                    type = AttributeType.DATE,
                    scoringDirection = ScoringDirection.NEWER_IS_BETTER,
                )
            repository.item.value =
                RatedItem(
                    id = "person-1",
                    scores = listOf(ScoreEntry(Attribute("People/Looks"), 8)),
                    values = listOf(ItemAttributeValue(dateAttr, "bad-date")),
                )

            val state = loader.loadContent("person-1", "", null)

            assertEquals(1, state.attributeScores.size)
            assertEquals("Looks", state.attributeScores.single().label)
        }

    @Test
    fun `existing number scores remain unchanged with date scores`() =
        runTest {
            val dateAttr =
                Attribute(
                    "People/Birth Date",
                    type = AttributeType.DATE,
                    scoringDirection = ScoringDirection.NEWER_IS_BETTER,
                )
            repository.item.value =
                RatedItem(
                    id = "person-1",
                    scores =
                        listOf(
                            ScoreEntry(Attribute("People/Looks"), 9),
                            ScoreEntry(Attribute("People/Style"), 7),
                        ),
                    values = listOf(ItemAttributeValue(dateAttr, "2021-06-01")),
                )

            val state = loader.loadContent("person-1", "", null)

            val numberScores = state.attributeScores.filter { it.label != "Birth Date" }
            assertEquals(2, numberScores.size)
            assertEquals(9, numberScores.first { it.label == "Looks" }.score)
            assertEquals(7, numberScores.first { it.label == "Style" }.score)
        }

    @Test
    fun `date score with displayInDiamond appears in diamond chart points`() =
        runTest {
            val dateAttr =
                Attribute(
                    "People/Birth Date",
                    type = AttributeType.DATE,
                    scoringDirection = ScoringDirection.OLDER_IS_BETTER,
                    displayInDiamond = true,
                    diamondOrder = 2,
                )
            repository.item.value =
                RatedItem(
                    id = "person-1",
                    scores = listOf(ScoreEntry(Attribute("People/Looks", displayInDiamond = true, diamondOrder = 1), 8)),
                    values = listOf(ItemAttributeValue(dateAttr, "1990-05-27")),
                )

            val state = loader.loadContent("person-1", "", null)

            val diamondLabels = state.diamondChartPoints.map { it.label }
            assert("Looks" in diamondLabels)
            assert("Birth Date" in diamondLabels)
        }

    private class FakeRatedItemRepository : RatedItemRepository {
        val item = MutableStateFlow<RatedItem?>(null)

        override fun observeRatedItems(): Flow<List<RatedItem>> = error("not used")

        override fun observeRatedItem(id: String): Flow<RatedItem?> = item

        override fun observeRankedItems(categoryName: String): Flow<List<RankedRatedItem>> = flowOf(emptyList())

        override suspend fun saveRatedItem(ratedItem: RatedItem) = error("not used")

        override suspend fun renameRatedItem(
            originalId: String,
            ratedItem: RatedItem,
        ) = error("not used")

        override suspend fun deleteRatedItem(id: String) = error("not used")
    }

    private class FakeSnapshotRepository : AttributeRankSnapshotRepository {
        override fun observeSnapshotsForItem(itemId: String): Flow<List<AttributeRankSnapshot>> = flowOf(emptyList())
    }

    private class FakeCategoryRepository : CategoryRepository {
        override fun observeCategories(): Flow<List<Category>> = error("not used")

        override fun observeCategory(name: String): Flow<Category?> = flowOf(null)

        override suspend fun saveCategory(category: Category) = error("not used")

        override suspend fun renameCategory(
            originalName: String,
            category: Category,
            renamedAttributeIds: Map<String, String>,
        ) = error("not used")

        override suspend fun deleteCategory(name: String) = error("not used")
    }

    private class FakeScoreProfileRepository : ScoreProfileRepository {
        override fun observeProfilesForCategory(categoryName: String): Flow<List<ScoreProfile>> = error("not used")

        override fun observeProfile(id: String): Flow<ScoreProfile?> = flowOf(null)

        override suspend fun saveProfile(profile: ScoreProfile) = error("not used")

        override suspend fun deleteProfile(id: String) = error("not used")
    }
}
