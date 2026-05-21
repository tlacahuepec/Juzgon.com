package com.juzgon.feature.item

import com.juzgon.domain.Attribute
import com.juzgon.domain.Category
import com.juzgon.domain.RankedRatedItem
import com.juzgon.domain.RatedItem
import com.juzgon.domain.ScoreEntry
import com.juzgon.domain.repository.CategoryRepository
import com.juzgon.domain.repository.RatedItemRepository
import com.juzgon.domain.usecase.ValidateRatingsUseCase
import com.juzgon.feature.home.MainDispatcherRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class ItemFormViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var categoryRepository: FakeCategoryRepository
    private lateinit var ratedItemRepository: FakeRatedItemRepository
    private lateinit var viewModel: ItemFormViewModel

    @Before
    fun setUp() {
        categoryRepository = FakeCategoryRepository()
        ratedItemRepository = FakeRatedItemRepository()
        viewModel = ItemFormViewModel(categoryRepository, ratedItemRepository, ValidateRatingsUseCase())
    }

    @Test
    fun loadCategoryCreatesOneScoreInputPerAttribute() =
        runTest {
            categoryRepository.categories.value = listOf(carsCategory)

            viewModel.loadCategory("Cars")

            assertEquals("Cars", currentState.categoryName)
            assertEquals(false, currentState.isLoading)
            assertEquals(listOf("Speed", "Brakes"), currentState.scores.map { it.attribute.id })
        }

    @Test
    fun editModeLoadsExistingItemValues() =
        runTest {
            categoryRepository.categories.value = listOf(carsCategory)
            ratedItemRepository.item.value =
                RatedItem(
                    id = "Roadster",
                    notes = "weekend car",
                    scores =
                        listOf(
                            ScoreEntry(speed, 6),
                            ScoreEntry(brakes, 10),
                        ),
                    createdAt = 100,
                    updatedAt = 200,
                )

            viewModel.loadCategory("Cars", itemId = "Roadster")

            assertEquals(ItemFormMode.Edit, currentState.mode)
            assertEquals("Roadster", currentState.title)
            assertEquals("weekend car", currentState.notes)
            assertEquals(listOf("6", "10"), currentState.scores.map { it.scoreText })
            assertTrue(currentState.saveEnabled)
        }

    @Test
    fun missingTitleAndScoresBlockSave() =
        runTest {
            categoryRepository.categories.value = listOf(carsCategory)
            viewModel.loadCategory("Cars")

            viewModel.onSaveClick()

            assertEquals(null, ratedItemRepository.savedItem)
            assertTrue(currentState.showValidationErrors)
            assertFalse(currentState.saveEnabled)
            assertEquals("Title is required", currentState.titleError)
            assertEquals(listOf("Score is required", "Score is required"), currentState.scoreErrors.map { it.score })
        }

    @Test
    fun boundaryScoresOneAndTenAreValid() =
        runTest {
            categoryRepository.categories.value = listOf(carsCategory)
            viewModel.loadCategory("Cars")

            viewModel.onTitleChanged("Roadster")
            viewModel.onScoreChanged("Speed", "1")
            viewModel.onScoreChanged("Brakes", "10")

            assertTrue(currentState.saveEnabled)

            viewModel.onSaveClick()

            assertEquals(
                RatedItem(
                    id = "Roadster",
                    scores =
                        listOf(
                            ScoreEntry(speed, 1),
                            ScoreEntry(brakes, 10),
                        ),
                ),
                ratedItemRepository.savedItem,
            )
        }

    @Test
    fun invalidScoreInputBlocksSave() =
        runTest {
            categoryRepository.categories.value = listOf(carsCategory)
            viewModel.loadCategory("Cars")
            viewModel.onTitleChanged("Roadster")

            viewModel.onScoreChanged("Speed", "abc")
            viewModel.onScoreChanged("Brakes", "8")

            assertFalse(currentState.saveEnabled)
            assertEquals("Score must be a whole number", currentState.scoreErrors.first().score)

            viewModel.onScoreChanged("Speed", "11")

            assertFalse(currentState.saveEnabled)
            assertEquals("Score must be between 1 and 10", currentState.scoreErrors.first().score)
            viewModel.onSaveClick()
            assertEquals(null, ratedItemRepository.savedItem)
        }

    @Test
    fun validSaveTrimsTitleAndNotesAndPersistsScores() =
        runTest {
            categoryRepository.categories.value = listOf(carsCategory)
            viewModel.loadCategory("Cars")

            viewModel.onTitleChanged("  Roadster  ")
            viewModel.onNotesChanged("  weekend car  ")
            viewModel.onScoreChanged("Speed", "9")
            viewModel.onScoreChanged("Brakes", "8")
            viewModel.onSaveClick()

            assertEquals(
                RatedItem(
                    id = "Roadster",
                    notes = "weekend car",
                    scores =
                        listOf(
                            ScoreEntry(speed, 9),
                            ScoreEntry(brakes, 8),
                        ),
                ),
                ratedItemRepository.savedItem,
            )
            assertTrue(currentState.saveCompleted)
        }

    @Test
    fun editSaveUpdatesExistingItemWithoutDuplicateError() =
        runTest {
            categoryRepository.categories.value = listOf(carsCategory)
            ratedItemRepository.item.value =
                RatedItem(
                    id = "Roadster",
                    notes = "old notes",
                    scores =
                        listOf(
                            ScoreEntry(speed, 6),
                            ScoreEntry(brakes, 7),
                        ),
                    createdAt = 100,
                    updatedAt = 200,
                )
            viewModel.loadCategory("Cars", itemId = "Roadster")

            viewModel.onNotesChanged("  track day  ")
            viewModel.onScoreChanged("Speed", "9")
            viewModel.onSaveClick()

            assertEquals(
                RatedItem(
                    id = "Roadster",
                    notes = "track day",
                    scores =
                        listOf(
                            ScoreEntry(speed, 9),
                            ScoreEntry(brakes, 7),
                        ),
                ),
                ratedItemRepository.savedItem,
            )
            assertTrue(currentState.saveCompleted)
        }

    @Test
    fun saveFailureShowsErrorAndDoesNotComplete() =
        runTest {
            categoryRepository.categories.value = listOf(carsCategory)
            ratedItemRepository.errorOnSave = IllegalStateException("Unable to save item")
            viewModel.loadCategory("Cars")
            viewModel.onTitleChanged("Roadster")
            viewModel.onScoreChanged("Speed", "9")
            viewModel.onScoreChanged("Brakes", "8")

            viewModel.onSaveClick()

            assertEquals("Unable to save item", currentState.errorMessage)
            assertFalse(currentState.saveCompleted)
        }

    private class FakeCategoryRepository : CategoryRepository {
        val categories = MutableStateFlow(emptyList<Category>())

        override fun observeCategories(): Flow<List<Category>> = categories

        override fun observeCategory(name: String): Flow<Category?> =
            categories.map { categories -> categories.firstOrNull { it.name == name } }

        override suspend fun saveCategory(category: Category) {
            error("ItemFormViewModel does not save categories")
        }

        override suspend fun renameCategory(
            originalName: String,
            category: Category,
        ) {
            error("ItemFormViewModel does not rename categories")
        }

        override suspend fun deleteCategory(name: String) {
            error("ItemFormViewModel does not delete categories")
        }
    }

    private class FakeRatedItemRepository : RatedItemRepository {
        val item = MutableStateFlow<RatedItem?>(null)
        var savedItem: RatedItem? = null
        var errorOnSave: Throwable? = null

        override fun observeRatedItems(): Flow<List<RatedItem>> {
            error("ItemFormViewModel does not observe all rated items")
        }

        override fun observeRatedItem(id: String): Flow<RatedItem?> = item

        override fun observeRankedItems(categoryName: String): Flow<List<RankedRatedItem>> {
            error("ItemFormViewModel does not observe ranked items")
        }

        override suspend fun saveRatedItem(ratedItem: RatedItem) {
            errorOnSave?.let { throw it }
            savedItem = ratedItem
        }

        override suspend fun deleteRatedItem(id: String) {
            error("ItemFormViewModel does not delete rated items")
        }
    }

    private val currentState: ItemFormUiState
        get() = viewModel.state.value

    private companion object {
        val speed = Attribute("Speed")
        val brakes = Attribute("Brakes")
        val carsCategory = Category(name = "Cars", attributes = listOf(speed, brakes))
    }
}
