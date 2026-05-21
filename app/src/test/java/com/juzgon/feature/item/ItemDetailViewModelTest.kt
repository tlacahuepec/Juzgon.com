package com.juzgon.feature.item

import com.juzgon.domain.Attribute
import com.juzgon.domain.RankedRatedItem
import com.juzgon.domain.RatedItem
import com.juzgon.domain.ScoreEntry
import com.juzgon.domain.repository.RatedItemRepository
import com.juzgon.feature.home.MainDispatcherRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class ItemDetailViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: FakeDetailRatedItemRepository
    private lateinit var viewModel: ItemDetailViewModel

    @Before
    fun setUp() {
        repository = FakeDetailRatedItemRepository()
        viewModel = ItemDetailViewModel(repository)
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

    private class FakeDetailRatedItemRepository : RatedItemRepository {
        val item = MutableStateFlow<RatedItem?>(null)

        override fun observeRatedItems(): Flow<List<RatedItem>> = error("not used")

        override fun observeRatedItem(id: String): Flow<RatedItem?> = item

        override fun observeRankedItems(categoryName: String): Flow<List<RankedRatedItem>> = error("not used")

        override suspend fun saveRatedItem(ratedItem: RatedItem) = error("not used")

        override suspend fun deleteRatedItem(id: String) = error("not used")
    }
}
