package com.juzgon.feature.category

import app.cash.turbine.test
import com.juzgon.domain.Attribute
import com.juzgon.domain.AttributeType
import com.juzgon.domain.Category
import com.juzgon.domain.ItemAttributeValue
import com.juzgon.domain.RankedRatedItem
import com.juzgon.domain.RatedItem
import com.juzgon.domain.ScoreEntry
import com.juzgon.domain.ScoreProfile
import com.juzgon.domain.repository.CategoryRepository
import com.juzgon.domain.repository.RatedItemRepository
import com.juzgon.domain.repository.ScoreProfileRepository
import com.juzgon.domain.usecase.CalculateProfileRankedItemsUseCase
import com.juzgon.feature.home.MainDispatcherRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@Suppress("LargeClass")
class CategoryDetailViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var categoryRepository: FakeCategoryRepository
    private lateinit var ratedItemRepository: FakeRatedItemRepository
    private lateinit var scoreProfileRepository: FakeScoreProfileRepository
    private lateinit var calculateProfileRankedItems: CalculateProfileRankedItemsUseCase
    private lateinit var viewModel: CategoryDetailViewModel

    @Before
    fun setUp() {
        categoryRepository = FakeCategoryRepository()
        ratedItemRepository = FakeRatedItemRepository()
        scoreProfileRepository = FakeScoreProfileRepository()
        calculateProfileRankedItems = CalculateProfileRankedItemsUseCase()
        viewModel =
            CategoryDetailViewModel(
                categoryRepository,
                ratedItemRepository,
                scoreProfileRepository,
                calculateProfileRankedItems,
            )
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
    fun sortOptionsIncludeNationalityAttribute() =
        runTest {
            val nationality = Attribute("Nationality", type = AttributeType.NATIONALITY, isRequired = false)
            categoryRepository.category.value =
                Category(
                    name = "Cars",
                    attributes = listOf(speed, nationality),
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
                        "Sort items by Nationality",
                    ),
                    state.sortOptions.map { option -> option.contentDescription },
                )
            }
        }

    @Test
    fun nationalityBadgeResolvedFromItemValue() =
        runTest {
            val nationality = Attribute("Nationality", type = AttributeType.NATIONALITY, isRequired = false)
            categoryRepository.category.value =
                Category(name = "Cars", attributes = listOf(speed, nationality))
            ratedItemRepository.rankedItems.value =
                listOf(
                    RankedRatedItem(
                        item =
                            ratedItem("sedan").copy(
                                values = listOf(ItemAttributeValue(nationality, "MX")),
                            ),
                        aggregateScore = 8.0,
                    ),
                )

            viewModel.state.test {
                awaitItem()
                viewModel.loadCategory("Cars")
                var state = awaitItem()
                if (state.isLoading) state = awaitItem()

                assertEquals("\uD83C\uDDF2\uD83C\uDDFD", state.items.single().nationalityBadge)
            }
        }

    @Test
    fun nationalityBadgeNullWhenValueMissing() =
        runTest {
            val nationality = Attribute("Nationality", type = AttributeType.NATIONALITY, isRequired = false)
            categoryRepository.category.value =
                Category(name = "Cars", attributes = listOf(speed, nationality))
            ratedItemRepository.rankedItems.value =
                listOf(
                    RankedRatedItem(item = ratedItem("sedan"), aggregateScore = 8.0),
                )

            viewModel.state.test {
                awaitItem()
                viewModel.loadCategory("Cars")
                var state = awaitItem()
                if (state.isLoading) state = awaitItem()

                assertEquals(null, state.items.single().nationalityBadge)
            }
        }

    @Test
    fun nationalityBadgeNullForUnknownCode() =
        runTest {
            val nationality = Attribute("Nationality", type = AttributeType.NATIONALITY, isRequired = false)
            categoryRepository.category.value =
                Category(name = "Cars", attributes = listOf(speed, nationality))
            ratedItemRepository.rankedItems.value =
                listOf(
                    RankedRatedItem(
                        item =
                            ratedItem("sedan").copy(
                                values = listOf(ItemAttributeValue(nationality, "ZZZZZ")),
                            ),
                        aggregateScore = 8.0,
                    ),
                )

            viewModel.state.test {
                awaitItem()
                viewModel.loadCategory("Cars")
                var state = awaitItem()
                if (state.isLoading) state = awaitItem()

                assertEquals(null, state.items.single().nationalityBadge)
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

    @Test
    fun profileSelectionReranksItems() =
        runTest {
            categoryRepository.category.value = carsCategory
            ratedItemRepository.rankedItems.value =
                listOf(
                    RankedRatedItem(
                        item =
                            RatedItem(
                                id = "sedan",
                                scores =
                                    listOf(
                                        ScoreEntry(attribute = speed, score = 9),
                                        ScoreEntry(attribute = brakes, score = 3),
                                    ),
                            ),
                        aggregateScore = 6.0,
                    ),
                    RankedRatedItem(
                        item =
                            RatedItem(
                                id = "coupe",
                                scores =
                                    listOf(
                                        ScoreEntry(attribute = speed, score = 5),
                                        ScoreEntry(attribute = brakes, score = 10),
                                    ),
                            ),
                        aggregateScore = 7.5,
                    ),
                )
            scoreProfileRepository.profiles.value =
                listOf(
                    ScoreProfile(
                        id = "p1",
                        categoryName = "Cars",
                        name = "Brakes Only",
                        includedAttributeIds = listOf("Brakes"),
                    ),
                )

            viewModel.state.test {
                awaitItem()
                viewModel.loadCategory("Cars")
                var state = awaitItem()
                if (state.isLoading) state = awaitItem()

                assertEquals(listOf("coupe", "sedan"), state.items.map { it.id })

                viewModel.onProfileSelected("p1")
                state = awaitItem()

                assertEquals(listOf("coupe", "sedan"), state.items.map { it.id })
                assertEquals("10.0", state.items[0].averageScoreText)
                assertEquals("3.0", state.items[1].averageScoreText)
            }
        }

    @Test
    fun defaultProfileRestoresOriginalRanking() =
        runTest {
            categoryRepository.category.value = carsCategory
            ratedItemRepository.rankedItems.value =
                listOf(
                    RankedRatedItem(
                        item =
                            RatedItem(
                                id = "sedan",
                                scores =
                                    listOf(
                                        ScoreEntry(attribute = speed, score = 9),
                                        ScoreEntry(attribute = brakes, score = 3),
                                    ),
                            ),
                        aggregateScore = 6.0,
                    ),
                    RankedRatedItem(
                        item =
                            RatedItem(
                                id = "coupe",
                                scores =
                                    listOf(
                                        ScoreEntry(attribute = speed, score = 5),
                                        ScoreEntry(attribute = brakes, score = 10),
                                    ),
                            ),
                        aggregateScore = 7.5,
                    ),
                )
            scoreProfileRepository.profiles.value =
                listOf(
                    ScoreProfile(
                        id = "p1",
                        categoryName = "Cars",
                        name = "Brakes Only",
                        includedAttributeIds = listOf("Brakes"),
                    ),
                )

            viewModel.state.test {
                awaitItem()
                viewModel.loadCategory("Cars")
                var state = awaitItem()
                if (state.isLoading) state = awaitItem()

                viewModel.onProfileSelected("p1")
                state = awaitItem()

                viewModel.onProfileSelected(null)
                state = awaitItem()

                assertEquals(listOf("coupe", "sedan"), state.items.map { it.id })
                assertEquals("7.5", state.items[0].averageScoreText)
                assertEquals("6.0", state.items[1].averageScoreText)
            }
        }

    @Test
    fun profilesListExposedInState() =
        runTest {
            categoryRepository.category.value = carsCategory
            scoreProfileRepository.profiles.value =
                listOf(
                    ScoreProfile(
                        id = "p1",
                        categoryName = "Cars",
                        name = "Speed Focus",
                        includedAttributeIds = listOf("Speed"),
                    ),
                    ScoreProfile(
                        id = "p2",
                        categoryName = "Cars",
                        name = "Brakes Only",
                        includedAttributeIds = listOf("Brakes"),
                    ),
                )

            viewModel.state.test {
                awaitItem()
                viewModel.loadCategory("Cars")
                var state = awaitItem()
                if (state.isLoading) state = awaitItem()

                assertEquals(
                    listOf(
                        ProfileOption(id = null, name = "All Attributes"),
                        ProfileOption(id = "p1", name = "Speed Focus"),
                        ProfileOption(id = "p2", name = "Brakes Only"),
                    ),
                    state.profiles,
                )
            }
        }

    @Test
    fun activeProfileLabelShownWhenSelected() =
        runTest {
            categoryRepository.category.value = carsCategory
            scoreProfileRepository.profiles.value =
                listOf(
                    ScoreProfile(
                        id = "p1",
                        categoryName = "Cars",
                        name = "Brakes Only",
                        includedAttributeIds = listOf("Brakes"),
                    ),
                )

            viewModel.state.test {
                awaitItem()
                viewModel.loadCategory("Cars")
                var state = awaitItem()
                if (state.isLoading) state = awaitItem()

                viewModel.onProfileSelected("p1")
                state = awaitItem()

                assertEquals("Ranking: Brakes Only", state.activeProfileLabel)
                assertEquals("p1", state.activeProfileId)
            }
        }

    @Test
    fun activeProfileLabelNullForDefault() =
        runTest {
            categoryRepository.category.value = carsCategory
            scoreProfileRepository.profiles.value =
                listOf(
                    ScoreProfile(
                        id = "p1",
                        categoryName = "Cars",
                        name = "Brakes Only",
                        includedAttributeIds = listOf("Brakes"),
                    ),
                )

            viewModel.state.test {
                awaitItem()
                viewModel.loadCategory("Cars")
                var state = awaitItem()
                if (state.isLoading) state = awaitItem()

                assertEquals(null, state.activeProfileLabel)
                assertEquals(null, state.activeProfileId)
            }
        }

    @Test
    fun searchFiltersItemsByNameCaseInsensitive() =
        runTest {
            categoryRepository.category.value = carsCategory
            ratedItemRepository.rankedItems.value =
                listOf(
                    RankedRatedItem(item = ratedItem("sedan"), aggregateScore = 8.74),
                    RankedRatedItem(item = ratedItem("COUPE"), aggregateScore = 8.25),
                    RankedRatedItem(item = ratedItem("Truck"), aggregateScore = 7.5),
                )

            viewModel.state.test {
                awaitItem()
                viewModel.loadCategory("Cars")
                var state = awaitItem()
                if (state.isLoading) state = awaitItem()

                assertEquals(3, state.items.size)

                viewModel.onSearchQueryChanged("coup")
                state = awaitItem()

                assertEquals(1, state.items.size)
                assertEquals("COUPE", state.items.single().id)
            }
        }

    @Test
    fun searchTrimsWhitespaceFromQuery() =
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
                if (state.isLoading) state = awaitItem()

                viewModel.onSearchQueryChanged("  sedan  ")
                state = awaitItem()

                assertEquals(1, state.items.size)
                assertEquals("sedan", state.items.single().id)
            }
        }

    @Test
    fun emptySearchReturnsFullList() =
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
                if (state.isLoading) state = awaitItem()

                viewModel.onSearchQueryChanged("sedan")
                state = awaitItem()
                assertEquals(1, state.items.size)

                viewModel.onSearchQueryChanged("")
                state = awaitItem()
                assertEquals(2, state.items.size)
            }
        }

    @Test
    fun searchAppliesBeforeProfileRankingAndSort() =
        runTest {
            categoryRepository.category.value = carsCategory
            ratedItemRepository.rankedItems.value =
                listOf(
                    RankedRatedItem(
                        item =
                            RatedItem(
                                id = "sedan",
                                scores = listOf(ScoreEntry(attribute = speed, score = 9)),
                            ),
                        aggregateScore = 9.0,
                    ),
                    RankedRatedItem(
                        item =
                            RatedItem(
                                id = "coupe",
                                scores = listOf(ScoreEntry(attribute = speed, score = 5)),
                            ),
                        aggregateScore = 5.0,
                    ),
                    RankedRatedItem(
                        item =
                            RatedItem(
                                id = "truck",
                                scores = listOf(ScoreEntry(attribute = speed, score = 3)),
                            ),
                        aggregateScore = 3.0,
                    ),
                )
            scoreProfileRepository.profiles.value =
                listOf(
                    ScoreProfile(
                        id = "p1",
                        categoryName = "Cars",
                        name = "Speed Focus",
                        includedAttributeIds = listOf("Speed"),
                    ),
                )

            viewModel.state.test {
                awaitItem()
                viewModel.loadCategory("Cars")
                var state = awaitItem()
                if (state.isLoading) state = awaitItem()

                viewModel.onSearchQueryChanged("coupe")
                state = awaitItem()

                assertEquals(1, state.items.size)
                assertEquals("coupe", state.items.single().id)
            }
        }

    @Test
    fun searchPreservesProfileRankingAndSort() =
        runTest {
            categoryRepository.category.value = carsCategory
            ratedItemRepository.rankedItems.value =
                listOf(
                    RankedRatedItem(item = ratedItem("sedan"), aggregateScore = 8.74),
                    RankedRatedItem(item = ratedItem("coupe"), aggregateScore = 8.25),
                    RankedRatedItem(item = ratedItem("truck"), aggregateScore = 7.5),
                )

            viewModel.state.test {
                awaitItem()
                viewModel.loadCategory("Cars")
                var state = awaitItem()
                if (state.isLoading) state = awaitItem()

                assertEquals(
                    listOf("sedan", "coupe", "truck"),
                    state.items.map { it.id },
                )

                viewModel.onSearchQueryChanged("sedan")
                state = awaitItem()

                assertEquals(listOf("sedan"), state.items.map { it.id })
                assertEquals("8.7", state.items.single().averageScoreText)
            }
        }

    @Test
    fun onSearchQueryChangedUpdatesState() =
        runTest {
            categoryRepository.category.value = carsCategory
            ratedItemRepository.rankedItems.value =
                listOf(
                    RankedRatedItem(item = ratedItem("sedan"), aggregateScore = 8.74),
                )

            viewModel.state.test {
                awaitItem()
                viewModel.loadCategory("Cars")
                var state = awaitItem()
                if (state.isLoading) state = awaitItem()

                viewModel.onSearchQueryChanged("test")
                state = awaitItem()

                assertEquals("test", state.searchQuery)
            }
        }

    @Test
    fun searchMatchesItemByNationalityValue() =
        runTest {
            val nationalityAttr = Attribute("Cars/Nationality", type = AttributeType.NATIONALITY)
            categoryRepository.category.value =
                Category(name = "Cars", attributes = listOf(speed, brakes, nationalityAttr))
            ratedItemRepository.rankedItems.value =
                listOf(
                    RankedRatedItem(
                        item =
                            RatedItem(
                                id = "sedan",
                                scores = listOf(ScoreEntry(speed, 8)),
                                values = listOf(ItemAttributeValue(nationalityAttr, "MX")),
                            ),
                        aggregateScore = 8.0,
                    ),
                    RankedRatedItem(
                        item =
                            RatedItem(
                                id = "coupe",
                                scores = listOf(ScoreEntry(speed, 7)),
                                values = listOf(ItemAttributeValue(nationalityAttr, "US")),
                            ),
                        aggregateScore = 7.0,
                    ),
                )

            viewModel.state.test {
                awaitItem()
                viewModel.loadCategory("Cars")
                var state = awaitItem()
                if (state.isLoading) state = awaitItem()

                viewModel.onSearchQueryChanged("MX")
                state = awaitItem()

                assertEquals(1, state.items.size)
                assertEquals("sedan", state.items.single().id)
            }
        }

    @Test
    fun searchMatchesItemByNotesContent() =
        runTest {
            categoryRepository.category.value = carsCategory
            ratedItemRepository.rankedItems.value =
                listOf(
                    RankedRatedItem(
                        item =
                            RatedItem(
                                id = "sedan",
                                scores = listOf(ScoreEntry(speed, 8)),
                                notes = "Great fuel economy",
                            ),
                        aggregateScore = 8.0,
                    ),
                    RankedRatedItem(
                        item =
                            RatedItem(
                                id = "truck",
                                scores = listOf(ScoreEntry(speed, 6)),
                                notes = "Good for hauling",
                            ),
                        aggregateScore = 6.0,
                    ),
                )

            viewModel.state.test {
                awaitItem()
                viewModel.loadCategory("Cars")
                var state = awaitItem()
                if (state.isLoading) state = awaitItem()

                viewModel.onSearchQueryChanged("fuel")
                state = awaitItem()

                assertEquals(1, state.items.size)
                assertEquals("sedan", state.items.single().id)
            }
        }

    @Test
    fun searchMatchesItemByDropdownValue() =
        runTest {
            val colorAttr = Attribute("Cars/Color", type = AttributeType.DROPDOWN)
            categoryRepository.category.value =
                Category(name = "Cars", attributes = listOf(speed, colorAttr))
            ratedItemRepository.rankedItems.value =
                listOf(
                    RankedRatedItem(
                        item =
                            RatedItem(
                                id = "sedan",
                                scores = listOf(ScoreEntry(speed, 8)),
                                values = listOf(ItemAttributeValue(colorAttr, "Red")),
                            ),
                        aggregateScore = 8.0,
                    ),
                    RankedRatedItem(
                        item =
                            RatedItem(
                                id = "coupe",
                                scores = listOf(ScoreEntry(speed, 7)),
                                values = listOf(ItemAttributeValue(colorAttr, "Blue")),
                            ),
                        aggregateScore = 7.0,
                    ),
                )

            viewModel.state.test {
                awaitItem()
                viewModel.loadCategory("Cars")
                var state = awaitItem()
                if (state.isLoading) state = awaitItem()

                viewModel.onSearchQueryChanged("Red")
                state = awaitItem()

                assertEquals(1, state.items.size)
                assertEquals("sedan", state.items.single().id)
            }
        }

    @Test
    fun searchDoesNotMatchNumberScoreAsText() =
        runTest {
            categoryRepository.category.value = carsCategory
            ratedItemRepository.rankedItems.value =
                listOf(
                    RankedRatedItem(
                        item =
                            RatedItem(
                                id = "sedan",
                                scores = listOf(ScoreEntry(speed, 8)),
                            ),
                        aggregateScore = 8.0,
                    ),
                )

            viewModel.state.test {
                awaitItem()
                viewModel.loadCategory("Cars")
                var state = awaitItem()
                if (state.isLoading) state = awaitItem()

                viewModel.onSearchQueryChanged("8")
                state = awaitItem()

                assertEquals(0, state.items.size)
            }
        }

    @Test
    fun nationalityFilterReturnsOnlyMatchingItems() =
        runTest {
            val nationalityAttr = Attribute("Cars/Nationality", type = AttributeType.NATIONALITY)
            categoryRepository.category.value =
                Category(name = "Cars", attributes = listOf(speed, nationalityAttr))
            ratedItemRepository.rankedItems.value =
                listOf(
                    RankedRatedItem(
                        item =
                            RatedItem(
                                id = "sedan",
                                scores = listOf(ScoreEntry(speed, 8)),
                                values = listOf(ItemAttributeValue(nationalityAttr, "MX")),
                            ),
                        aggregateScore = 8.0,
                    ),
                    RankedRatedItem(
                        item =
                            RatedItem(
                                id = "coupe",
                                scores = listOf(ScoreEntry(speed, 7)),
                                values = listOf(ItemAttributeValue(nationalityAttr, "US")),
                            ),
                        aggregateScore = 7.0,
                    ),
                    RankedRatedItem(
                        item =
                            RatedItem(
                                id = "suv",
                                scores = listOf(ScoreEntry(speed, 6)),
                                values = listOf(ItemAttributeValue(nationalityAttr, "DE")),
                            ),
                        aggregateScore = 6.0,
                    ),
                )

            viewModel.state.test {
                awaitItem()
                viewModel.loadCategory("Cars")
                var state = awaitItem()
                if (state.isLoading) state = awaitItem()

                viewModel.onFilterSelected(
                    AttributeFilter.Nationality("Cars/Nationality", setOf("MX")),
                )
                state = awaitItem()

                assertEquals(1, state.items.size)
                assertEquals("sedan", state.items.single().id)
            }
        }

    @Test
    fun numberRangeFilterExcludesOutOfRange() =
        runTest {
            categoryRepository.category.value = carsCategory
            ratedItemRepository.rankedItems.value =
                listOf(
                    RankedRatedItem(
                        item = RatedItem(id = "slow", scores = listOf(ScoreEntry(speed, 3))),
                        aggregateScore = 3.0,
                    ),
                    RankedRatedItem(
                        item = RatedItem(id = "medium", scores = listOf(ScoreEntry(speed, 7))),
                        aggregateScore = 7.0,
                    ),
                    RankedRatedItem(
                        item = RatedItem(id = "fast", scores = listOf(ScoreEntry(speed, 10))),
                        aggregateScore = 10.0,
                    ),
                )

            viewModel.state.test {
                awaitItem()
                viewModel.loadCategory("Cars")
                var state = awaitItem()
                if (state.isLoading) state = awaitItem()

                viewModel.onFilterSelected(
                    AttributeFilter.NumberRange("Speed", min = 5, max = 8),
                )
                state = awaitItem()

                assertEquals(1, state.items.size)
                assertEquals("medium", state.items.single().id)
            }
        }

    @Test
    fun dropdownFilterReturnsOnlyMatchingValues() =
        runTest {
            val colorAttr = Attribute("Cars/Color", type = AttributeType.DROPDOWN)
            categoryRepository.category.value =
                Category(name = "Cars", attributes = listOf(speed, colorAttr))
            ratedItemRepository.rankedItems.value =
                listOf(
                    RankedRatedItem(
                        item =
                            RatedItem(
                                id = "sedan",
                                scores = listOf(ScoreEntry(speed, 8)),
                                values = listOf(ItemAttributeValue(colorAttr, "Red")),
                            ),
                        aggregateScore = 8.0,
                    ),
                    RankedRatedItem(
                        item =
                            RatedItem(
                                id = "coupe",
                                scores = listOf(ScoreEntry(speed, 7)),
                                values = listOf(ItemAttributeValue(colorAttr, "Blue")),
                            ),
                        aggregateScore = 7.0,
                    ),
                    RankedRatedItem(
                        item =
                            RatedItem(
                                id = "suv",
                                scores = listOf(ScoreEntry(speed, 6)),
                                values = listOf(ItemAttributeValue(colorAttr, "Red")),
                            ),
                        aggregateScore = 6.0,
                    ),
                )

            viewModel.state.test {
                awaitItem()
                viewModel.loadCategory("Cars")
                var state = awaitItem()
                if (state.isLoading) state = awaitItem()

                viewModel.onFilterSelected(
                    AttributeFilter.Dropdown("Cars/Color", setOf("Red")),
                )
                state = awaitItem()

                assertEquals(2, state.items.size)
                assertEquals(setOf("sedan", "suv"), state.items.map { it.id }.toSet())
            }
        }

    @Test
    fun dateRangeFilterExcludesOutsideRange() =
        runTest {
            val dateAttr = Attribute("Cars/Release", type = AttributeType.DATE)
            categoryRepository.category.value =
                Category(name = "Cars", attributes = listOf(speed, dateAttr))
            ratedItemRepository.rankedItems.value =
                listOf(
                    RankedRatedItem(
                        item =
                            RatedItem(
                                id = "old",
                                scores = listOf(ScoreEntry(speed, 7)),
                                values = listOf(ItemAttributeValue(dateAttr, "2018-03-15")),
                            ),
                        aggregateScore = 7.0,
                    ),
                    RankedRatedItem(
                        item =
                            RatedItem(
                                id = "mid",
                                scores = listOf(ScoreEntry(speed, 8)),
                                values = listOf(ItemAttributeValue(dateAttr, "2021-06-01")),
                            ),
                        aggregateScore = 8.0,
                    ),
                    RankedRatedItem(
                        item =
                            RatedItem(
                                id = "new",
                                scores = listOf(ScoreEntry(speed, 9)),
                                values = listOf(ItemAttributeValue(dateAttr, "2024-01-10")),
                            ),
                        aggregateScore = 9.0,
                    ),
                )

            viewModel.state.test {
                awaitItem()
                viewModel.loadCategory("Cars")
                var state = awaitItem()
                if (state.isLoading) state = awaitItem()

                viewModel.onFilterSelected(
                    AttributeFilter.DateRange("Cars/Release", startDate = "2020-01-01", endDate = "2022-12-31"),
                )
                state = awaitItem()

                assertEquals(1, state.items.size)
                assertEquals("mid", state.items.single().id)
            }
        }

    @Test
    fun booleanFilterReturnsOnlyMatchingValue() =
        runTest {
            val activeAttr = Attribute("Cars/Active", type = AttributeType.BOOLEAN)
            categoryRepository.category.value =
                Category(name = "Cars", attributes = listOf(speed, activeAttr))
            ratedItemRepository.rankedItems.value =
                listOf(
                    RankedRatedItem(
                        item =
                            RatedItem(
                                id = "sedan",
                                scores = listOf(ScoreEntry(speed, 8)),
                                values = listOf(ItemAttributeValue(activeAttr, "true")),
                            ),
                        aggregateScore = 8.0,
                    ),
                    RankedRatedItem(
                        item =
                            RatedItem(
                                id = "coupe",
                                scores = listOf(ScoreEntry(speed, 7)),
                                values = listOf(ItemAttributeValue(activeAttr, "false")),
                            ),
                        aggregateScore = 7.0,
                    ),
                )

            viewModel.state.test {
                awaitItem()
                viewModel.loadCategory("Cars")
                var state = awaitItem()
                if (state.isLoading) state = awaitItem()

                viewModel.onFilterSelected(
                    AttributeFilter.BooleanFilter("Cars/Active", value = true),
                )
                state = awaitItem()

                assertEquals(1, state.items.size)
                assertEquals("sedan", state.items.single().id)
            }
        }

    @Test
    fun multipleFiltersApplyAndLogic() =
        runTest {
            val nationalityAttr = Attribute("Cars/Nationality", type = AttributeType.NATIONALITY)
            categoryRepository.category.value =
                Category(name = "Cars", attributes = listOf(speed, nationalityAttr))
            ratedItemRepository.rankedItems.value =
                listOf(
                    RankedRatedItem(
                        item =
                            RatedItem(
                                id = "fastMX",
                                scores = listOf(ScoreEntry(speed, 9)),
                                values = listOf(ItemAttributeValue(nationalityAttr, "MX")),
                            ),
                        aggregateScore = 9.0,
                    ),
                    RankedRatedItem(
                        item =
                            RatedItem(
                                id = "slowMX",
                                scores = listOf(ScoreEntry(speed, 3)),
                                values = listOf(ItemAttributeValue(nationalityAttr, "MX")),
                            ),
                        aggregateScore = 3.0,
                    ),
                    RankedRatedItem(
                        item =
                            RatedItem(
                                id = "fastUS",
                                scores = listOf(ScoreEntry(speed, 9)),
                                values = listOf(ItemAttributeValue(nationalityAttr, "US")),
                            ),
                        aggregateScore = 9.0,
                    ),
                )

            viewModel.state.test {
                awaitItem()
                viewModel.loadCategory("Cars")
                var state = awaitItem()
                if (state.isLoading) state = awaitItem()

                viewModel.onFilterSelected(
                    AttributeFilter.Nationality("Cars/Nationality", setOf("MX")),
                )
                state = awaitItem()
                viewModel.onFilterSelected(
                    AttributeFilter.NumberRange("Speed", min = 7, max = 10),
                )
                state = awaitItem()

                assertEquals(1, state.items.size)
                assertEquals("fastMX", state.items.single().id)
            }
        }

    @Test
    fun filtersComposeWithTextSearch() =
        runTest {
            val nationalityAttr = Attribute("Cars/Nationality", type = AttributeType.NATIONALITY)
            categoryRepository.category.value =
                Category(name = "Cars", attributes = listOf(speed, nationalityAttr))
            ratedItemRepository.rankedItems.value =
                listOf(
                    RankedRatedItem(
                        item =
                            RatedItem(
                                id = "sedan-mx",
                                scores = listOf(ScoreEntry(speed, 8)),
                                values = listOf(ItemAttributeValue(nationalityAttr, "MX")),
                            ),
                        aggregateScore = 8.0,
                    ),
                    RankedRatedItem(
                        item =
                            RatedItem(
                                id = "coupe-mx",
                                scores = listOf(ScoreEntry(speed, 7)),
                                values = listOf(ItemAttributeValue(nationalityAttr, "MX")),
                            ),
                        aggregateScore = 7.0,
                    ),
                    RankedRatedItem(
                        item =
                            RatedItem(
                                id = "sedan-us",
                                scores = listOf(ScoreEntry(speed, 9)),
                                values = listOf(ItemAttributeValue(nationalityAttr, "US")),
                            ),
                        aggregateScore = 9.0,
                    ),
                )

            viewModel.state.test {
                awaitItem()
                viewModel.loadCategory("Cars")
                var state = awaitItem()
                if (state.isLoading) state = awaitItem()

                viewModel.onSearchQueryChanged("sedan")
                state = awaitItem()
                viewModel.onFilterSelected(
                    AttributeFilter.Nationality("Cars/Nationality", setOf("MX")),
                )
                state = awaitItem()

                assertEquals(1, state.items.size)
                assertEquals("sedan-mx", state.items.single().id)
            }
        }

    @Test
    fun onFilterClearedRestoresItems() =
        runTest {
            val nationalityAttr = Attribute("Cars/Nationality", type = AttributeType.NATIONALITY)
            categoryRepository.category.value =
                Category(name = "Cars", attributes = listOf(speed, nationalityAttr))
            ratedItemRepository.rankedItems.value =
                listOf(
                    RankedRatedItem(
                        item =
                            RatedItem(
                                id = "sedan",
                                scores = listOf(ScoreEntry(speed, 8)),
                                values = listOf(ItemAttributeValue(nationalityAttr, "MX")),
                            ),
                        aggregateScore = 8.0,
                    ),
                    RankedRatedItem(
                        item =
                            RatedItem(
                                id = "coupe",
                                scores = listOf(ScoreEntry(speed, 7)),
                                values = listOf(ItemAttributeValue(nationalityAttr, "US")),
                            ),
                        aggregateScore = 7.0,
                    ),
                )

            viewModel.state.test {
                awaitItem()
                viewModel.loadCategory("Cars")
                var state = awaitItem()
                if (state.isLoading) state = awaitItem()

                viewModel.onFilterSelected(
                    AttributeFilter.Nationality("Cars/Nationality", setOf("MX")),
                )
                state = awaitItem()
                assertEquals(1, state.items.size)

                viewModel.onFilterCleared("Cars/Nationality")
                state = awaitItem()
                assertEquals(2, state.items.size)
            }
        }

    @Test
    fun onFilterSelectedReplacesExistingFilterForSameAttribute() =
        runTest {
            val nationalityAttr = Attribute("Cars/Nationality", type = AttributeType.NATIONALITY)
            categoryRepository.category.value =
                Category(name = "Cars", attributes = listOf(speed, nationalityAttr))
            ratedItemRepository.rankedItems.value =
                listOf(
                    RankedRatedItem(
                        item =
                            RatedItem(
                                id = "sedan",
                                scores = listOf(ScoreEntry(speed, 8)),
                                values = listOf(ItemAttributeValue(nationalityAttr, "MX")),
                            ),
                        aggregateScore = 8.0,
                    ),
                    RankedRatedItem(
                        item =
                            RatedItem(
                                id = "coupe",
                                scores = listOf(ScoreEntry(speed, 7)),
                                values = listOf(ItemAttributeValue(nationalityAttr, "US")),
                            ),
                        aggregateScore = 7.0,
                    ),
                )

            viewModel.state.test {
                awaitItem()
                viewModel.loadCategory("Cars")
                var state = awaitItem()
                if (state.isLoading) state = awaitItem()

                viewModel.onFilterSelected(
                    AttributeFilter.Nationality("Cars/Nationality", setOf("MX")),
                )
                state = awaitItem()
                assertEquals(1, state.items.size)
                assertEquals("sedan", state.items.single().id)

                viewModel.onFilterSelected(
                    AttributeFilter.Nationality("Cars/Nationality", setOf("US")),
                )
                state = awaitItem()
                assertEquals(1, state.items.size)
                assertEquals("coupe", state.items.single().id)
            }
        }

    @Test
    fun filterChipsIncludeOnlyFilterableTypes() =
        runTest {
            val nationalityAttr = Attribute("Cars/Nationality", type = AttributeType.NATIONALITY)
            val colorAttr = Attribute("Cars/Color", type = AttributeType.DROPDOWN)
            val imageAttr = Attribute("Cars/Photo", type = AttributeType.IMAGE)
            val urlAttr = Attribute("Cars/Link", type = AttributeType.URL)
            val notesAttr = Attribute("Cars/Notes", type = AttributeType.NOTES)
            val dateAttr = Attribute("Cars/Release", type = AttributeType.DATE)
            val boolAttr = Attribute("Cars/Active", type = AttributeType.BOOLEAN)
            categoryRepository.category.value =
                Category(
                    name = "Cars",
                    attributes =
                        listOf(
                            speed,
                            nationalityAttr,
                            colorAttr,
                            imageAttr,
                            urlAttr,
                            notesAttr,
                            dateAttr,
                            boolAttr,
                        ),
                )
            ratedItemRepository.rankedItems.value =
                listOf(
                    RankedRatedItem(
                        item = RatedItem(id = "sedan", scores = listOf(ScoreEntry(speed, 8))),
                        aggregateScore = 8.0,
                    ),
                )

            viewModel.state.test {
                awaitItem()
                viewModel.loadCategory("Cars")
                var state = awaitItem()
                if (state.isLoading) state = awaitItem()

                val chipTypes = state.filterChips.map { it.type }.toSet()
                assertEquals(
                    setOf(
                        AttributeType.NUMBER,
                        AttributeType.NATIONALITY,
                        AttributeType.DROPDOWN,
                        AttributeType.DATE,
                        AttributeType.BOOLEAN,
                    ),
                    chipTypes,
                )
            }
        }

    @Test
    fun filterChipsPopulateAvailableValues() =
        runTest {
            val nationalityAttr = Attribute("Cars/Nationality", type = AttributeType.NATIONALITY)
            categoryRepository.category.value =
                Category(name = "Cars", attributes = listOf(speed, nationalityAttr))
            ratedItemRepository.rankedItems.value =
                listOf(
                    RankedRatedItem(
                        item =
                            RatedItem(
                                id = "sedan",
                                scores = listOf(ScoreEntry(speed, 8)),
                                values = listOf(ItemAttributeValue(nationalityAttr, "MX")),
                            ),
                        aggregateScore = 8.0,
                    ),
                    RankedRatedItem(
                        item =
                            RatedItem(
                                id = "coupe",
                                scores = listOf(ScoreEntry(speed, 7)),
                                values = listOf(ItemAttributeValue(nationalityAttr, "US")),
                            ),
                        aggregateScore = 7.0,
                    ),
                    RankedRatedItem(
                        item =
                            RatedItem(
                                id = "suv",
                                scores = listOf(ScoreEntry(speed, 6)),
                                values = listOf(ItemAttributeValue(nationalityAttr, "MX")),
                            ),
                        aggregateScore = 6.0,
                    ),
                )

            viewModel.state.test {
                awaitItem()
                viewModel.loadCategory("Cars")
                var state = awaitItem()
                if (state.isLoading) state = awaitItem()

                val natChip = state.filterChips.first { it.type == AttributeType.NATIONALITY }
                assertEquals(setOf("MX", "US"), natChip.availableValues.toSet())
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

    private class FakeScoreProfileRepository : ScoreProfileRepository {
        val profiles = MutableStateFlow(emptyList<ScoreProfile>())

        override fun observeProfilesForCategory(categoryName: String): Flow<List<ScoreProfile>> = profiles

        override fun observeProfile(id: String): Flow<ScoreProfile?> {
            error("CategoryDetailViewModel does not observe single profiles")
        }

        override suspend fun saveProfile(profile: ScoreProfile) {
            error("CategoryDetailViewModel does not save profiles")
        }

        override suspend fun deleteProfile(id: String) {
            error("CategoryDetailViewModel does not delete profiles")
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
