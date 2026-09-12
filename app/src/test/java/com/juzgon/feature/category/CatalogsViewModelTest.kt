package com.juzgon.feature.category

import app.cash.turbine.test
import com.juzgon.domain.Attribute
import com.juzgon.domain.CatalogType
import com.juzgon.domain.Category
import com.juzgon.domain.RankedRatedItem
import com.juzgon.domain.RatedItem
import com.juzgon.domain.ScoreEntry
import com.juzgon.domain.repository.CategoryRepository
import com.juzgon.domain.repository.RatedItemRepository
import com.juzgon.feature.home.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CatalogsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakeCategoryRepository: FakeCategoryRepository
    private lateinit var fakeRatedItemRepository: FakeRatedItemRepository
    private lateinit var viewModel: CatalogsViewModel

    @Before
    fun setUp() {
        fakeCategoryRepository = FakeCategoryRepository()
        fakeRatedItemRepository = FakeRatedItemRepository()
        viewModel = CatalogsViewModel(fakeCategoryRepository, fakeRatedItemRepository)
    }

    @Test
    fun initialStateIsLoading() {
        assertTrue(viewModel.state.value.isLoading)
    }

    @Test
    fun stateLoadsCategoriesFromRepository() =
        runTest {
            viewModel.state.test {
                var state = awaitItem()
                if (state.isLoading) {
                    state = awaitItem()
                }

                fakeCategoryRepository.categories.value = listOf(f1Category, scifiCategory)

                val loaded = awaitItem()
                assertFalse(loaded.isLoading)
                assertEquals(2, loaded.categories.size)
                assertEquals("Formula 1 Drivers", loaded.categories[0].name)
                assertEquals(20, loaded.categories[0].itemCount)
                assertEquals(CatalogType.PERSON, loaded.categories[0].type)
                assertEquals("Sci-Fi Movies", loaded.categories[1].name)
                assertEquals(15, loaded.categories[1].itemCount)
                assertEquals(CatalogType.MOVIE, loaded.categories[1].type)
            }
        }

    @Test
    fun searchQueryFiltersCategoriesByName() =
        runTest {
            fakeCategoryRepository.categories.value = listOf(f1Category, scifiCategory, coffeeCategory)

            viewModel.state.test {
                val initial = awaitItem()
                assertEquals(3, initial.categories.size)

                viewModel.onSearchQueryChanged("Sci-Fi")

                val searched = awaitItem()
                assertEquals("Sci-Fi", searched.searchQuery)
                assertEquals(1, searched.categories.size)
                assertEquals("Sci-Fi Movies", searched.categories[0].name)
            }
        }

    @Test
    fun typeFilterFiltersCategoriesByType() =
        runTest {
            fakeCategoryRepository.categories.value = listOf(f1Category, scifiCategory, coffeeCategory)

            viewModel.state.test {
                val initial = awaitItem()
                assertEquals(3, initial.categories.size)

                viewModel.onTypeFilterSelected(CatalogType.PRODUCT)

                val filtered = awaitItem()
                assertEquals(CatalogType.PRODUCT, filtered.selectedType)
                assertEquals(1, filtered.categories.size)
                assertEquals("Specialty Coffee", filtered.categories[0].name)

                // Back to All
                viewModel.onTypeFilterSelected(null)
                val allState = awaitItem()
                assertNull(allState.selectedType)
                assertEquals(3, allState.categories.size)
            }
        }

    @Test
    fun typeFilterChipsIncludeAllAndPresentTypes() =
        runTest {
            fakeCategoryRepository.categories.value = listOf(f1Category, scifiCategory, coffeeCategory)

            viewModel.state.test {
                val state = awaitItem()
                val chipLabels = state.filterChips.map { it.label }
                assertTrue(chipLabels.contains("All"))
                assertTrue(chipLabels.contains("People"))
                assertTrue(chipLabels.contains("Films"))
                assertTrue(chipLabels.contains("Products"))
            }
        }

    @Test
    fun searchAndTypeFilterWorkTogether() =
        runTest {
            val gamesCategory = Category(name = "Sci-Fi Games", attributes = listOf(Attribute("fun")), type = CatalogType.VIDEO_GAME)
            fakeCategoryRepository.categories.value = listOf(scifiCategory, gamesCategory, f1Category)

            viewModel.state.test {
                val initial = awaitItem()
                assertEquals(3, initial.categories.size)

                // Search for Sci-Fi
                viewModel.onSearchQueryChanged("Sci-Fi")
                val searched = awaitItem()
                assertEquals(2, searched.categories.size)

                // Filter by VIDEO_GAME
                viewModel.onTypeFilterSelected(CatalogType.VIDEO_GAME)
                val filtered = awaitItem()
                assertEquals(1, filtered.categories.size)
                assertEquals("Sci-Fi Games", filtered.categories[0].name)
            }
        }

    @Test
    fun averageRatingsDerivedFromRatedItemRepository() =
        runTest {
            val f1Item1 =
                RankedRatedItem(
                    item = RatedItem(id = "Max Verstappen", scores = listOf(ScoreEntry(Attribute("speed"), 10))),
                    aggregateScore = 9.8,
                )
            val f1Item2 =
                RankedRatedItem(
                    item = RatedItem(id = "Charles Leclerc", scores = listOf(ScoreEntry(Attribute("speed"), 9))),
                    aggregateScore = 9.0,
                )

            fakeCategoryRepository.categories.value = listOf(f1Category)
            fakeRatedItemRepository.rankedItemsByCat = mapOf("Formula 1 Drivers" to flowOf(listOf(f1Item1, f1Item2)))

            val vm = CatalogsViewModel(fakeCategoryRepository, fakeRatedItemRepository)

            vm.state.test {
                var state = awaitItem()
                while (state.categories.isEmpty() || state.categories[0].averageRating == null) {
                    state = awaitItem()
                }

                val card = state.categories[0]
                assertNotNull(card.averageRating)
                assertEquals(9.4, card.averageRating!!, 0.01)
                assertEquals("★ 9.4", card.averageRatingText)
            }
        }

    @Test
    fun navigationEventsEmittedOnFabAndCategoryClick() =
        runTest {
            viewModel.navigationEvents.test {
                viewModel.onCreateCategoryClick()
                assertEquals(CatalogsNavigationEvent.CreateCategory, awaitItem())

                viewModel.onCategoryClick("Formula 1 Drivers")
                assertEquals(CatalogsNavigationEvent.OpenCategory("Formula 1 Drivers"), awaitItem())
            }
        }

    @Test
    fun stateShowsErrorWhenRepositoryThrowsAndRetryWorks() =
        runTest {
            val throwingRepo = ThrowingCategoryRepository()
            val vm = CatalogsViewModel(throwingRepo, fakeRatedItemRepository)

            vm.state.test {
                var state = awaitItem()
                if (state.isLoading) {
                    state = awaitItem()
                }
                assertNotNull(state.errorMessage)
                assertFalse(state.isLoading)

                throwingRepo.shouldThrow = false
                vm.onRetry()

                var loaded = awaitItem()
                if (loaded.isLoading) {
                    loaded = awaitItem()
                }
                assertFalse(loaded.isLoading)
                assertNull(loaded.errorMessage)
                cancelAndIgnoreRemainingEvents()
            }
        }

    private class FakeCategoryRepository : CategoryRepository {
        val categories = MutableStateFlow(emptyList<Category>())

        override fun observeCategories(): Flow<List<Category>> = categories

        override fun observeCategory(name: String): Flow<Category?> = flowOf(null)

        override suspend fun saveCategory(category: Category) {
            error("not used")
        }

        override suspend fun renameCategory(
            originalName: String,
            category: Category,
            renamedAttributeIds: Map<String, String>,
        ) {
            error("not used")
        }

        override suspend fun deleteCategory(name: String) {
            error("not used")
        }
    }

    private class ThrowingCategoryRepository : CategoryRepository {
        var shouldThrow = true

        override fun observeCategories(): Flow<List<Category>> =
            flow {
                if (shouldThrow) throw RepositoryUnavailableException()
                emit(emptyList())
            }

        private class RepositoryUnavailableException : Exception("DB error")

        override fun observeCategory(name: String): Flow<Category?> = flowOf(null)

        override suspend fun saveCategory(category: Category) {
            error("not used")
        }

        override suspend fun renameCategory(
            originalName: String,
            category: Category,
            renamedAttributeIds: Map<String, String>,
        ) {
            error("not used")
        }

        override suspend fun deleteCategory(name: String) {
            error("not used")
        }
    }

    private class FakeRatedItemRepository(
        var rankedItemsByCat: Map<String, Flow<List<RankedRatedItem>>> = emptyMap(),
    ) : RatedItemRepository {
        override fun observeRatedItems(): Flow<List<RatedItem>> = flowOf(emptyList())

        override fun observeRatedItem(id: String): Flow<RatedItem?> = flowOf(null)

        override fun observeRankedItems(categoryName: String): Flow<List<RankedRatedItem>> =
            rankedItemsByCat[categoryName] ?: flowOf(emptyList())

        override suspend fun saveRatedItem(ratedItem: RatedItem) {
            error("not used")
        }

        override suspend fun renameRatedItem(
            originalId: String,
            ratedItem: RatedItem,
        ) {
            error("not used")
        }

        override suspend fun deleteRatedItem(id: String) {
            error("not used")
        }
    }

    private companion object {
        val f1Category =
            Category(
                name = "Formula 1 Drivers",
                attributes = listOf(Attribute("speed")),
                itemCount = 20,
                type = CatalogType.PERSON,
            )
        val scifiCategory =
            Category(
                name = "Sci-Fi Movies",
                attributes = listOf(Attribute("plot")),
                itemCount = 15,
                type = CatalogType.MOVIE,
            )
        val coffeeCategory =
            Category(
                name = "Specialty Coffee",
                attributes = listOf(Attribute("aroma")),
                itemCount = 12,
                type = CatalogType.PRODUCT,
            )
    }
}
