package com.juzgon.feature.home

import app.cash.turbine.test
import com.juzgon.domain.Category
import com.juzgon.domain.repository.CategoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: FakeCategoryRepository
    private lateinit var viewModel: HomeViewModel

    @Before
    fun setUp() {
        repository = FakeCategoryRepository()
        viewModel = HomeViewModel(repository)
    }

    @Test
    fun stateShowsCategoriesInRepositoryOrderByDefault() =
        runTest {
            viewModel.state.test {
                assertEquals(emptyList<HomeCategoryUiModel>(), awaitItem().categories)

                repository.categories.value = listOf(travelCategory, foodCategory)

                assertEquals(
                    listOf(
                        HomeCategoryUiModel(name = "Travel", attributeCount = 1),
                        HomeCategoryUiModel(name = "Food", attributeCount = 2),
                    ),
                    awaitItem().categories,
                )
            }
        }

    @Test
    fun searchFiltersCategoriesByName() =
        runTest {
            repository.categories.value = listOf(travelCategory, foodCategory, musicCategory)

            viewModel.state.test {
                assertEquals(3, awaitItem().categories.size)

                viewModel.onSearchQueryChanged("oo")

                val state = awaitItem()
                assertEquals("oo", state.searchQuery)
                assertEquals(
                    listOf(HomeCategoryUiModel(name = "Food", attributeCount = 2)),
                    state.categories,
                )
            }
        }

    @Test
    fun nameSortOrdersCategoriesByName() =
        runTest {
            repository.categories.value = listOf(travelCategory, foodCategory, musicCategory)

            viewModel.state.test {
                assertEquals(3, awaitItem().categories.size)

                viewModel.onSortOptionSelected(HomeSortOption.Name)

                val state = awaitItem()
                assertEquals(HomeSortOption.Name, state.sortOption)
                assertEquals(
                    listOf(
                        HomeCategoryUiModel(name = "Food", attributeCount = 2),
                        HomeCategoryUiModel(name = "Music", attributeCount = 1),
                        HomeCategoryUiModel(name = "Travel", attributeCount = 1),
                    ),
                    state.categories,
                )
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

        override suspend fun deleteCategory(name: String) {
            error("HomeViewModel does not delete categories")
        }
    }

    private companion object {
        val foodCategory = Category(name = "Food", attributes = listOf("taste", "service"))
        val musicCategory = Category(name = "Music", attributes = listOf("sound"))
        val travelCategory = Category(name = "Travel", attributes = listOf("comfort"))
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
