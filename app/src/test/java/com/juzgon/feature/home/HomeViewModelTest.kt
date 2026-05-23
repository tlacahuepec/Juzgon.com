package com.juzgon.feature.home

import app.cash.turbine.test
import com.juzgon.domain.Attribute
import com.juzgon.domain.Category
import com.juzgon.domain.repository.CategoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
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
    private lateinit var viewModel: HomeViewModel

    @Before
    fun setUp() {
        fakeRepository = FakeCategoryRepository()
        viewModel = HomeViewModel(fakeRepository)
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
            val vm = HomeViewModel(throwingRepo)

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
            val vm = HomeViewModel(throwingRepo)

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
