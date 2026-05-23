package com.juzgon.feature.category

import app.cash.turbine.test
import com.juzgon.domain.Attribute
import com.juzgon.domain.AttributeType
import com.juzgon.domain.Category
import com.juzgon.domain.ItemAttributeValue
import com.juzgon.domain.RankedRatedItem
import com.juzgon.domain.RatedItem
import com.juzgon.domain.ScoreEntry
import com.juzgon.domain.repository.CategoryRepository
import com.juzgon.domain.repository.RatedItemRepository
import com.juzgon.feature.home.MainDispatcherRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class CategoryDetailViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var categoryRepository: FakeCategoryRepository
    private lateinit var ratedItemRepository: FakeRatedItemRepository
    private lateinit var viewModel: CategoryDetailViewModel

    @Before
    fun setUp() {
        categoryRepository = FakeCategoryRepository()
        ratedItemRepository = FakeRatedItemRepository()
        viewModel = CategoryDetailViewModel(categoryRepository, ratedItemRepository)
    }

    @Test
    fun emptyCategoryMapsToEmptyState() =
        runTest {
            categoryRepository.category.value = carsCategory

            viewModel.state.test {
                assertEquals(CategoryDetailUiState(), awaitItem())

                viewModel.loadCategory("Cars")
                var state = awaitItem()
                if (state.isLoading) {
                    state = awaitItem()
                }

                assertEquals("Cars", state.categoryName)
                assertEquals("2 attributes", state.attributeSummary)
                assertEquals(emptyList<CategoryDetailItemUiModel>(), state.items)
                assertEquals(false, state.isLoading)
                assertEquals(null, state.errorMessage)
            }
        }

    @Test
    fun rankedItemsMapToUiRowsInRepositoryOrder() =
        runTest {
            categoryRepository.category.value = carsCategory
            ratedItemRepository.rankedItems.value =
                listOf(
                    RankedRatedItem(item = ratedItem("sedan"), aggregateScore = 8.74),
                    RankedRatedItem(item = ratedItem("coupe"), aggregateScore = 8.25),
                )

            viewModel.state.test {
                awaitItem()

                viewModel.loadCategory("Cars")
                var state = awaitItem()
                if (state.isLoading) {
                    state = awaitItem()
                }

                assertEquals(
                    listOf(
                        CategoryDetailItemUiModel(rank = 1, id = "sedan", averageScoreText = "8.7"),
                        CategoryDetailItemUiModel(rank = 2, id = "coupe", averageScoreText = "8.3"),
                    ),
                    state.items,
                )
            }
        }

    @Test
    fun rankedItemsResolvePrimaryImageFromFirstImageAttribute() =
        runTest {
            val photo = Attribute("Photo", type = AttributeType.IMAGE)
            val alternatePhoto = Attribute("Alternate", type = AttributeType.IMAGE)
            categoryRepository.category.value =
                Category(name = "Cars", attributes = listOf(speed, photo, alternatePhoto))
            ratedItemRepository.rankedItems.value =
                listOf(
                    RankedRatedItem(
                        item =
                            ratedItem("sedan").copy(
                                values =
                                    listOf(
                                        ItemAttributeValue(alternatePhoto, "content://images/alternate"),
                                        ItemAttributeValue(photo, "content://images/sedan"),
                                    ),
                            ),
                        aggregateScore = 8.74,
                    ),
                )

            viewModel.state.test {
                awaitItem()

                viewModel.loadCategory("Cars")
                var state = awaitItem()
                if (state.isLoading) {
                    state = awaitItem()
                }

                assertEquals("content://images/sedan", state.items.single().imageValue)
            }
        }

    @Test
    fun rankedItemsUsePlaceholderWhenImageValueIsMissing() =
        runTest {
            val photo = Attribute("Photo", type = AttributeType.IMAGE)
            categoryRepository.category.value = Category(name = "Cars", attributes = listOf(speed, photo))
            ratedItemRepository.rankedItems.value =
                listOf(
                    RankedRatedItem(item = ratedItem("sedan"), aggregateScore = 8.74),
                )

            viewModel.state.test {
                awaitItem()

                viewModel.loadCategory("Cars")
                var state = awaitItem()
                if (state.isLoading) {
                    state = awaitItem()
                }

                assertEquals(null, state.items.single().imageValue)
            }
        }

    @Test
    fun missingCategoryMapsToErrorState() =
        runTest {
            categoryRepository.category.value = null

            viewModel.state.test {
                awaitItem()

                viewModel.loadCategory("Missing")
                var state = awaitItem()
                if (state.isLoading) {
                    state = awaitItem()
                }

                assertEquals("Missing", state.categoryName)
                assertEquals("Category not found", state.errorMessage)
                assertEquals(false, state.isLoading)
            }
        }

    @Test
    fun retryReloadsCategory() =
        runTest {
            categoryRepository.category.value = null

            viewModel.state.test {
                awaitItem()

                viewModel.loadCategory("Cars")
                var errorState = awaitItem()
                if (errorState.isLoading) {
                    errorState = awaitItem()
                }
                assertEquals("Category not found", errorState.errorMessage)

                categoryRepository.category.value = carsCategory
                viewModel.onRetry()

                var loaded = awaitItem()
                if (loaded.isLoading) {
                    loaded = awaitItem()
                }
                assertEquals("Cars", loaded.categoryName)
                assertEquals(null, loaded.errorMessage)
                assertEquals(false, loaded.isLoading)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun scoreSortOrdersItemsByAggregateScoreDescending() =
        runTest {
            categoryRepository.category.value = carsCategory
            ratedItemRepository.rankedItems.value =
                listOf(
                    RankedRatedItem(item = ratedItem("coupe"), aggregateScore = 7.0),
                    RankedRatedItem(item = ratedItem("sedan"), aggregateScore = 9.0),
                )

            viewModel.state.test {
                awaitItem()

                viewModel.loadCategory("Cars")
                var state = awaitItem()
                if (state.isLoading) state = awaitItem()

                assertEquals(
                    listOf("sedan", "coupe"),
                    state.items.map { it.id },
                )
                assertEquals(listOf(1, 2), state.items.map { it.rank })
            }
        }

    @Test
    fun nameSortOrdersItemsByIdAscending() =
        runTest {
            categoryRepository.category.value = carsCategory
            ratedItemRepository.rankedItems.value =
                listOf(
                    RankedRatedItem(item = ratedItem("sedan"), aggregateScore = 9.0),
                    RankedRatedItem(item = ratedItem("coupe"), aggregateScore = 7.0),
                )

            viewModel.state.test {
                awaitItem()

                viewModel.loadCategory("Cars")
                var state = awaitItem()
                if (state.isLoading) state = awaitItem()

                viewModel.onSortOptionSelected(CategoryDetailSortOption.Name)
                state = awaitItem()

                assertEquals(
                    listOf("coupe", "sedan"),
                    state.items.map { it.id },
                )
                assertEquals(listOf(1, 2), state.items.map { it.rank })
            }
        }

    @Test
    fun scoreSortTieBreaksByNameAscending() =
        runTest {
            categoryRepository.category.value = carsCategory
            ratedItemRepository.rankedItems.value =
                listOf(
                    RankedRatedItem(item = ratedItem("sedan"), aggregateScore = 8.0),
                    RankedRatedItem(item = ratedItem("coupe"), aggregateScore = 8.0),
                )

            viewModel.state.test {
                awaitItem()

                viewModel.loadCategory("Cars")
                var state = awaitItem()
                if (state.isLoading) state = awaitItem()

                assertEquals(
                    listOf("coupe", "sedan"),
                    state.items.map { it.id },
                )
            }
        }

    @Test
    fun sortOptionChangeReordersItems() =
        runTest {
            categoryRepository.category.value = carsCategory
            ratedItemRepository.rankedItems.value =
                listOf(
                    RankedRatedItem(item = ratedItem("sedan"), aggregateScore = 9.0),
                    RankedRatedItem(item = ratedItem("coupe"), aggregateScore = 7.0),
                )

            viewModel.state.test {
                awaitItem()

                viewModel.loadCategory("Cars")
                var state = awaitItem()
                if (state.isLoading) state = awaitItem()

                assertEquals(listOf("sedan", "coupe"), state.items.map { it.id })
                assertEquals(listOf(1, 2), state.items.map { it.rank })
                assertEquals(listOf("Score", "Score"), state.items.map { it.metricLabel })
                assertEquals(listOf("9.0", "7.0"), state.items.map { it.metricValueText })
                assertEquals(CategoryDetailSortOption.Score, state.sortOption)

                viewModel.onSortOptionSelected(CategoryDetailSortOption.Name)
                state = awaitItem()

                assertEquals(listOf("coupe", "sedan"), state.items.map { it.id })
                assertEquals(listOf(1, 2), state.items.map { it.rank })
                assertEquals(listOf("Score", "Score"), state.items.map { it.metricLabel })
                assertEquals(listOf("7.0", "9.0"), state.items.map { it.metricValueText })
                assertEquals(CategoryDetailSortOption.Name, state.sortOption)
            }
        }

    @Test
    fun sortOptionsIncludeNonImageAttributesOnly() =
        runTest {
            val notes = Attribute("Notes", type = AttributeType.NOTES, isRequired = false)
            val available = Attribute("Available", type = AttributeType.BOOLEAN, isRequired = false)
            val photo = Attribute("Photo", type = AttributeType.IMAGE, isRequired = false)
            categoryRepository.category.value =
                Category(
                    name = "Cars",
                    attributes = listOf(speed, notes, available, photo),
                )

            viewModel.state.test {
                awaitItem()

                viewModel.loadCategory("Cars")
                var state = awaitItem()
                if (state.isLoading) state = awaitItem()

                assertEquals(
                    listOf(
                        "Sort items by score",
                        "Sort items by name",
                        "Sort items by Speed",
                        "Sort items by Notes",
                        "Sort items by Available",
                    ),
                    state.sortOptions.map { option -> option.contentDescription },
                )
            }
        }

    @Test
    fun numericAttributeSortOrdersDescendingAndPlacesMissingLast() =
        runTest {
            val handling = Attribute("Handling")
            categoryRepository.category.value = Category(name = "Cars", attributes = listOf(speed, handling))
            ratedItemRepository.rankedItems.value =
                listOf(
                    RankedRatedItem(
                        item =
                            RatedItem(
                                id = "sedan",
                                scores = listOf(ScoreEntry(attribute = speed, score = 5)),
                            ),
                        aggregateScore = 8.0,
                    ),
                    RankedRatedItem(
                        item =
                            RatedItem(
                                id = "coupe",
                                scores = listOf(ScoreEntry(attribute = speed, score = 9)),
                            ),
                        aggregateScore = 7.0,
                    ),
                    RankedRatedItem(
                        item =
                            RatedItem(
                                id = "wagon",
                                scores = listOf(ScoreEntry(attribute = handling, score = 8)),
                            ),
                        aggregateScore = 9.0,
                    ),
                )

            viewModel.state.test {
                awaitItem()
                viewModel.loadCategory("Cars")
                var state = awaitItem()
                if (state.isLoading) state = awaitItem()

                viewModel.onSortOptionSelected(CategoryDetailSortOption.Attribute(speed.id))
                state = awaitItem()

                assertEquals(listOf("coupe", "sedan", "wagon"), state.items.map { item -> item.id })
                assertEquals(listOf(1, 2, 3), state.items.map { item -> item.rank })
                assertEquals(listOf("Speed", "Speed", "Speed"), state.items.map { item -> item.metricLabel })
                assertEquals(listOf("9", "5", "Not rated"), state.items.map { item -> item.metricValueText })
            }
        }

    @Test
    fun textAttributeSortOrdersByValueAndTieBreaksByName() =
        runTest {
            val notes = Attribute("Notes", type = AttributeType.NOTES, isRequired = false)
            categoryRepository.category.value = Category(name = "Cars", attributes = listOf(speed, notes))
            ratedItemRepository.rankedItems.value =
                listOf(
                    RankedRatedItem(
                        item =
                            ratedItem("sedan").copy(
                                values = listOf(ItemAttributeValue(notes, "zeta")),
                            ),
                        aggregateScore = 9.0,
                    ),
                    RankedRatedItem(
                        item =
                            ratedItem("coupe").copy(
                                values = listOf(ItemAttributeValue(notes, "alpha")),
                            ),
                        aggregateScore = 8.0,
                    ),
                    RankedRatedItem(
                        item =
                            ratedItem("wagon").copy(
                                values = listOf(ItemAttributeValue(notes, "alpha")),
                            ),
                        aggregateScore = 7.0,
                    ),
                    RankedRatedItem(
                        item = ratedItem("roadster"),
                        aggregateScore = 6.0,
                    ),
                )

            viewModel.state.test {
                awaitItem()
                viewModel.loadCategory("Cars")
                var state = awaitItem()
                if (state.isLoading) state = awaitItem()

                viewModel.onSortOptionSelected(CategoryDetailSortOption.Attribute(notes.id))
                state = awaitItem()

                assertEquals(listOf("coupe", "wagon", "sedan", "roadster"), state.items.map { item -> item.id })
                assertEquals(listOf(1, 2, 3, 4), state.items.map { item -> item.rank })
                assertEquals(listOf("Notes", "Notes", "Notes", "Notes"), state.items.map { item -> item.metricLabel })
                assertEquals(
                    listOf("alpha", "alpha", "zeta", "Not rated"),
                    state.items.map { item -> item.metricValueText },
                )
            }
        }

    @Test
    fun deleteCategoryEmitsNavigateBackEventOnSuccess() =
        runTest {
            categoryRepository.category.value = carsCategory

            viewModel.navigationEvents.test {
                viewModel.loadCategory("Cars")
                var state = viewModel.state.value
                if (state.isLoading) {
                    state = viewModel.state.first { !it.isLoading }
                }

                viewModel.onDeleteClick()
                viewModel.onDeleteConfirmed()

                assertEquals(CategoryDetailNavigationEvent.NavigateBack, awaitItem())
            }
        }

    @Test
    fun deleteClickWithNoItemsShowsConfirmDialog() =
        runTest {
            categoryRepository.category.value = carsCategory

            viewModel.state.test {
                awaitItem()
                viewModel.loadCategory("Cars")
                var state = awaitItem()
                if (state.isLoading) state = awaitItem()

                viewModel.onDeleteClick()
                state = awaitItem()

                assertEquals(true, state.showDeleteConfirmDialog)
                assertEquals(false, state.showDeleteWithItemsWarning)
            }
        }

    @Test
    fun deleteClickWithItemsShowsWarningDialog() =
        runTest {
            categoryRepository.category.value = carsCategory
            ratedItemRepository.rankedItems.value =
                listOf(
                    RankedRatedItem(item = ratedItem("sedan"), aggregateScore = 8.0),
                )

            viewModel.state.test {
                awaitItem()
                viewModel.loadCategory("Cars")
                var state = awaitItem()
                if (state.isLoading) state = awaitItem()

                viewModel.onDeleteClick()
                state = awaitItem()

                assertEquals(false, state.showDeleteConfirmDialog)
                assertEquals(true, state.showDeleteWithItemsWarning)
            }
        }

    @Test
    fun deleteDialogDismissedClearsDialogState() =
        runTest {
            categoryRepository.category.value = carsCategory

            viewModel.state.test {
                awaitItem()
                viewModel.loadCategory("Cars")
                var state = awaitItem()
                if (state.isLoading) state = awaitItem()

                viewModel.onDeleteClick()
                state = awaitItem()
                assertEquals(true, state.showDeleteConfirmDialog)

                viewModel.onDeleteDialogDismissed()
                state = awaitItem()

                assertEquals(false, state.showDeleteConfirmDialog)
                assertEquals(false, state.showDeleteWithItemsWarning)
            }
        }

    @Test
    fun editCategoryClickEmitsNavigateToEditCategoryEvent() =
        runTest {
            categoryRepository.category.value = carsCategory

            viewModel.navigationEvents.test {
                viewModel.loadCategory("Cars")
                viewModel.onEditCategoryClick()

                assertEquals(CategoryDetailNavigationEvent.NavigateToEditCategory, awaitItem())
            }
        }

    private class FakeCategoryRepository : CategoryRepository {
        val category = MutableStateFlow<Category?>(null)

        override fun observeCategories(): Flow<List<Category>> {
            error("CategoryDetailViewModel does not observe all categories")
        }

        override fun observeCategory(name: String): Flow<Category?> = category

        override suspend fun saveCategory(category: Category) {
            error("CategoryDetailViewModel does not save categories")
        }

        override suspend fun renameCategory(
            originalName: String,
            category: Category,
            renamedAttributeIds: Map<String, String>,
        ) {
            error("CategoryDetailViewModel does not rename categories")
        }

        override suspend fun deleteCategory(name: String) {
            category.value = null
        }
    }

    private class FakeRatedItemRepository : RatedItemRepository {
        val rankedItems = MutableStateFlow(emptyList<RankedRatedItem>())

        override fun observeRatedItems(): Flow<List<RatedItem>> {
            error("CategoryDetailViewModel does not observe all rated items")
        }

        override fun observeRatedItem(id: String): Flow<RatedItem?> {
            error("CategoryDetailViewModel does not observe one rated item")
        }

        override fun observeRankedItems(categoryName: String): Flow<List<RankedRatedItem>> = rankedItems

        override suspend fun saveRatedItem(ratedItem: RatedItem) {
            error("CategoryDetailViewModel does not save rated items")
        }

        override suspend fun renameRatedItem(
            originalId: String,
            ratedItem: RatedItem,
        ) {
            error("CategoryDetailViewModel does not rename rated items")
        }

        override suspend fun deleteRatedItem(id: String) {
            error("CategoryDetailViewModel does not delete rated items")
        }
    }

    private companion object {
        val speed = Attribute("Speed")
        val brakes = Attribute("Brakes")
        val carsCategory = Category(name = "Cars", attributes = listOf(speed, brakes))

        fun ratedItem(id: String): RatedItem =
            RatedItem(
                id = id,
                scores = listOf(ScoreEntry(attribute = speed, score = 8)),
            )
    }
}
