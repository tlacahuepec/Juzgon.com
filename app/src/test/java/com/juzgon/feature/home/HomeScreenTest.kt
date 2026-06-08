package com.juzgon.feature.home

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.unit.dp
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
class HomeScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun homeScreenShowsEmptyState() {
        var createClicked = false

        composeRule.setContent {
            MaterialTheme {
                HomeScreen(
                    state = HomeUiState(),
                    actions =
                        HomeScreenActions(
                            onSearchQueryChange = {},
                            onSortOptionSelected = {},
                            onCreateCategoryClick = { createClicked = true },
                            onCategoryClick = {},
                            onRetry = {},
                        ),
                )
            }
        }

        composeRule.onNodeWithText("No categories yet").assertIsDisplayed()
        composeRule.onNodeWithText("Create category").assertIsDisplayed()
        composeRule.onNodeWithText("Create category").performClick()

        assertTrue(createClicked)
    }

    @Test
    fun homeScreenShowsContentState() {
        var createClicked = false
        var openedCategory = ""

        composeRule.setContent {
            MaterialTheme {
                HomeScreen(
                    state =
                        HomeUiState(
                            categories =
                                listOf(
                                    HomeCategoryUiModel(name = "Food", attributeCount = 2),
                                    HomeCategoryUiModel(name = "Travel", attributeCount = 1),
                                ),
                        ),
                    actions =
                        HomeScreenActions(
                            onSearchQueryChange = {},
                            onSortOptionSelected = {},
                            onCreateCategoryClick = { createClicked = true },
                            onCategoryClick = { openedCategory = it },
                            onRetry = {},
                        ),
                )
            }
        }

        composeRule.onNodeWithText("Search categories").assertIsDisplayed()
        composeRule.onNodeWithText("Recent").assertIsDisplayed()
        composeRule.onNodeWithText("Name").assertIsDisplayed()
        composeRule.onNodeWithText("Food").assertIsDisplayed()
        composeRule.onNodeWithText("0 items · 2 attributes").assertIsDisplayed()
        composeRule.onNodeWithText("Food").performClick()
        composeRule.onNodeWithTag(HOME_CATEGORY_LIST_TAG).performScrollToIndex(1)
        composeRule.onNodeWithText("Travel").assertIsDisplayed()
        composeRule.onNodeWithText("0 items · 1 attribute").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Create category").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Create category").performClick()

        assertTrue(createClicked)

        assertEquals("Food", openedCategory)
    }

    @Test
    fun homeVisualSummaryShowsDerivedStatsAndKeepsActionsReachable() {
        var createClicked = false
        var openedCategory = ""
        var exportClicked = false
        var aboutClicked = false
        var settingsClicked = false

        composeRule.setContent {
            JuzgonTheme(darkTheme = true, dynamicColor = false) {
                HomeScreen(
                    state =
                        HomeUiState(
                            categories =
                                listOf(
                                    HomeCategoryUiModel(name = "Food", attributeCount = 2, itemCount = 3),
                                    HomeCategoryUiModel(name = "Travel", attributeCount = 1, itemCount = 1),
                                ),
                            collectionStats =
                                HomeCollectionStatsUiModel(
                                    categoryCount = 2,
                                    itemCount = 4,
                                    attributeCount = 3,
                                ),
                        ),
                    actions =
                        HomeScreenActions(
                            onSearchQueryChange = {},
                            onSortOptionSelected = {},
                            onCreateCategoryClick = { createClicked = true },
                            onCategoryClick = { openedCategory = it },
                            onRetry = {},
                            onExportClick = { exportClicked = true },
                            onAboutClick = { aboutClicked = true },
                            onAiSettingsClick = { settingsClicked = true },
                        ),
                )
            }
        }

        composeRule.onNodeWithText("Collection overview").assertIsDisplayed()
        composeRule.onNodeWithText("2 categories").assertIsDisplayed()
        composeRule.onNodeWithText("4 items").assertIsDisplayed()
        composeRule.onNodeWithText("3 attributes").assertIsDisplayed()
        composeRule
            .onNodeWithContentDescription("Home collection summary, 2 categories, 4 items, 3 attributes")
            .assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Export backup").performClick()
        composeRule.onNodeWithContentDescription("About").performClick()
        composeRule.onNodeWithContentDescription("AI Settings").performClick()
        composeRule.onAllNodesWithContentDescription("Create category")[0].performClick()
        composeRule.onNodeWithText("Food").performScrollTo().performClick()

        assertTrue(exportClicked)
        assertTrue(aboutClicked)
        assertTrue(settingsClicked)
        assertTrue(createClicked)
        assertEquals("Food", openedCategory)
    }

    @Test
    fun homeInteractiveControlsExposeAccessibleSemantics() {
        composeRule.setContent {
            MaterialTheme {
                HomeScreen(
                    state =
                        HomeUiState(
                            categories =
                                listOf(
                                    HomeCategoryUiModel(name = "Food", attributeCount = 2),
                                ),
                        ),
                    actions =
                        HomeScreenActions(
                            onSearchQueryChange = {},
                            onSortOptionSelected = {},
                            onCreateCategoryClick = {},
                            onCategoryClick = {},
                            onRetry = {},
                        ),
                )
            }
        }

        composeRule.onNodeWithContentDescription("Create category").assertHasClickAction()
        composeRule.onNodeWithContentDescription("Search categories").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Sort categories by recent").assertHasClickAction()
        composeRule.onNodeWithContentDescription("Sort categories by name").assertHasClickAction()
        composeRule
            .onNodeWithContentDescription("Open category Food, 0 items · 2 attributes")
            .assertHasClickAction()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
    }

    @Test
    fun homePrimaryActionsMeetMinimumTouchTargetSize() {
        composeRule.setContent {
            MaterialTheme {
                HomeScreen(
                    state =
                        HomeUiState(
                            categories =
                                listOf(
                                    HomeCategoryUiModel(name = "Food", attributeCount = 2),
                                ),
                        ),
                    actions =
                        HomeScreenActions(
                            onSearchQueryChange = {},
                            onSortOptionSelected = {},
                            onCreateCategoryClick = {},
                            onCategoryClick = {},
                            onRetry = {},
                        ),
                )
            }
        }

        composeRule.onNodeWithContentDescription("Create category").assertMinimumTouchTarget()
        composeRule.onNodeWithContentDescription("Sort categories by recent").assertMinimumTouchTarget()
        composeRule.onNodeWithContentDescription("Sort categories by name").assertMinimumTouchTarget()
        composeRule
            .onNodeWithContentDescription("Open category Food, 0 items · 2 attributes")
            .assertMinimumTouchTarget()
    }

    @Test
    fun homeScreenShowsLoadingState() {
        composeRule.setContent {
            MaterialTheme {
                HomeScreen(
                    state = HomeUiState(isLoading = true),
                    actions =
                        HomeScreenActions(
                            onSearchQueryChange = {},
                            onSortOptionSelected = {},
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
    fun homeScreenShowsErrorState() {
        composeRule.setContent {
            MaterialTheme {
                HomeScreen(
                    state = HomeUiState(errorMessage = "Failed to load categories"),
                    actions =
                        HomeScreenActions(
                            onSearchQueryChange = {},
                            onSortOptionSelected = {},
                            onCreateCategoryClick = {},
                            onCategoryClick = {},
                            onRetry = {},
                        ),
                )
            }
        }

        composeRule.onNodeWithText("Failed to load categories").assertIsDisplayed()
        composeRule.onNodeWithText("Retry").assertIsDisplayed()
    }

    @Test
    fun retryButtonInvokesCallback() {
        var retryClicked = false

        composeRule.setContent {
            MaterialTheme {
                HomeScreen(
                    state = HomeUiState(errorMessage = "Failed to load categories"),
                    actions =
                        HomeScreenActions(
                            onSearchQueryChange = {},
                            onSortOptionSelected = {},
                            onCreateCategoryClick = {},
                            onCategoryClick = {},
                            onRetry = { retryClicked = true },
                        ),
                )
            }
        }

        composeRule.onNodeWithText("Retry").performClick()

        assertTrue(retryClicked)
    }

    @Test
    fun exportButtonIsDisplayedAndInvokesCallback() {
        var exportClicked = false

        composeRule.setContent {
            MaterialTheme {
                HomeScreen(
                    state =
                        HomeUiState(
                            categories =
                                listOf(
                                    HomeCategoryUiModel(name = "Food", attributeCount = 2),
                                ),
                        ),
                    actions =
                        HomeScreenActions(
                            onSearchQueryChange = {},
                            onSortOptionSelected = {},
                            onCreateCategoryClick = {},
                            onCategoryClick = {},
                            onRetry = {},
                            onExportClick = { exportClicked = true },
                        ),
                )
            }
        }

        composeRule.onNodeWithContentDescription("Export backup").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Export backup").performClick()

        assertTrue(exportClicked)
    }

    @Test
    fun aboutButtonIsDisplayedAndInvokesCallback() {
        var aboutClicked = false

        composeRule.setContent {
            MaterialTheme {
                HomeScreen(
                    state =
                        HomeUiState(
                            categories =
                                listOf(
                                    HomeCategoryUiModel(name = "Food", attributeCount = 2),
                                ),
                        ),
                    actions =
                        HomeScreenActions(
                            onSearchQueryChange = {},
                            onSortOptionSelected = {},
                            onCreateCategoryClick = {},
                            onCategoryClick = {},
                            onRetry = {},
                            onAboutClick = { aboutClicked = true },
                        ),
                )
            }
        }

        composeRule.onNodeWithContentDescription("About").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("About").performClick()

        assertTrue(aboutClicked)
    }

    @Test
    fun heroCardRendersWithTopItemData() {
        composeRule.setContent {
            JuzgonTheme(darkTheme = true, dynamicColor = false) {
                HomeScreen(
                    state =
                        HomeUiState(
                            categories =
                                listOf(
                                    HomeCategoryUiModel(name = "Games", attributeCount = 3, itemCount = 2),
                                ),
                            heroItem =
                                HomeHeroUiModel(
                                    name = "Elden Ring",
                                    tierLabel = "S-Tier",
                                    scoreText = "9.5/10",
                                    categoryName = "Games",
                                ),
                        ),
                    actions =
                        HomeScreenActions(
                            onSearchQueryChange = {},
                            onSortOptionSelected = {},
                            onCreateCategoryClick = {},
                            onCategoryClick = {},
                            onRetry = {},
                        ),
                )
            }
        }

        composeRule.onNodeWithText("Elden Ring").assertIsDisplayed()
        composeRule.onNodeWithText("S-Tier").assertIsDisplayed()
        composeRule.onNodeWithText("9.5/10").assertIsDisplayed()
    }

    @Test
    fun trendingRowDisplaysTopScoredItems() {
        composeRule.setContent {
            JuzgonTheme(darkTheme = true, dynamicColor = false) {
                HomeScreen(
                    state =
                        HomeUiState(
                            categories =
                                listOf(
                                    HomeCategoryUiModel(name = "Games", attributeCount = 2, itemCount = 3),
                                ),
                            trendingItems =
                                listOf(
                                    HomeTrendingItemUiModel(
                                        name = "Alpha",
                                        scoreText = "9.0/10",
                                        contentDescription = "Alpha, score 9.0/10",
                                        categoryName = "Games",
                                    ),
                                    HomeTrendingItemUiModel(
                                        name = "Beta",
                                        scoreText = "8.5/10",
                                        contentDescription = "Beta, score 8.5/10",
                                        categoryName = "Games",
                                    ),
                                ),
                        ),
                    actions =
                        HomeScreenActions(
                            onSearchQueryChange = {},
                            onSortOptionSelected = {},
                            onCreateCategoryClick = {},
                            onCategoryClick = {},
                            onRetry = {},
                        ),
                )
            }
        }

        composeRule.onNodeWithText("Trending").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Alpha, score 9.0/10").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Beta, score 8.5/10").assertIsDisplayed()
    }

    @Test
    fun segmentedFilterTriggersSortChange() {
        var selectedSort = HomeSortOption.Recent

        composeRule.setContent {
            JuzgonTheme(darkTheme = true, dynamicColor = false) {
                HomeScreen(
                    state =
                        HomeUiState(
                            categories =
                                listOf(
                                    HomeCategoryUiModel(name = "Food", attributeCount = 1),
                                ),
                        ),
                    actions =
                        HomeScreenActions(
                            onSearchQueryChange = {},
                            onSortOptionSelected = { selectedSort = it },
                            onCreateCategoryClick = {},
                            onCategoryClick = {},
                            onRetry = {},
                        ),
                )
            }
        }

        composeRule.onNodeWithText("Name").performClick()
        assertEquals(HomeSortOption.Name, selectedSort)
    }

    @Test
    fun emptyStateRendersWithoutHeroOrTrending() {
        composeRule.setContent {
            JuzgonTheme(darkTheme = true, dynamicColor = false) {
                HomeScreen(
                    state = HomeUiState(),
                    actions =
                        HomeScreenActions(
                            onSearchQueryChange = {},
                            onSortOptionSelected = {},
                            onCreateCategoryClick = {},
                            onCategoryClick = {},
                            onRetry = {},
                        ),
                )
            }
        }

        composeRule.onNodeWithText("No categories yet").assertIsDisplayed()
        composeRule.onNodeWithText("Create category").assertIsDisplayed()
    }

    private fun androidx.compose.ui.test.SemanticsNodeInteraction.assertMinimumTouchTarget() {
        assertWidthIsAtLeast(48.dp)
        assertHeightIsAtLeast(48.dp)
    }
}
