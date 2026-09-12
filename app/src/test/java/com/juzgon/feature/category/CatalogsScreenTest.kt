@file:Suppress("LongParameterList")

package com.juzgon.feature.category

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.juzgon.domain.CatalogType
import com.juzgon.ui.theme.JuzgonTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class CatalogsScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun rendersTwoColumnGridWithCategoryCards() {
        val categories =
            listOf(
                CatalogCardUiModel(
                    name = "Formula 1 Drivers",
                    itemCount = 20,
                    averageRating = 9.4,
                    type = CatalogType.PERSON,
                ),
                CatalogCardUiModel(
                    name = "Sci-Fi Masterpieces",
                    itemCount = 15,
                    averageRating = 9.2,
                    type = CatalogType.MOVIE,
                ),
            )

        composeRule.setContent {
            JuzgonTheme(darkTheme = true, dynamicColor = false) {
                CatalogsScreen(
                    state = CatalogsUiState(categories = categories),
                    actions =
                        CatalogsScreenActions(
                            onSearchQueryChange = {},
                            onFilterSelected = {},
                            onCreateCategoryClick = {},
                            onCategoryClick = {},
                            onRetry = {},
                        ),
                )
            }
        }

        // Title and header
        composeRule.onNodeWithText("My Catalogs").assertIsDisplayed()

        // Card 1
        composeRule.onNodeWithText("Formula 1 Drivers").assertIsDisplayed()
        composeRule.onNodeWithText("20 items").assertIsDisplayed()
        composeRule.onNodeWithText("★ 9.4").assertIsDisplayed()

        // Card 2
        composeRule.onNodeWithText("Sci-Fi Masterpieces").assertIsDisplayed()
        composeRule.onNodeWithText("15 items").assertIsDisplayed()
        composeRule.onNodeWithText("★ 9.2").assertIsDisplayed()
    }

    @Test
    fun searchQueryFiltersCategoriesInRealTime() {
        var queryChanged = ""

        composeRule.setContent {
            MaterialTheme {
                CatalogsScreen(
                    state =
                        CatalogsUiState(
                            categories =
                                listOf(
                                    CatalogCardUiModel(name = "Formula 1 Drivers", itemCount = 20),
                                ),
                        ),
                    actions =
                        CatalogsScreenActions(
                            onSearchQueryChange = { queryChanged = it },
                            onFilterSelected = {},
                            onCreateCategoryClick = {},
                            onCategoryClick = {},
                            onRetry = {},
                        ),
                )
            }
        }

        composeRule.onNodeWithContentDescription("Search categories").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Search categories").performTextInput("Formula")

        assertEquals("Formula", queryChanged)
    }

    @Test
    fun typeFilterPillsFilterCategories() {
        var selectedType: CatalogType? = null

        val filterChips =
            listOf(
                CatalogTypeFilterChip(id = "all", label = "All", isSelected = true),
                CatalogTypeFilterChip(id = "PERSON", label = "People", type = CatalogType.PERSON),
                CatalogTypeFilterChip(id = "MOVIE", label = "Films", type = CatalogType.MOVIE),
                CatalogTypeFilterChip(id = "PRODUCT", label = "Products", type = CatalogType.PRODUCT),
            )

        composeRule.setContent {
            MaterialTheme {
                CatalogsScreen(
                    state = CatalogsUiState(filterChips = filterChips),
                    actions =
                        CatalogsScreenActions(
                            onSearchQueryChange = {},
                            onFilterSelected = { selectedType = it },
                            onCreateCategoryClick = {},
                            onCategoryClick = {},
                            onRetry = {},
                        ),
                )
            }
        }

        composeRule.onNodeWithText("All").assertIsDisplayed()
        composeRule.onNodeWithText("People").assertIsDisplayed()
        composeRule.onNodeWithText("Films").assertIsDisplayed()
        composeRule.onNodeWithText("Products").assertIsDisplayed()

        composeRule.onNodeWithText("Films").performClick()
        assertEquals(CatalogType.MOVIE, selectedType)

        composeRule.onNodeWithText("People").performClick()
        assertEquals(CatalogType.PERSON, selectedType)
    }

    @Test
    fun tappingCategoryCardNavigatesToDetail() {
        var openedCategory = ""

        composeRule.setContent {
            MaterialTheme {
                CatalogsScreen(
                    state =
                        CatalogsUiState(
                            categories =
                                listOf(
                                    CatalogCardUiModel(name = "Formula 1 Drivers", itemCount = 20),
                                ),
                        ),
                    actions =
                        CatalogsScreenActions(
                            onSearchQueryChange = {},
                            onFilterSelected = {},
                            onCreateCategoryClick = {},
                            onCategoryClick = { openedCategory = it },
                            onRetry = {},
                        ),
                )
            }
        }

        composeRule.onNodeWithText("Formula 1 Drivers").assertHasClickAction()
        composeRule.onNodeWithText("Formula 1 Drivers").performClick()

        assertEquals("Formula 1 Drivers", openedCategory)
    }

    @Test
    fun tappingFabNavigatesToCreateCategory() {
        var createClicked = false

        composeRule.setContent {
            MaterialTheme {
                CatalogsScreen(
                    state = CatalogsUiState(),
                    actions =
                        CatalogsScreenActions(
                            onSearchQueryChange = {},
                            onFilterSelected = {},
                            onCreateCategoryClick = { createClicked = true },
                            onCategoryClick = {},
                            onRetry = {},
                        ),
                )
            }
        }

        composeRule.onNodeWithContentDescription("Create category").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Create category").assertHasClickAction()
        composeRule.onNodeWithContentDescription("Create category").performClick()

        assertTrue(createClicked)
    }

    @Test
    fun rendersEmptyStateWhenNoCategoriesMatchSearchOrFilter() {
        composeRule.setContent {
            MaterialTheme {
                CatalogsScreen(
                    state =
                        CatalogsUiState(
                            searchQuery = "Unmatched query",
                            categories = emptyList(),
                        ),
                    actions =
                        CatalogsScreenActions(
                            onSearchQueryChange = {},
                            onFilterSelected = {},
                            onCreateCategoryClick = {},
                            onCategoryClick = {},
                            onRetry = {},
                        ),
                )
            }
        }

        composeRule.onNodeWithText("No categories match your search or filter").assertIsDisplayed()
    }

    @Test
    fun rendersEmptyStateWhenNoCategoriesExistYet() {
        var createClicked = false

        composeRule.setContent {
            MaterialTheme {
                CatalogsScreen(
                    state = CatalogsUiState(categories = emptyList()),
                    actions =
                        CatalogsScreenActions(
                            onSearchQueryChange = {},
                            onFilterSelected = {},
                            onCreateCategoryClick = { createClicked = true },
                            onCategoryClick = {},
                            onRetry = {},
                        ),
                )
            }
        }

        composeRule.onNodeWithText("No categories yet").assertIsDisplayed()
        composeRule.onNodeWithText("Create category").performClick()
        assertTrue(createClicked)
    }

    @Test
    fun showsLoadingState() {
        composeRule.setContent {
            MaterialTheme {
                CatalogsScreen(
                    state = CatalogsUiState(isLoading = true),
                    actions =
                        CatalogsScreenActions(
                            onSearchQueryChange = {},
                            onFilterSelected = {},
                            onCreateCategoryClick = {},
                            onCategoryClick = {},
                            onRetry = {},
                        ),
                )
            }
        }

        composeRule.onNodeWithContentDescription("Loading categories").assertIsDisplayed()
    }

    @Test
    fun showsErrorStateAndRetry() {
        var retryClicked = false

        composeRule.setContent {
            MaterialTheme {
                CatalogsScreen(
                    state = CatalogsUiState(errorMessage = "Failed to load categories"),
                    actions =
                        CatalogsScreenActions(
                            onSearchQueryChange = {},
                            onFilterSelected = {},
                            onCreateCategoryClick = {},
                            onCategoryClick = {},
                            onRetry = { retryClicked = true },
                        ),
                )
            }
        }

        composeRule.onNodeWithText("Failed to load categories").assertIsDisplayed()
        composeRule.onNodeWithText("Retry").assertIsDisplayed()
        composeRule.onNodeWithText("Retry").performClick()

        assertTrue(retryClicked)
    }
}
