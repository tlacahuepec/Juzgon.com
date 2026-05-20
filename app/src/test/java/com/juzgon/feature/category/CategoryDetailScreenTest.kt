package com.juzgon.feature.category

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
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
    }

    @Test
    fun contentStateRendersItemsAndAverageBadges() {
        setContent(
            CategoryDetailUiState(
                categoryName = "Cars",
                attributeSummary = "4 attributes",
                items =
                    listOf(
                        CategoryDetailItemUiModel(id = "sedan", averageScoreText = "8.7"),
                        CategoryDetailItemUiModel(id = "coupe", averageScoreText = "7.4"),
                    ),
                isLoading = false,
            ),
        )

        composeRule.onNodeWithText("4 attributes").assertIsDisplayed()
        composeRule.onNodeWithText("sedan").assertIsDisplayed()
        composeRule.onNodeWithText("8.7").assertIsDisplayed()
        composeRule.onNodeWithText("coupe").assertIsDisplayed()
        composeRule.onNodeWithText("7.4").assertIsDisplayed()
    }

    @Test
    fun itemRowsExposeAccessibleSummarySemantics() {
        setContent(
            CategoryDetailUiState(
                categoryName = "Cars",
                attributeSummary = "4 attributes",
                items =
                    listOf(
                        CategoryDetailItemUiModel(id = "sedan", averageScoreText = "8.7"),
                    ),
                isLoading = false,
            ),
        )

        composeRule.onNodeWithContentDescription("Rated item sedan, average score 8.7").assertIsDisplayed()
    }

    @Test
    fun detailNavigationAndItemRowsMeetMinimumTouchTargetSize() {
        setContent(
            CategoryDetailUiState(
                categoryName = "Cars",
                attributeSummary = "4 attributes",
                items =
                    listOf(
                        CategoryDetailItemUiModel(id = "sedan", averageScoreText = "8.7"),
                    ),
                isLoading = false,
            ),
        )

        composeRule.onNodeWithContentDescription("Back").assertMinimumTouchTarget()
        composeRule.onNodeWithContentDescription("Rated item sedan, average score 8.7").assertMinimumTouchTarget()
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

    private fun setContent(
        state: CategoryDetailUiState,
        onBackClick: () -> Unit = {},
    ) {
        composeRule.setContent {
            MaterialTheme {
                CategoryDetailScreen(
                    state = state,
                    onBackClick = onBackClick,
                )
            }
        }
    }

    private fun androidx.compose.ui.test.SemanticsNodeInteraction.assertMinimumTouchTarget() {
        assertWidthIsAtLeast(48.dp)
        assertHeightIsAtLeast(48.dp)
    }
}
