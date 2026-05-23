@file:Suppress("LongParameterList")

package com.juzgon.feature.category

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
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
                )
            }
        }
    }

    private fun androidx.compose.ui.test.SemanticsNodeInteraction.assertMinimumTouchTarget() {
        assertWidthIsAtLeast(48.dp)
        assertHeightIsAtLeast(48.dp)
    }
}
