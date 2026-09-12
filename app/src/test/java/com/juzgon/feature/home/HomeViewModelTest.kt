package com.juzgon.feature.home

import app.cash.turbine.test
import com.juzgon.domain.Attribute
import com.juzgon.domain.Category
import com.juzgon.domain.RankedRatedItem
import com.juzgon.domain.RatedItem
import com.juzgon.domain.ScoreEntry
import com.juzgon.domain.repository.CategoryRepository
import com.juzgon.domain.repository.RatedItemRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakeRepository: FakeCategoryRepository
    private lateinit var fakeRatedItemRepository: FakeRatedItemRepository
    private lateinit var viewModel: HomeViewModel

    @Before
    fun setUp() {
        fakeRepository = FakeCategoryRepository()
        fakeRatedItemRepository = FakeRatedItemRepository()
        viewModel = HomeViewModel(fakeRepository, fakeRatedItemRepository)
    }

    @Test
    fun stateShowsCategoriesInRepositoryOrderByDefault() =
        runTest {
            viewModel.state.test {
                assertEquals(emptyList<HomeCategoryUiModel>(), awaitItem().categories)

                fakeRepository.categories.value = listOf(travelCategory, foodCategory)

                assertEquals(
                    listOf(
                        HomeCategoryUiModel(name = "Travel", attributeCount = 1, itemCount = 0),
                        HomeCategoryUiModel(name = "Food", attributeCount = 2, itemCount = 0),
                    ),
                    awaitItem().categories,
                )
            }
        }

    @Test
    fun itemCountIsMappedFromCategoryDomainModel() =
        runTest {
            val foodWithItems = foodCategory.copy(itemCount = 3)
            val travelWithItems = travelCategory.copy(itemCount = 1)
            fakeRepository.categories.value = listOf(travelWithItems, foodWithItems)

            viewModel.state.test {
                var state = awaitItem()
                if (state.isLoading) state = awaitItem()

                assertEquals(
                    listOf(
                        HomeCategoryUiModel(name = "Travel", attributeCount = 1, itemCount = 1),
                        HomeCategoryUiModel(name = "Food", attributeCount = 2, itemCount = 3),
                    ),
                    state.categories,
                )
            }
        }

    @Test
    fun collectionStatsAreDerivedFromAllLoadedCategories() =
        runTest {
            val foodWithItems = foodCategory.copy(itemCount = 3)
            val travelWithItems = travelCategory.copy(itemCount = 1)
            fakeRepository.categories.value = listOf(travelWithItems, foodWithItems)

            viewModel.state.test {
                var state = awaitItem()
                if (state.isLoading) state = awaitItem()

                assertEquals(
                    HomeCollectionStatsUiModel(
                        categoryCount = 2,
                        itemCount = 4,
                        attributeCount = 3,
                    ),
                    state.collectionStats,
                )
            }
        }

    @Test
    fun searchFiltersCategoriesByName() =
        runTest {
            fakeRepository.categories.value = listOf(travelCategory, foodCategory, musicCategory)

            viewModel.state.test {
                assertEquals(3, awaitItem().categories.size)

                viewModel.onSearchQueryChanged("oo")

                val state = awaitItem()
                assertEquals("oo", state.searchQuery)
                assertEquals(
                    listOf(HomeCategoryUiModel(name = "Food", attributeCount = 2, itemCount = 0)),
                    state.categories,
                )
            }
        }

    @Test
    fun nameSortOrdersCategoriesByName() =
        runTest {
            fakeRepository.categories.value = listOf(travelCategory, foodCategory, musicCategory)

            viewModel.state.test {
                assertEquals(3, awaitItem().categories.size)

                viewModel.onSortOptionSelected(HomeSortOption.Name)

                val state = awaitItem()
                assertEquals(HomeSortOption.Name, state.sortOption)
                assertEquals(
                    listOf(
                        HomeCategoryUiModel(name = "Food", attributeCount = 2, itemCount = 0),
                        HomeCategoryUiModel(name = "Music", attributeCount = 1, itemCount = 0),
                        HomeCategoryUiModel(name = "Travel", attributeCount = 1, itemCount = 0),
                    ),
                    state.categories,
                )
            }
        }

    @Test
    fun createCategoryClickEmitsNavigationEvent() =
        runTest {
            viewModel.navigationEvents.test {
                viewModel.onCreateCategoryClick()

                assertEquals(HomeNavigationEvent.CreateCategory, awaitItem())
            }
        }

    @Test
    fun categoryClickEmitsNavigationEvent() =
        runTest {
            viewModel.navigationEvents.test {
                viewModel.onCategoryClick("Cars")

                assertEquals(HomeNavigationEvent.OpenCategory("Cars"), awaitItem())
            }
        }

    @Test
    fun itemClickEmitsNavigationEvent() =
        runTest {
            viewModel.navigationEvents.test {
                viewModel.onItemClick("Formula 1", "Max Verstappen")

                assertEquals(
                    HomeNavigationEvent.OpenItem("Formula 1", "Max Verstappen"),
                    awaitItem(),
                )
            }
        }

    @Test
    fun topItemsFlowDerivesHeroAndTrendingWithRadarPointsAndTieBreaking() =
        runTest {
            val scoreEntries =
                listOf(
                    ScoreEntry(Attribute("speed"), 10),
                    ScoreEntry(Attribute("consistency"), 9),
                    ScoreEntry(Attribute("overtaking"), 9),
                )
            val topItem =
                RankedRatedItem(
                    item = RatedItem(id = "Max Verstappen", scores = scoreEntries),
                    aggregateScore = 9.6,
                )
            val tiedItemA =
                RankedRatedItem(
                    item = RatedItem(id = "Charles Leclerc", scores = emptyList()),
                    aggregateScore = 9.2,
                )
            val tiedItemB =
                RankedRatedItem(
                    item = RatedItem(id = "Lando Norris", scores = emptyList()),
                    aggregateScore = 9.2,
                )

            val categoryRepo = FakeCategoryRepository()
            categoryRepo.categories.value = listOf(Category(name = "F1", attributes = listOf(Attribute("speed"))))
            val ratedRepo =
                FakeRatedItemRepository(
                    rankedItemsByCat =
                        mapOf(
                            "F1" to flowOf(listOf(tiedItemB, topItem, tiedItemA)),
                        ),
                )
            val vm = HomeViewModel(categoryRepo, ratedRepo)

            vm.state.test {
                var state = awaitItem()
                while (state.heroItem == null) {
                    state = awaitItem()
                }

                val hero = state.heroItem
                assertNotNull(hero)
                assertEquals("Max Verstappen", hero?.name)
                assertEquals("Max Verstappen", hero?.itemId)
                assertEquals("F1", hero?.categoryName)
                assertEquals("S-Tier", hero?.tierLabel)
                assertEquals("9.6/10", hero?.scoreText)
                assertEquals(3, hero?.radarPoints?.size)

                // Trending items are sorted by score descending, then by id ascending ("Charles Leclerc" before "Lando Norris")
                assertEquals(3, state.trendingItems.size)
                assertEquals("Max Verstappen", state.trendingItems[0].name)
                assertEquals("Charles Leclerc", state.trendingItems[1].name)
                assertEquals("Lando Norris", state.trendingItems[2].name)
            }
        }

    @Test
    fun initialStateIsLoading() {
        assertTrue(viewModel.state.value.isLoading)
    }

    @Test
    fun stateIsNotLoadingAfterCategoriesEmit() =
        runTest {
            viewModel.state.test {
                // With UnconfinedTestDispatcher the initial loading state may transition
                // to the loaded state synchronously; consume items until non-loading.
                var state = awaitItem()
                if (state.isLoading) {
                    state = awaitItem()
                }

                fakeRepository.categories.value = listOf(travelCategory)

                val loaded = awaitItem()
                assertEquals(false, loaded.isLoading)
                assertNull(loaded.errorMessage)
            }
        }

    @Test
    fun stateShowsErrorWhenRepositoryThrows() =
        runTest {
            val throwingRepo = ThrowingCategoryRepository()
            val vm = HomeViewModel(throwingRepo, FakeRatedItemRepository())

            vm.state.test {
                var state = awaitItem()
                if (state.isLoading) {
                    state = awaitItem()
                }
                assertNotNull(state.errorMessage)
                assertEquals(false, state.isLoading)
            }
        }

    @Test
    fun retryResubscribesToRepository() =
        runTest {
            val throwingRepo = ThrowingCategoryRepository()
            val vm = HomeViewModel(throwingRepo, FakeRatedItemRepository())

            vm.state.test {
                var state = awaitItem()
                if (state.isLoading) {
                    state = awaitItem()
                }
                assertNotNull(state.errorMessage)

                throwingRepo.shouldThrow = false
                vm.onRetry()

                var loaded = awaitItem()
                if (loaded.isLoading) {
                    loaded = awaitItem()
                }
                assertEquals(false, loaded.isLoading)
                assertNull(loaded.errorMessage)
                cancelAndIgnoreRemainingEvents()
            }
        }

    private class FakeCategoryRepository : CategoryRepository {
        val categories = MutableStateFlow(emptyList<Category>())

        override fun observeCategories(): Flow<List<Category>> = categories

        override fun observeCategory(name: String): Flow<Category?> {
            error("HomeViewModel does not observe one category")
        }

        override suspend fun saveCategory(category: Category) {
            error("HomeViewModel does not save categories")
        }

        override suspend fun renameCategory(
            originalName: String,
            category: Category,
            renamedAttributeIds: Map<String, String>,
        ) {
            error("HomeViewModel does not rename categories")
        }

        override suspend fun deleteCategory(name: String) {
            error("HomeViewModel does not delete categories")
        }
    }

    private class ThrowingCategoryRepository : CategoryRepository {
        var shouldThrow = true

        override fun observeCategories(): Flow<List<Category>> =
            flow {
                if (shouldThrow) throw RepositoryUnavailableException()
                emit(emptyList())
            }

        override fun observeCategory(name: String): Flow<Category?> {
            error("not used")
        }

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

    private class RepositoryUnavailableException : Exception("DB error")

    private class FakeRatedItemRepository(
        private val rankedItemsByCat: Map<String, Flow<List<RankedRatedItem>>> = emptyMap(),
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
        val foodCategory = Category(name = "Food", attributes = listOf(Attribute("taste"), Attribute("service")))
        val musicCategory = Category(name = "Music", attributes = listOf(Attribute("sound")))
        val travelCategory = Category(name = "Travel", attributes = listOf(Attribute("comfort")))
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    private val testDispatcher: TestDispatcher = UnconfinedTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
