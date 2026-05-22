package com.juzgon.feature.item

import com.juzgon.domain.Attribute
import com.juzgon.domain.AttributeRankSnapshot
import com.juzgon.domain.AttributeType
import com.juzgon.domain.ItemAttributeValue
import com.juzgon.domain.RankedRatedItem
import com.juzgon.domain.RatedItem
import com.juzgon.domain.ScoreEntry
import com.juzgon.domain.repository.AttributeRankSnapshotRepository
import com.juzgon.domain.repository.RatedItemRepository
import com.juzgon.feature.home.MainDispatcherRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class ItemDetailViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: FakeDetailRatedItemRepository
    private lateinit var snapshotRepository: FakeDetailAttributeRankSnapshotRepository
    private lateinit var viewModel: ItemDetailViewModel

    @Before
    fun setUp() {
        repository = FakeDetailRatedItemRepository()
        snapshotRepository = FakeDetailAttributeRankSnapshotRepository()
        viewModel = ItemDetailViewModel(repository, snapshotRepository)
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

    private class FakeDetailRatedItemRepository : RatedItemRepository {
        val item = MutableStateFlow<RatedItem?>(null)

        override fun observeRatedItems(): Flow<List<RatedItem>> = error("not used")

        override fun observeRatedItem(id: String): Flow<RatedItem?> = item

        override fun observeRankedItems(categoryName: String): Flow<List<RankedRatedItem>> = error("not used")

        override suspend fun saveRatedItem(ratedItem: RatedItem) = error("not used")

        override suspend fun deleteRatedItem(id: String) = error("not used")
    }

    private class FakeDetailAttributeRankSnapshotRepository : AttributeRankSnapshotRepository {
        val snapshots = MutableStateFlow<List<AttributeRankSnapshot>>(emptyList())

        override fun observeSnapshotsForItem(itemId: String): Flow<List<AttributeRankSnapshot>> = snapshots
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
