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
import com.juzgon.domain.repository.AttributeRankSnapshotRepository
import com.juzgon.domain.repository.CategoryRepository
import com.juzgon.domain.repository.RatedItemRepository
import com.juzgon.domain.repository.ScoreProfileRepository
import com.juzgon.domain.usecase.CalculateProfileRankedItemsUseCase
import com.juzgon.feature.home.MainDispatcherRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

class ItemDetailViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: FakeDetailRatedItemRepository
    private lateinit var snapshotRepository: FakeDetailAttributeRankSnapshotRepository
    private lateinit var categoryRepository: FakeDetailCategoryRepository
    private lateinit var scoreProfileRepository: FakeDetailScoreProfileRepository
    private lateinit var viewModel: ItemDetailViewModel

    private val clock = AppClock { LocalDate.of(2026, 6, 1) }

    @Before
    fun setUp() {
        repository = FakeDetailRatedItemRepository()
        snapshotRepository = FakeDetailAttributeRankSnapshotRepository()
        categoryRepository = FakeDetailCategoryRepository()
        scoreProfileRepository = FakeDetailScoreProfileRepository()
        viewModel =
            ItemDetailViewModel(
                repository,
                snapshotRepository,
                categoryRepository,
                scoreProfileRepository,
                CalculateProfileRankedItemsUseCase(),
                ItemDetailDateProcessor(BirthDateAgeCalculator(clock), DateScoreCalculator(clock)),
            )
    }

    @Test
    fun loadItemMapsAttributeScoresToDetailState() =
        runTest {
            repository.item.value =
                RatedItem(
                    id = "Roadster",
                    scores =
                        listOf(
                            ScoreEntry(Attribute("Speed"), 8),
                            ScoreEntry(Attribute("Brakes"), 6),
                        ),
                )

            viewModel.loadItem("Roadster")

            val state = viewModel.state.value
            assertFalse(state.isLoading)
            assertEquals("Roadster", state.itemId)
            assertEquals(2, state.attributeScores.size)
            assertEquals("Speed", state.attributeScores[0].label)
            assertEquals(8, state.attributeScores[0].score)
            assertEquals("Brakes", state.attributeScores[1].label)
            assertEquals(6, state.attributeScores[1].score)
        }

    @Test
    fun loadItemBuildsRankedAttributeProgressCards() =
        runTest {
            repository.item.value =
                RatedItem(
                    id = "Roadster",
                    scores =
                        listOf(
                            ScoreEntry(Attribute("Speed"), 8),
                            ScoreEntry(Attribute("Brakes"), 10),
                            ScoreEntry(Attribute("Control"), 6),
                        ),
                )

            viewModel.loadItem("Roadster")

            val cards = viewModel.state.value.rankedAttributes
            assertEquals(listOf("Brakes", "Speed", "Control"), cards.map { it.label })
            assertEquals(listOf(1, 2, 3), cards.map { it.rank })
            assertEquals(listOf(100, 80, 60), cards.map { it.progressPercent })
        }

    @Test
    fun loadItemBuildsDiamondChartPointsFromConfiguredNumericAttributes() =
        runTest {
            repository.item.value =
                RatedItem(
                    id = "Roadster",
                    scores =
                        listOf(
                            ScoreEntry(Attribute("Speed", diamondOrder = 2), 8),
                            ScoreEntry(Attribute("Brakes", diamondOrder = 1), 10),
                            ScoreEntry(Attribute("Comfort", displayInDiamond = false), 6),
                        ),
                )

            viewModel.loadItem("Roadster")

            val points = viewModel.state.value.diamondChartPoints
            assertEquals(listOf("Brakes", "Speed"), points.map { it.label })
            assertEquals(listOf(10, 8), points.map { it.value })
        }

    @Test
    fun loadItemBuildsRankAndValueMovementFromLatestPreviousSnapshots() =
        runTest {
            repository.item.value =
                RatedItem(
                    id = "Roadster",
                    updatedAt = 300L,
                    scores =
                        listOf(
                            ScoreEntry(Attribute("Speed"), 9),
                            ScoreEntry(Attribute("Brakes"), 8),
                        ),
                )
            snapshotRepository.snapshots.value = movementHistorySnapshots()

            viewModel.loadItem("Roadster")

            val cards = viewModel.state.value.rankedAttributes
            assertEquals(AttributeMovementDirection.Improved, cards[0].movement?.rank)
            assertEquals(AttributeMovementDirection.Improved, cards[0].movement?.value)
            assertEquals(AttributeMovementDirection.Declined, cards[1].movement?.rank)
            assertEquals(AttributeMovementDirection.Declined, cards[1].movement?.value)
        }

    @Test
    fun loadItemOmitsMovementWithoutPreviousSnapshots() =
        runTest {
            repository.item.value =
                RatedItem(
                    id = "Roadster",
                    updatedAt = 300L,
                    scores = listOf(ScoreEntry(Attribute("Speed"), 8)),
                )
            snapshotRepository.snapshots.value =
                listOf(
                    AttributeRankSnapshot(
                        itemId = "Roadster",
                        capturedAt = 300L,
                        attributeId = "Speed",
                        value = 8,
                        rank = 1,
                    ),
                )

            viewModel.loadItem("Roadster")

            assertNull(
                viewModel.state.value.rankedAttributes
                    .single()
                    .movement,
            )
        }

    @Test
    fun loadItemComputesEqualWeightOverallAverage() =
        runTest {
            repository.item.value =
                RatedItem(
                    id = "Roadster",
                    scores =
                        listOf(
                            ScoreEntry(Attribute("Speed"), 8),
                            ScoreEntry(Attribute("Brakes"), 6),
                        ),
                )

            viewModel.loadItem("Roadster")

            assertEquals("7.0", viewModel.state.value.overallScoreText)
        }

    @Test
    fun loadItemComputesWeightedOverallAverage() =
        runTest {
            repository.item.value =
                RatedItem(
                    id = "Roadster",
                    scores =
                        listOf(
                            ScoreEntry(Attribute("Speed", weight = 2.0), 10),
                            ScoreEntry(Attribute("Brakes", weight = 1.0), 4),
                        ),
                )

            viewModel.loadItem("Roadster")

            assertEquals("8.0", viewModel.state.value.overallScoreText)
        }

    @Test
    fun loadItemMapsNotesWhenPresent() =
        runTest {
            repository.item.value =
                RatedItem(
                    id = "Roadster",
                    notes = "weekend car",
                    scores = listOf(ScoreEntry(Attribute("Speed"), 8)),
                )

            viewModel.loadItem("Roadster")

            assertEquals("weekend car", viewModel.state.value.notes)
        }

    @Test
    fun loadItemSetsEmptyNotesWhenAbsent() =
        runTest {
            repository.item.value =
                RatedItem(id = "Roadster", scores = listOf(ScoreEntry(Attribute("Speed"), 8)))

            viewModel.loadItem("Roadster")

            assertEquals("", viewModel.state.value.notes)
        }

    @Test
    fun loadItemShowsErrorWhenNotFound() =
        runTest {
            repository.item.value = null

            viewModel.loadItem("Unknown")

            val state = viewModel.state.value
            assertFalse(state.isLoading)
            assertTrue(state.errorMessage != null)
        }

    @Test
    fun loadItemIsIdempotentForSameId() =
        runTest {
            repository.item.value =
                RatedItem(id = "Roadster", scores = listOf(ScoreEntry(Attribute("Speed"), 8)))

            viewModel.loadItem("Roadster")
            viewModel.loadItem("Roadster")

            assertFalse(viewModel.state.value.isLoading)
        }

    @Test
    fun loadItemMapsNonNumberAttributeValuesToAttributeValues() =
        runTest {
            val notesAttr = Attribute("Details", type = AttributeType.NOTES)
            repository.item.value =
                RatedItem(
                    id = "Roadster",
                    scores = listOf(ScoreEntry(Attribute("Speed"), 8)),
                    values = listOf(ItemAttributeValue(notesAttr, "Very fast car")),
                )

            viewModel.loadItem("Roadster")

            val state = viewModel.state.value
            assertEquals(1, state.attributeValues.size)
            assertEquals("Details", state.attributeValues[0].label)
            assertEquals("Very fast car", state.attributeValues[0].value)
        }

    @Test
    fun loadItemMapsImageAttributeValuesToAttributeValues() =
        runTest {
            val photoAttr = Attribute("Photo", type = AttributeType.IMAGE)
            repository.item.value =
                RatedItem(
                    id = "Roadster",
                    scores = listOf(ScoreEntry(Attribute("Speed"), 8)),
                    values = listOf(ItemAttributeValue(photoAttr, "content://images/roadster")),
                )

            viewModel.loadItem("Roadster")

            val state = viewModel.state.value
            assertEquals(1, state.attributeValues.size)
            assertEquals("Photo", state.attributeValues.single().label)
            assertEquals("content://images/roadster", state.attributeValues.single().value)
            assertEquals(AttributeType.IMAGE, state.attributeValues.single().type)
            assertEquals(
                "content://images/roadster",
                state.attributeValues
                    .single()
                    .imageReferences
                    .single()
                    .sourceUri,
            )
        }

    @Test
    fun loadItemMapsFirstImageValueAsPrimaryImage() =
        runTest {
            val photoAttr = Attribute("Photo", type = AttributeType.IMAGE)
            repository.item.value =
                RatedItem(
                    id = "Roadster",
                    scores = listOf(ScoreEntry(Attribute("Speed"), 8)),
                    values = listOf(ItemAttributeValue(photoAttr, "content://images/roadster")),
                )

            viewModel.loadItem("Roadster")

            assertEquals(
                "content://images/roadster",
                viewModel.state.value.primaryImage
                    ?.sourceUri,
            )
        }

    @Test
    fun loadItemFormatsTypedAttributeValuesForDisplay() =
        runTest {
            repository.item.value =
                RatedItem(
                    id = "Roadster",
                    scores = listOf(ScoreEntry(Attribute("Speed"), 8)),
                    values =
                        listOf(
                            ItemAttributeValue(Attribute("Available", type = AttributeType.BOOLEAN), "true"),
                            ItemAttributeValue(Attribute("Release", type = AttributeType.DATE), "2026-01-02"),
                            ItemAttributeValue(Attribute("Website", type = AttributeType.URL), "https://example.com"),
                        ),
                )

            viewModel.loadItem("Roadster")

            assertEquals(
                listOf("Yes", "Jan 2, 2026", "https://example.com"),
                viewModel.state.value.attributeValues
                    .map { it.displayValue },
            )
        }

    @Test
    fun profileBreakdownShownWhenProfileActive() =
        runTest {
            val speedAttr = Attribute("Speed", weight = 1.0)
            val brakesAttr = Attribute("Brakes", weight = 1.0)
            repository.item.value =
                RatedItem(
                    id = "Roadster",
                    scores = listOf(ScoreEntry(speedAttr, 8), ScoreEntry(brakesAttr, 6)),
                )
            repository.rankedItems.value =
                listOf(
                    RankedRatedItem(
                        item =
                            RatedItem(
                                id = "Roadster",
                                scores = listOf(ScoreEntry(speedAttr, 8), ScoreEntry(brakesAttr, 6)),
                            ),
                        aggregateScore = 7.0,
                    ),
                    RankedRatedItem(
                        item =
                            RatedItem(
                                id = "Sedan",
                                scores = listOf(ScoreEntry(speedAttr, 5), ScoreEntry(brakesAttr, 4)),
                            ),
                        aggregateScore = 4.5,
                    ),
                )
            categoryRepository.category =
                Category(name = "Cars", attributes = listOf(speedAttr, brakesAttr))
            scoreProfileRepository.profile =
                ScoreProfile(
                    id = "profile-1",
                    categoryName = "Cars",
                    name = "Speed Focus",
                    includedAttributeIds = listOf("Speed"),
                )

            viewModel.loadItem("Roadster", "Cars", "profile-1")

            val breakdown = viewModel.state.value.profileBreakdown
            assertEquals("Speed Focus", breakdown?.profileName)
            assertEquals("8.0", breakdown?.profileScoreText)
            assertEquals(1, breakdown?.profileRank)
            assertEquals(2, breakdown?.totalItems)
            assertEquals(setOf("Speed"), breakdown?.includedAttributeIds)
        }

    @Test
    fun profileBreakdownNullWhenNoProfile() =
        runTest {
            repository.item.value =
                RatedItem(
                    id = "Roadster",
                    scores = listOf(ScoreEntry(Attribute("Speed"), 8)),
                )

            viewModel.loadItem("Roadster", "Cars", null)

            assertNull(viewModel.state.value.profileBreakdown)
        }

    @Test
    fun profileBreakdownReflectsCorrectRankAmongItems() =
        runTest {
            val speedAttr = Attribute("Speed", weight = 1.0)
            repository.item.value =
                RatedItem(
                    id = "Roadster",
                    scores = listOf(ScoreEntry(speedAttr, 5)),
                )
            repository.rankedItems.value =
                listOf(
                    RankedRatedItem(
                        item = RatedItem(id = "Sedan", scores = listOf(ScoreEntry(speedAttr, 9))),
                        aggregateScore = 9.0,
                    ),
                    RankedRatedItem(
                        item = RatedItem(id = "Roadster", scores = listOf(ScoreEntry(speedAttr, 5))),
                        aggregateScore = 5.0,
                    ),
                )
            categoryRepository.category =
                Category(name = "Cars", attributes = listOf(speedAttr))
            scoreProfileRepository.profile =
                ScoreProfile(
                    id = "profile-1",
                    categoryName = "Cars",
                    name = "All",
                    includedAttributeIds = listOf("Speed"),
                )

            viewModel.loadItem("Roadster", "Cars", "profile-1")

            assertEquals(
                2,
                viewModel.state.value.profileBreakdown
                    ?.profileRank,
            )
        }

    @Test
    fun deleteConfirmedDeletesItemAndMarksCompletion() =
        runTest {
            repository.item.value =
                RatedItem(id = "Roadster", scores = listOf(ScoreEntry(Attribute("Speed"), 8)))
            viewModel.loadItem("Roadster")

            viewModel.onDeleteClick()
            viewModel.onDeleteConfirmed()

            assertEquals("Roadster", repository.deletedItemId)
            assertEquals(true, viewModel.state.value.deleteCompleted)
            assertEquals(false, viewModel.state.value.showDeleteConfirmDialog)
        }

    @Test
    fun defaultViewModeIsDiamond() =
        runTest {
            repository.item.value =
                RatedItem(id = "Roadster", scores = listOf(ScoreEntry(Attribute("Speed"), 8)))

            viewModel.loadItem("Roadster")

            assertEquals(ItemDetailViewMode.DIAMOND, viewModel.state.value.viewMode)
        }

    @Test
    fun onViewModeChangedUpdatesToBars() =
        runTest {
            repository.item.value =
                RatedItem(id = "Roadster", scores = listOf(ScoreEntry(Attribute("Speed"), 8)))
            viewModel.loadItem("Roadster")

            viewModel.onViewModeChanged(ItemDetailViewMode.BARS)

            assertEquals(ItemDetailViewMode.BARS, viewModel.state.value.viewMode)
        }

    @Test
    fun onViewModeChangedBackToDiamond() =
        runTest {
            repository.item.value =
                RatedItem(id = "Roadster", scores = listOf(ScoreEntry(Attribute("Speed"), 8)))
            viewModel.loadItem("Roadster")

            viewModel.onViewModeChanged(ItemDetailViewMode.BARS)
            viewModel.onViewModeChanged(ItemDetailViewMode.DIAMOND)

            assertEquals(ItemDetailViewMode.DIAMOND, viewModel.state.value.viewMode)
        }

    @Test
    fun loadItemPopulatesTierLabel() =
        runTest {
            repository.item.value =
                RatedItem(
                    id = "Roadster",
                    scores =
                        listOf(
                            ScoreEntry(Attribute("Speed"), 9),
                            ScoreEntry(Attribute("Brakes"), 9),
                        ),
                )

            viewModel.loadItem("Roadster")

            assertEquals("S-Tier", viewModel.state.value.tierLabel)
        }

    @Test
    fun loadItemPopulatesAttributeGrid() =
        runTest {
            repository.item.value =
                RatedItem(
                    id = "Roadster",
                    scores =
                        listOf(
                            ScoreEntry(Attribute("Speed"), 8),
                            ScoreEntry(Attribute("Brakes"), 6),
                        ),
                )

            viewModel.loadItem("Roadster")

            val grid = viewModel.state.value.attributeGrid
            assertEquals(2, grid.size)
            assertEquals("S", grid[0].emoji)
            assertEquals("Speed", grid[0].label)
            assertEquals("8/10", grid[0].scoreText)
            assertEquals("B", grid[1].emoji)
            assertEquals("Brakes", grid[1].label)
            assertEquals("6/10", grid[1].scoreText)
        }

    private class FakeDetailRatedItemRepository : RatedItemRepository {
        val item = MutableStateFlow<RatedItem?>(null)
        val rankedItems = MutableStateFlow<List<RankedRatedItem>>(emptyList())
        var deletedItemId: String? = null

        override fun observeRatedItems(): Flow<List<RatedItem>> = error("not used")

        override fun observeRatedItem(id: String): Flow<RatedItem?> = item

        override fun observeRankedItems(categoryName: String): Flow<List<RankedRatedItem>> = rankedItems

        override suspend fun saveRatedItem(ratedItem: RatedItem) = error("not used")

        override suspend fun renameRatedItem(
            originalId: String,
            ratedItem: RatedItem,
        ) = error("not used")

        override suspend fun deleteRatedItem(id: String) {
            deletedItemId = id
        }
    }

    private class FakeDetailAttributeRankSnapshotRepository : AttributeRankSnapshotRepository {
        val snapshots = MutableStateFlow<List<AttributeRankSnapshot>>(emptyList())

        override fun observeSnapshotsForItem(itemId: String): Flow<List<AttributeRankSnapshot>> = snapshots
    }

    private class FakeDetailCategoryRepository : CategoryRepository {
        var category: Category? = null

        override fun observeCategories(): Flow<List<Category>> = error("not used")

        override fun observeCategory(name: String): Flow<Category?> = flowOf(category)

        override suspend fun saveCategory(category: Category) = error("not used")

        override suspend fun renameCategory(
            originalName: String,
            category: Category,
            renamedAttributeIds: Map<String, String>,
        ) = error("not used")

        override suspend fun deleteCategory(name: String) = error("not used")
    }

    private class FakeDetailScoreProfileRepository : ScoreProfileRepository {
        var profile: ScoreProfile? = null

        override fun observeProfilesForCategory(categoryName: String): Flow<List<ScoreProfile>> = error("not used")

        override fun observeProfile(id: String): Flow<ScoreProfile?> = flowOf(profile)

        override suspend fun saveProfile(profile: ScoreProfile) = error("not used")

        override suspend fun deleteProfile(id: String) = error("not used")
    }

    private fun movementHistorySnapshots(): List<AttributeRankSnapshot> =
        listOf(
            AttributeRankSnapshot(
                itemId = "Roadster",
                capturedAt = 100L,
                attributeId = "Speed",
                value = 6,
                rank = 2,
            ),
            AttributeRankSnapshot(
                itemId = "Roadster",
                capturedAt = 100L,
                attributeId = "Brakes",
                value = 10,
                rank = 1,
            ),
            AttributeRankSnapshot(
                itemId = "Roadster",
                capturedAt = 200L,
                attributeId = "Speed",
                value = 7,
                rank = 2,
            ),
            AttributeRankSnapshot(
                itemId = "Roadster",
                capturedAt = 200L,
                attributeId = "Brakes",
                value = 9,
                rank = 1,
            ),
            AttributeRankSnapshot(
                itemId = "Roadster",
                capturedAt = 300L,
                attributeId = "Speed",
                value = 9,
                rank = 1,
            ),
            AttributeRankSnapshot(
                itemId = "Roadster",
                capturedAt = 300L,
                attributeId = "Brakes",
                value = 8,
                rank = 2,
            ),
        )
}
