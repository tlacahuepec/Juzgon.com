@file:Suppress("LongParameterList", "LargeClass")

package com.juzgon.feature.category

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import com.juzgon.ui.components.GridCardAttribute
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class CategoryDetailScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun emptyStateRenders() {
        setContent(
            CategoryDetailUiState(
                categoryName = "Cars",
                attributeSummary = "4 attributes",
                isLoading = false,
            ),
        )

        composeRule.onNodeWithText("Cars").assertIsDisplayed()
        composeRule.onNodeWithText("4 attributes").assertIsDisplayed()
        composeRule.onNodeWithText("No items yet").assertIsDisplayed()
        composeRule.onNodeWithText("Add item").assertIsDisplayed()
    }

    @Test
    fun contentStateRendersItemsAndAverageBadges() {
        setContent(
            CategoryDetailUiState(
                categoryName = "Cars",
                attributeSummary = "4 attributes",
                items =
                    listOf(
                        CategoryDetailItemUiModel(rank = 1, id = "sedan", averageScoreText = "8.7"),
                        CategoryDetailItemUiModel(rank = 2, id = "coupe", averageScoreText = "7.4"),
                    ),
                isLoading = false,
            ),
        )

        composeRule.onNodeWithText("4 attributes").assertIsDisplayed()
        composeRule.onNodeWithText("sedan").assertIsDisplayed()
        composeRule.onNodeWithText("#1").assertIsDisplayed()
        composeRule.onNodeWithText("8.7").assertIsDisplayed()
        composeRule.onNodeWithText("coupe").assertIsDisplayed()
        composeRule.onNodeWithText("#2").assertIsDisplayed()
        composeRule.onNodeWithText("7.4").assertIsDisplayed()
    }

    @Test
    fun contentStateRendersVisualItemCards() {
        setContent(
            CategoryDetailUiState(
                categoryName = "Cars",
                attributeSummary = "4 attributes",
                items =
                    listOf(
                        CategoryDetailItemUiModel(
                            rank = 1,
                            id = "sedan",
                            averageScoreText = "8.7",
                            imageValue = "content://images/sedan",
                        ),
                        CategoryDetailItemUiModel(rank = 2, id = "coupe", averageScoreText = "7.4"),
                    ),
                isLoading = false,
            ),
        )

        composeRule.onNodeWithContentDescription("sedan image preview").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("coupe image placeholder").assertIsDisplayed()
    }

    @Test
    fun multiWordTitleRendersPrimaryAndRemainingSegments() {
        setContent(
            CategoryDetailUiState(
                categoryName = "Cars",
                attributeSummary = "4 attributes",
                items =
                    listOf(
                        CategoryDetailItemUiModel(
                            rank = 1,
                            id = "Grand Touring Sedan",
                            averageScoreText = "8.7",
                        ),
                    ),
                isLoading = false,
            ),
        )

        composeRule.onNodeWithText("Grand").assertIsDisplayed()
        composeRule.onNodeWithText("Touring Sedan").assertIsDisplayed()
        composeRule
            .onNodeWithContentDescription("Rated item Grand Touring Sedan, rank 1, Score 8.7")
            .assertIsDisplayed()
    }

    @Test
    fun singleWordTitleRendersSingleEmphasizedSegment() {
        setContent(
            CategoryDetailUiState(
                categoryName = "Cars",
                attributeSummary = "4 attributes",
                items =
                    listOf(
                        CategoryDetailItemUiModel(
                            rank = 1,
                            id = "Roadster",
                            averageScoreText = "8.7",
                        ),
                    ),
                isLoading = false,
            ),
        )

        composeRule.onAllNodesWithText("Roadster").assertCountEquals(1)
    }

    @Test
    fun itemRowsExposeAccessibleSummarySemantics() {
        setContent(
            CategoryDetailUiState(
                categoryName = "Cars",
                attributeSummary = "4 attributes",
                items =
                    listOf(
                        CategoryDetailItemUiModel(rank = 1, id = "sedan", averageScoreText = "8.7"),
                    ),
                isLoading = false,
            ),
        )

        composeRule.onNodeWithContentDescription("Rated item sedan, rank 1, Score 8.7").assertIsDisplayed()
    }

    @Test
    fun itemRowInvokesEditCallback() {
        var editedItemId: String? = null
        setContent(
            state =
                CategoryDetailUiState(
                    categoryName = "Cars",
                    attributeSummary = "4 attributes",
                    items =
                        listOf(
                            CategoryDetailItemUiModel(rank = 1, id = "sedan", averageScoreText = "8.7"),
                        ),
                    isLoading = false,
                ),
            onEditItemClick = { editedItemId = it },
        )

        composeRule.onNodeWithContentDescription("Rated item sedan, rank 1, Score 8.7").performClick()

        assertEquals("sedan", editedItemId)
    }

    @Test
    fun detailNavigationAndItemRowsMeetMinimumTouchTargetSize() {
        setContent(
            CategoryDetailUiState(
                categoryName = "Cars",
                attributeSummary = "4 attributes",
                items =
                    listOf(
                        CategoryDetailItemUiModel(rank = 1, id = "sedan", averageScoreText = "8.7"),
                    ),
                isLoading = false,
            ),
        )

        composeRule.onNodeWithContentDescription("Back").assertMinimumTouchTarget()
        composeRule.onNodeWithContentDescription("Add item").assertMinimumTouchTarget()
        composeRule
            .onNodeWithContentDescription("Rated item sedan, rank 1, Score 8.7")
            .assertMinimumTouchTarget()
    }

    @Test
    fun attributeSortCardDisplaysAttributeMetricInsteadOfGeneralScore() {
        setContent(
            CategoryDetailUiState(
                categoryName = "Cars",
                attributeSummary = "2 attributes",
                items =
                    listOf(
                        CategoryDetailItemUiModel(
                            rank = 1,
                            id = "sedan",
                            averageScoreText = "8.7",
                            metricLabel = "Speed",
                            metricValueText = "9",
                        ),
                    ),
                sortOption = CategoryDetailSortOption.Attribute("Speed"),
                isLoading = false,
            ),
        )

        composeRule.onNodeWithText("Speed").assertIsDisplayed()
        composeRule.onNodeWithText("9").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Rated item sedan, rank 1, Speed 9").assertIsDisplayed()
        composeRule.onNodeWithText("8.7").assertDoesNotExist()
    }

    @Test
    fun backButtonInvokesCallback() {
        var backClicked = false
        setContent(
            state =
                CategoryDetailUiState(
                    categoryName = "Cars",
                    isLoading = false,
                ),
            onBackClick = { backClicked = true },
        )

        composeRule.onNodeWithContentDescription("Back").performClick()

        assertTrue(backClicked)
    }

    @Test
    fun addItemActionInvokesCallbackFromEmptyState() {
        var addItemClicked = false
        setContent(
            state =
                CategoryDetailUiState(
                    categoryName = "Cars",
                    attributeSummary = "4 attributes",
                    isLoading = false,
                ),
            onAddItemClick = { addItemClicked = true },
        )

        composeRule.onNodeWithContentDescription("Add item").performClick()

        assertTrue(addItemClicked)
    }

    @Test
    fun addItemActionIsAvailableInContentState() {
        setContent(
            CategoryDetailUiState(
                categoryName = "Cars",
                attributeSummary = "4 attributes",
                items =
                    listOf(
                        CategoryDetailItemUiModel(rank = 1, id = "sedan", averageScoreText = "8.7"),
                    ),
                isLoading = false,
            ),
        )

        composeRule.onNodeWithContentDescription("Add item").assertHasClickAction()
    }

    @Test
    fun errorStateRendersWithRetryButton() {
        setContent(
            CategoryDetailUiState(
                categoryName = "Cars",
                isLoading = false,
                errorMessage = "Category not found",
            ),
        )

        composeRule.onNodeWithText("Category not found").assertIsDisplayed()
        composeRule.onNodeWithText("Retry").assertIsDisplayed()
    }

    @Test
    fun retryButtonInvokesCallback() {
        var retryClicked = false
        setContent(
            state =
                CategoryDetailUiState(
                    categoryName = "Cars",
                    isLoading = false,
                    errorMessage = "Category not found",
                ),
            onRetry = { retryClicked = true },
        )

        composeRule.onNodeWithText("Retry").performClick()

        assertTrue(retryClicked)
    }

    @Test
    fun sortControlsAreDisplayedWhenContentLoaded() {
        setContent(
            CategoryDetailUiState(
                categoryName = "Cars",
                attributeSummary = "2 attributes",
                items =
                    listOf(
                        CategoryDetailItemUiModel(rank = 1, id = "sedan", averageScoreText = "8.7"),
                    ),
                isLoading = false,
            ),
        )

        composeRule.onNodeWithContentDescription("Sort items by score").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Sort items by name").assertIsDisplayed()
    }

    @Test
    fun sortControlsMeetMinimumTouchTargetSize() {
        setContent(
            CategoryDetailUiState(
                categoryName = "Cars",
                attributeSummary = "2 attributes",
                items =
                    listOf(
                        CategoryDetailItemUiModel(rank = 1, id = "sedan", averageScoreText = "8.7"),
                    ),
                isLoading = false,
            ),
        )

        composeRule.onNodeWithContentDescription("Sort items by score").assertMinimumTouchTarget()
        composeRule.onNodeWithContentDescription("Sort items by name").assertMinimumTouchTarget()
    }

    @Test
    fun sortControlsRenderAttributeSortOption() {
        setContent(
            CategoryDetailUiState(
                categoryName = "Cars",
                attributeSummary = "3 attributes",
                items =
                    listOf(
                        CategoryDetailItemUiModel(rank = 1, id = "sedan", averageScoreText = "8.7"),
                    ),
                sortOptions =
                    listOf(
                        CategoryDetailSortOptionUiModel(
                            option = CategoryDetailSortOption.Score,
                            label = "Score",
                            contentDescription = "Sort items by score",
                        ),
                        CategoryDetailSortOptionUiModel(
                            option = CategoryDetailSortOption.Name,
                            label = "Name",
                            contentDescription = "Sort items by name",
                        ),
                        CategoryDetailSortOptionUiModel(
                            option = CategoryDetailSortOption.Attribute("Speed"),
                            label = "Speed",
                            contentDescription = "Sort items by Speed",
                        ),
                    ),
                isLoading = false,
            ),
        )

        composeRule.onNodeWithContentDescription("Sort items by Speed").assertIsDisplayed()
    }

    @Test
    fun sortControlInvokesCallbackForAttributeOption() {
        var selectedSort: CategoryDetailSortOption? = null
        setContent(
            state =
                CategoryDetailUiState(
                    categoryName = "Cars",
                    attributeSummary = "3 attributes",
                    items =
                        listOf(
                            CategoryDetailItemUiModel(rank = 1, id = "sedan", averageScoreText = "8.7"),
                        ),
                    sortOptions =
                        listOf(
                            CategoryDetailSortOptionUiModel(
                                option = CategoryDetailSortOption.Score,
                                label = "Score",
                                contentDescription = "Sort items by score",
                            ),
                            CategoryDetailSortOptionUiModel(
                                option = CategoryDetailSortOption.Attribute("Speed"),
                                label = "Speed",
                                contentDescription = "Sort items by Speed",
                            ),
                        ),
                    isLoading = false,
                ),
            onSortOptionSelected = { selectedSort = it },
        )

        composeRule.onNodeWithContentDescription("Sort items by Speed").performClick()

        assertEquals(CategoryDetailSortOption.Attribute("Speed"), selectedSort)
    }

    @Test
    fun editAndDeleteActionsAreAvailableWhenLoaded() {
        setContent(
            CategoryDetailUiState(
                categoryName = "Cars",
                attributeSummary = "4 attributes",
                isLoading = false,
            ),
        )

        composeRule.onNodeWithContentDescription("Edit category").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Delete category").assertIsDisplayed()
    }

    @Test
    fun editCategoryButtonInvokesCallback() {
        var editClicked = false
        setContent(
            state = CategoryDetailUiState(categoryName = "Cars", isLoading = false),
            onEditCategoryClick = { editClicked = true },
        )

        composeRule.onNodeWithContentDescription("Edit category").performClick()

        assertTrue(editClicked)
    }

    @Test
    fun deleteCategoryButtonInvokesCallback() {
        var deleteClicked = false
        setContent(
            state = CategoryDetailUiState(categoryName = "Cars", isLoading = false),
            onDeleteClick = { deleteClicked = true },
        )

        composeRule.onNodeWithContentDescription("Delete category").performClick()

        assertTrue(deleteClicked)
    }

    @Test
    fun deleteConfirmDialogIsShownWhenFlagSet() {
        setContent(
            CategoryDetailUiState(
                categoryName = "Cars",
                isLoading = false,
                showDeleteConfirmDialog = true,
            ),
        )

        composeRule.onNodeWithText("Delete category").assertIsDisplayed()
        composeRule
            .onNodeWithText("Are you sure you want to delete this category? This action cannot be undone.")
            .assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Confirm delete").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Cancel delete").assertIsDisplayed()
    }

    @Test
    fun deleteWarningDialogIsShownWithItemCountWhenFlagSet() {
        setContent(
            CategoryDetailUiState(
                categoryName = "Cars",
                items =
                    listOf(
                        CategoryDetailItemUiModel(rank = 1, id = "sedan", averageScoreText = "8.7"),
                        CategoryDetailItemUiModel(rank = 2, id = "coupe", averageScoreText = "7.4"),
                    ),
                isLoading = false,
                showDeleteWithItemsWarning = true,
            ),
        )

        composeRule
            .onNodeWithText("This category has 2 items that will also be deleted. This action cannot be undone.")
            .assertIsDisplayed()
    }

    @Test
    fun deleteConfirmButtonInvokesConfirmCallback() {
        var confirmed = false
        setContent(
            state =
                CategoryDetailUiState(
                    categoryName = "Cars",
                    isLoading = false,
                    showDeleteConfirmDialog = true,
                ),
            onDeleteConfirmed = { confirmed = true },
        )

        composeRule.onNodeWithContentDescription("Confirm delete").performClick()

        assertTrue(confirmed)
    }

    @Test
    fun deleteCancelButtonInvokesDismissCallback() {
        var dismissed = false
        setContent(
            state =
                CategoryDetailUiState(
                    categoryName = "Cars",
                    isLoading = false,
                    showDeleteConfirmDialog = true,
                ),
            onDeleteDialogDismissed = { dismissed = true },
        )

        composeRule.onNodeWithContentDescription("Cancel delete").performClick()

        assertTrue(dismissed)
    }

    @Test
    fun editAndDeleteActionsNotShownDuringLoading() {
        setContent(
            CategoryDetailUiState(
                categoryName = "Cars",
                isLoading = true,
            ),
        )

        composeRule.onNodeWithContentDescription("Edit category").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Delete category").assertDoesNotExist()
    }

    @Test
    fun editAndDeleteActionsMeetMinimumTouchTargetSize() {
        setContent(
            CategoryDetailUiState(
                categoryName = "Cars",
                isLoading = false,
            ),
        )

        composeRule.onNodeWithContentDescription("Edit category").assertMinimumTouchTarget()
        composeRule.onNodeWithContentDescription("Delete category").assertMinimumTouchTarget()
    }

    @Test
    fun compactSortTriggerAppearsWhenMoreThanSixSortOptions() {
        setContent(
            CategoryDetailUiState(
                categoryName = "Cars",
                attributeSummary = "7 attributes",
                items =
                    listOf(
                        CategoryDetailItemUiModel(rank = 1, id = "sedan", averageScoreText = "8.7"),
                    ),
                sortOptions = buildManySortOptions(count = 7),
                isLoading = false,
            ),
        )

        composeRule
            .onNodeWithContentDescription("Sort options, currently sorted by Score")
            .assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Sort items by score").assertDoesNotExist()
    }

    @Test
    fun compactSortTriggerMeetsMinimumTouchTargetSize() {
        setContent(
            CategoryDetailUiState(
                categoryName = "Cars",
                attributeSummary = "7 attributes",
                items =
                    listOf(
                        CategoryDetailItemUiModel(rank = 1, id = "sedan", averageScoreText = "8.7"),
                    ),
                sortOptions = buildManySortOptions(count = 7),
                isLoading = false,
            ),
        )

        composeRule
            .onNodeWithContentDescription("Sort options, currently sorted by Score")
            .assertMinimumTouchTarget()
    }

    @Test
    fun compactSortTriggerOpensBottomSheet() {
        setContent(
            CategoryDetailUiState(
                categoryName = "Cars",
                attributeSummary = "7 attributes",
                items =
                    listOf(
                        CategoryDetailItemUiModel(rank = 1, id = "sedan", averageScoreText = "8.7"),
                    ),
                sortOptions = buildManySortOptions(count = 7),
                isLoading = false,
            ),
        )

        composeRule
            .onNodeWithContentDescription("Sort options, currently sorted by Score")
            .performClick()

        composeRule.onNodeWithContentDescription("Sort options sheet").assertIsDisplayed()
    }

    @Test
    fun bottomSheetListsAllSortOptions() {
        setContent(
            CategoryDetailUiState(
                categoryName = "Cars",
                attributeSummary = "7 attributes",
                items =
                    listOf(
                        CategoryDetailItemUiModel(rank = 1, id = "sedan", averageScoreText = "8.7"),
                    ),
                sortOptions = buildManySortOptions(count = 7),
                isLoading = false,
            ),
        )

        composeRule
            .onNodeWithContentDescription("Sort options, currently sorted by Score")
            .performClick()

        composeRule.onNodeWithContentDescription("Sort items by score").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Sort items by name").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Sort items by Attr1").assertIsDisplayed()
    }

    @Test
    fun bottomSheetSelectionInvokesCallback() {
        var selectedSort: CategoryDetailSortOption? = null
        setContent(
            state =
                CategoryDetailUiState(
                    categoryName = "Cars",
                    attributeSummary = "7 attributes",
                    items =
                        listOf(
                            CategoryDetailItemUiModel(rank = 1, id = "sedan", averageScoreText = "8.7"),
                        ),
                    sortOptions = buildManySortOptions(count = 7),
                    isLoading = false,
                ),
            onSortOptionSelected = { selectedSort = it },
        )

        composeRule
            .onNodeWithContentDescription("Sort options, currently sorted by Score")
            .performClick()
        composeRule.onNodeWithContentDescription("Sort items by Attr1").performClick()

        assertEquals(CategoryDetailSortOption.Attribute("Attr1"), selectedSort)
    }

    @Test
    fun searchFieldAppearsWhenTenOrMoreSortOptions() {
        setContent(
            CategoryDetailUiState(
                categoryName = "Cars",
                attributeSummary = "10 attributes",
                items =
                    listOf(
                        CategoryDetailItemUiModel(rank = 1, id = "sedan", averageScoreText = "8.7"),
                    ),
                sortOptions = buildManySortOptions(count = 10),
                isLoading = false,
            ),
        )

        composeRule
            .onNodeWithContentDescription("Sort options, currently sorted by Score")
            .performClick()

        composeRule.onNodeWithContentDescription("Search sort options").assertIsDisplayed()
    }

    @Test
    fun searchFieldNotShownForFewerThanTenOptions() {
        setContent(
            CategoryDetailUiState(
                categoryName = "Cars",
                attributeSummary = "7 attributes",
                items =
                    listOf(
                        CategoryDetailItemUiModel(rank = 1, id = "sedan", averageScoreText = "8.7"),
                    ),
                sortOptions = buildManySortOptions(count = 7),
                isLoading = false,
            ),
        )

        composeRule
            .onNodeWithContentDescription("Sort options, currently sorted by Score")
            .performClick()

        composeRule.onNodeWithContentDescription("Search sort options").assertDoesNotExist()
    }

    @Test
    fun searchFieldFiltersOptions() {
        setContent(
            CategoryDetailUiState(
                categoryName = "Cars",
                attributeSummary = "10 attributes",
                items =
                    listOf(
                        CategoryDetailItemUiModel(rank = 1, id = "sedan", averageScoreText = "8.7"),
                    ),
                sortOptions = buildManySortOptions(count = 10),
                isLoading = false,
            ),
        )

        composeRule
            .onNodeWithContentDescription("Sort options, currently sorted by Score")
            .performClick()
        composeRule
            .onNodeWithContentDescription("Search sort options")
            .performTextInput("Attr3")

        composeRule.onNodeWithContentDescription("Sort items by Attr3").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Sort items by Attr1").assertDoesNotExist()
    }

    @Test
    fun fifteenAttributesRenderWithoutCrash() {
        setContent(
            CategoryDetailUiState(
                categoryName = "Cars",
                attributeSummary = "15 attributes",
                items =
                    listOf(
                        CategoryDetailItemUiModel(rank = 1, id = "sedan", averageScoreText = "8.7"),
                    ),
                sortOptions = buildManySortOptions(count = 15),
                isLoading = false,
            ),
        )

        composeRule
            .onNodeWithContentDescription("Sort options, currently sorted by Score")
            .assertIsDisplayed()
    }

    @Test
    fun inlineChipsStillRenderForSixOrFewerOptions() {
        setContent(
            CategoryDetailUiState(
                categoryName = "Cars",
                attributeSummary = "4 attributes",
                items =
                    listOf(
                        CategoryDetailItemUiModel(rank = 1, id = "sedan", averageScoreText = "8.7"),
                    ),
                sortOptions = buildManySortOptions(count = 4),
                isLoading = false,
            ),
        )

        composeRule.onNodeWithContentDescription("Sort items by score").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Sort items by name").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Sort items by Attr1").assertIsDisplayed()
    }

    @Test
    fun compactTriggerShowsSelectedAttributeLabel() {
        setContent(
            CategoryDetailUiState(
                categoryName = "Cars",
                attributeSummary = "7 attributes",
                items =
                    listOf(
                        CategoryDetailItemUiModel(
                            rank = 1,
                            id = "sedan",
                            averageScoreText = "8.7",
                            metricLabel = "Attr3",
                            metricValueText = "fast",
                        ),
                    ),
                sortOption = CategoryDetailSortOption.Attribute("Attr3"),
                sortOptions = buildManySortOptions(count = 7),
                isLoading = false,
            ),
        )

        composeRule
            .onNodeWithContentDescription("Sort options, currently sorted by Attr3")
            .assertIsDisplayed()
    }

    @Test
    fun profileSelectorRendersOptions() {
        setContent(
            CategoryDetailUiState(
                categoryName = "Cars",
                attributeSummary = "2 attributes",
                items =
                    listOf(
                        CategoryDetailItemUiModel(rank = 1, id = "sedan", averageScoreText = "8.7"),
                    ),
                isLoading = false,
                profiles =
                    listOf(
                        ProfileOption(id = null, name = "All Attributes"),
                        ProfileOption(id = "p1", name = "Speed Focus"),
                    ),
            ),
        )

        composeRule.onNodeWithContentDescription("Profile: All Attributes").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Profile: Speed Focus").assertIsDisplayed()
    }

    @Test
    fun profileSelectorInvokesCallback() {
        var selectedId: String? = "initial"
        setContent(
            state =
                CategoryDetailUiState(
                    categoryName = "Cars",
                    attributeSummary = "2 attributes",
                    items =
                        listOf(
                            CategoryDetailItemUiModel(rank = 1, id = "sedan", averageScoreText = "8.7"),
                        ),
                    isLoading = false,
                    profiles =
                        listOf(
                            ProfileOption(id = null, name = "All Attributes"),
                            ProfileOption(id = "p1", name = "Speed Focus"),
                        ),
                ),
            onProfileSelected = { selectedId = it },
        )

        composeRule.onNodeWithContentDescription("Profile: Speed Focus").performClick()
        assertEquals("p1", selectedId)
    }

    @Test
    fun activeProfileLabelDisplayed() {
        setContent(
            CategoryDetailUiState(
                categoryName = "Cars",
                attributeSummary = "2 attributes",
                items =
                    listOf(
                        CategoryDetailItemUiModel(rank = 1, id = "sedan", averageScoreText = "8.7"),
                    ),
                isLoading = false,
                profiles =
                    listOf(
                        ProfileOption(id = null, name = "All Attributes"),
                        ProfileOption(id = "p1", name = "Speed Focus"),
                    ),
                activeProfileId = "p1",
                activeProfileLabel = "Ranking: Speed Focus",
            ),
        )

        composeRule.onNodeWithText("Ranking: Speed Focus").assertIsDisplayed()
    }

    @Test
    fun noProfileSelectorWhenNoProfiles() {
        setContent(
            CategoryDetailUiState(
                categoryName = "Cars",
                attributeSummary = "2 attributes",
                items =
                    listOf(
                        CategoryDetailItemUiModel(rank = 1, id = "sedan", averageScoreText = "8.7"),
                    ),
                isLoading = false,
                profiles = emptyList(),
            ),
        )

        composeRule.onNodeWithContentDescription("Profile: All Attributes").assertDoesNotExist()
    }

    private fun buildManySortOptions(count: Int): List<CategoryDetailSortOptionUiModel> =
        listOf(
            CategoryDetailSortOptionUiModel(
                option = CategoryDetailSortOption.Score,
                label = "Score",
                contentDescription = "Sort items by score",
            ),
            CategoryDetailSortOptionUiModel(
                option = CategoryDetailSortOption.Name,
                label = "Name",
                contentDescription = "Sort items by name",
            ),
        ) +
            (1..count).map { index ->
                CategoryDetailSortOptionUiModel(
                    option = CategoryDetailSortOption.Attribute("Attr$index"),
                    label = "Attr$index",
                    contentDescription = "Sort items by Attr$index",
                )
            }

    private fun setContent(
        state: CategoryDetailUiState,
        onBackClick: () -> Unit = {},
        onRetry: () -> Unit = {},
        onSortOptionSelected: (CategoryDetailSortOption) -> Unit = {},
        onAddItemClick: () -> Unit = {},
        onEditItemClick: (String) -> Unit = {},
        onDeleteClick: () -> Unit = {},
        onDeleteConfirmed: () -> Unit = {},
        onDeleteDialogDismissed: () -> Unit = {},
        onEditCategoryClick: () -> Unit = {},
        onScoreProfilesClick: () -> Unit = {},
        onProfileSelected: (String?) -> Unit = {},
        onVisibleRangeSelected: (CategoryDetailVisibleRange) -> Unit = {},
        onViewModeToggled: () -> Unit = {},
    ) {
        composeRule.setContent {
            MaterialTheme {
                CategoryDetailScreen(
                    state = state,
                    onBackClick = onBackClick,
                    onRetry = onRetry,
                    onSortOptionSelected = onSortOptionSelected,
                    onAddItemClick = onAddItemClick,
                    onEditItemClick = onEditItemClick,
                    onDeleteClick = onDeleteClick,
                    onDeleteConfirmed = onDeleteConfirmed,
                    onDeleteDialogDismissed = onDeleteDialogDismissed,
                    onEditCategoryClick = onEditCategoryClick,
                    onScoreProfilesClick = onScoreProfilesClick,
                    onProfileSelected = onProfileSelected,
                    onVisibleRangeSelected = onVisibleRangeSelected,
                    onViewModeToggled = onViewModeToggled,
                )
            }
        }
    }

    // region Visible Range Tests

    @Test
    fun visibleRangeChipsDisplayedWhenOptionsExist() {
        setContent(
            CategoryDetailUiState(
                categoryName = "Cars",
                attributeSummary = "2 attributes",
                items =
                    (1..10).map { i ->
                        CategoryDetailItemUiModel(rank = i, id = "Item$i", averageScoreText = "8.0")
                    },
                isLoading = false,
                visibleRange = CategoryDetailVisibleRange.Top10,
                visibleRangeOptions =
                    listOf(
                        CategoryDetailVisibleRange.Top10,
                        CategoryDetailVisibleRange.Top20,
                        CategoryDetailVisibleRange.Top50,
                        CategoryDetailVisibleRange.All,
                    ),
            ),
        )

        composeRule.onNodeWithText("Top 10").assertIsDisplayed()
        composeRule.onNodeWithText("Top 20").assertIsDisplayed()
        composeRule.onNodeWithText("Top 50").assertIsDisplayed()
        composeRule.onNodeWithText("All").assertIsDisplayed()
    }

    @Test
    fun visibleRangeChipsHiddenWhenNoOptions() {
        setContent(
            CategoryDetailUiState(
                categoryName = "Cars",
                attributeSummary = "2 attributes",
                items =
                    listOf(
                        CategoryDetailItemUiModel(rank = 1, id = "sedan", averageScoreText = "8.7"),
                    ),
                isLoading = false,
                visibleRangeOptions = emptyList(),
            ),
        )

        composeRule.onNodeWithText("Top 10").assertDoesNotExist()
        composeRule.onNodeWithText("Top 20").assertDoesNotExist()
    }

    @Test
    fun visibleRangeChipSelectionInvokesCallback() {
        var selectedRange: CategoryDetailVisibleRange? = null
        setContent(
            state =
                CategoryDetailUiState(
                    categoryName = "Cars",
                    attributeSummary = "2 attributes",
                    items =
                        (1..10).map { i ->
                            CategoryDetailItemUiModel(rank = i, id = "Item$i", averageScoreText = "8.0")
                        },
                    isLoading = false,
                    visibleRange = CategoryDetailVisibleRange.Top10,
                    visibleRangeOptions =
                        listOf(
                            CategoryDetailVisibleRange.Top10,
                            CategoryDetailVisibleRange.Top20,
                            CategoryDetailVisibleRange.Top50,
                            CategoryDetailVisibleRange.All,
                        ),
                ),
            onVisibleRangeSelected = { selectedRange = it },
        )

        composeRule.onNodeWithText("Top 20").performClick()

        assertEquals(CategoryDetailVisibleRange.Top20, selectedRange)
    }

    // endregion

    // region Grid View Mode Tests

    @Test
    fun gridViewModeRendersGridCards() {
        setContent(
            CategoryDetailUiState(
                categoryName = "Cars",
                attributeSummary = "2 attributes",
                items =
                    listOf(
                        CategoryDetailItemUiModel(
                            rank = 1,
                            id = "sedan",
                            averageScoreText = "8.7",
                            tierLabel = "A-Tier",
                        ),
                        CategoryDetailItemUiModel(
                            rank = 2,
                            id = "coupe",
                            averageScoreText = "7.4",
                            tierLabel = "B-Tier",
                        ),
                    ),
                isLoading = false,
                viewMode = CategoryDetailViewMode.GRID,
            ),
        )

        composeRule.onNodeWithContentDescription("sedan, A-Tier 8.7").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithContentDescription("coupe, B-Tier 7.4").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun gridViewToggleCallsCallback() {
        var toggled = false
        setContent(
            state =
                CategoryDetailUiState(
                    categoryName = "Cars",
                    attributeSummary = "2 attributes",
                    items =
                        listOf(
                            CategoryDetailItemUiModel(rank = 1, id = "sedan", averageScoreText = "8.7"),
                        ),
                    isLoading = false,
                ),
            onViewModeToggled = { toggled = true },
        )

        composeRule.onNodeWithContentDescription("View as grid").performClick()

        assertTrue(toggled)
    }

    @Test
    fun gridCardRendersNameTierAndScore() {
        setContent(
            CategoryDetailUiState(
                categoryName = "Cars",
                attributeSummary = "2 attributes",
                items =
                    listOf(
                        CategoryDetailItemUiModel(
                            rank = 1,
                            id = "sedan",
                            averageScoreText = "9.2",
                            tierLabel = "S-Tier",
                        ),
                    ),
                isLoading = false,
                viewMode = CategoryDetailViewMode.GRID,
            ),
        )

        composeRule.onNodeWithContentDescription("sedan, S-Tier 9.2").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun viewModeToggleSegmentsAreDisplayed() {
        setContent(
            CategoryDetailUiState(
                categoryName = "Cars",
                attributeSummary = "2 attributes",
                items =
                    listOf(
                        CategoryDetailItemUiModel(rank = 1, id = "sedan", averageScoreText = "8.7"),
                    ),
                isLoading = false,
            ),
        )

        composeRule.onNodeWithContentDescription("View as list").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("View as grid").assertIsDisplayed()
    }

    @Test
    fun viewModeToggleMeetsMinimumTouchTarget() {
        setContent(
            CategoryDetailUiState(
                categoryName = "Cars",
                attributeSummary = "2 attributes",
                items =
                    listOf(
                        CategoryDetailItemUiModel(rank = 1, id = "sedan", averageScoreText = "8.7"),
                    ),
                isLoading = false,
            ),
        )

        composeRule.onNodeWithContentDescription("View as list").assertMinimumTouchTarget()
        composeRule.onNodeWithContentDescription("View as grid").assertMinimumTouchTarget()
    }

    @Test
    fun gridCardGridAttributesAreDisplayed() {
        setContent(
            CategoryDetailUiState(
                categoryName = "Cars",
                attributeSummary = "2 attributes",
                items =
                    listOf(
                        CategoryDetailItemUiModel(
                            rank = 1,
                            id = "sedan",
                            averageScoreText = "8.7",
                            tierLabel = "A-Tier",
                            gridAttributes =
                                listOf(
                                    GridCardAttribute(emoji = "S", label = "Speed", scoreText = "8/10"),
                                ),
                        ),
                    ),
                isLoading = false,
                viewMode = CategoryDetailViewMode.GRID,
            ),
        )

        composeRule.onNodeWithContentDescription("sedan, A-Tier 8.7").performScrollTo().assertIsDisplayed()
    }

    // endregion

    private fun androidx.compose.ui.test.SemanticsNodeInteraction.assertMinimumTouchTarget() {
        assertWidthIsAtLeast(48.dp)
        assertHeightIsAtLeast(48.dp)
    }
}
