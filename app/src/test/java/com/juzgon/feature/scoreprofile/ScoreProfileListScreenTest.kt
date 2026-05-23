@file:Suppress("LongParameterList")

package com.juzgon.feature.scoreprofile

import androidx.compose.material3.MaterialTheme
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
class ScoreProfileListScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun rendersProfileCards() {
        setContent(
            ScoreProfileListUiState(
                isLoading = false,
                categoryName = "Food",
                profiles =
                    listOf(
                        ScoreProfileSummary(id = "p1", name = "Physical Only", attributeCount = 3),
                        ScoreProfileSummary(id = "p2", name = "Service Focus", attributeCount = 1),
                    ),
            ),
        )

        composeRule.onNodeWithText("Physical Only").assertIsDisplayed()
        composeRule.onNodeWithText("3 attributes").assertIsDisplayed()
        composeRule.onNodeWithText("Service Focus").assertIsDisplayed()
        composeRule.onNodeWithText("1 attribute").assertIsDisplayed()
    }

    @Test
    fun emptyStateShown() {
        setContent(
            ScoreProfileListUiState(
                isLoading = false,
                categoryName = "Food",
                profiles = emptyList(),
            ),
        )

        composeRule.onNodeWithText("No score profiles yet").assertIsDisplayed()
    }

    @Test
    fun deleteConfirmationDialogShown() {
        setContent(
            ScoreProfileListUiState(
                isLoading = false,
                categoryName = "Food",
                profiles =
                    listOf(
                        ScoreProfileSummary(id = "p1", name = "Physical Only", attributeCount = 3),
                    ),
                showDeleteDialog = true,
                profileToDelete = "p1",
            ),
        )

        composeRule.onNodeWithText("Delete score profile?").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Confirm delete").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Cancel delete").assertIsDisplayed()
    }

    @Test
    fun createButtonInvokesCallback() {
        var createClicked = false
        setContent(
            state = ScoreProfileListUiState(isLoading = false, categoryName = "Food"),
            onCreateClick = { createClicked = true },
        )

        composeRule.onNodeWithContentDescription("Create score profile").performClick()
        assertTrue(createClicked)
    }

    @Test
    fun editButtonInvokesCallback() {
        var editedId = ""
        setContent(
            state =
                ScoreProfileListUiState(
                    isLoading = false,
                    categoryName = "Food",
                    profiles =
                        listOf(
                            ScoreProfileSummary(id = "p1", name = "Physical Only", attributeCount = 2),
                        ),
                ),
            onEditClick = { editedId = it },
        )

        composeRule.onNodeWithContentDescription("Edit Physical Only").performClick()
        assertEquals("p1", editedId)
    }

    @Test
    fun deleteButtonInvokesCallback() {
        var deletedId = ""
        setContent(
            state =
                ScoreProfileListUiState(
                    isLoading = false,
                    categoryName = "Food",
                    profiles =
                        listOf(
                            ScoreProfileSummary(id = "p1", name = "Physical Only", attributeCount = 2),
                        ),
                ),
            onDeleteRequest = { deletedId = it },
        )

        composeRule.onNodeWithContentDescription("Delete Physical Only").performClick()
        assertEquals("p1", deletedId)
    }

    @Test
    fun backButtonInvokesCallback() {
        var backClicked = false
        setContent(
            state = ScoreProfileListUiState(isLoading = false, categoryName = "Food"),
            onBackClick = { backClicked = true },
        )

        composeRule.onNodeWithContentDescription("Back").performClick()
        assertTrue(backClicked)
    }

    @Test
    fun actionsMeetMinimumTouchTargetSize() {
        setContent(
            ScoreProfileListUiState(
                isLoading = false,
                categoryName = "Food",
                profiles =
                    listOf(
                        ScoreProfileSummary(id = "p1", name = "Physical Only", attributeCount = 2),
                    ),
            ),
        )

        composeRule.onNodeWithContentDescription("Back").assertMinimumTouchTarget()
        composeRule.onNodeWithContentDescription("Create score profile").assertMinimumTouchTarget()
        composeRule.onNodeWithContentDescription("Edit Physical Only").assertMinimumTouchTarget()
        composeRule.onNodeWithContentDescription("Delete Physical Only").assertMinimumTouchTarget()
    }

    private fun setContent(
        state: ScoreProfileListUiState,
        onBackClick: () -> Unit = {},
        onCreateClick: () -> Unit = {},
        onEditClick: (String) -> Unit = {},
        onDeleteRequest: (String) -> Unit = {},
        onDeleteConfirmed: () -> Unit = {},
        onDeleteDismissed: () -> Unit = {},
    ) {
        composeRule.setContent {
            MaterialTheme {
                ScoreProfileListScreen(
                    state = state,
                    onBackClick = onBackClick,
                    onCreateClick = onCreateClick,
                    onEditClick = onEditClick,
                    onDeleteRequest = onDeleteRequest,
                    onDeleteConfirmed = onDeleteConfirmed,
                    onDeleteDismissed = onDeleteDismissed,
                )
            }
        }
    }

    private fun androidx.compose.ui.test.SemanticsNodeInteraction.assertMinimumTouchTarget() {
        assertWidthIsAtLeast(48.dp)
        assertHeightIsAtLeast(48.dp)
    }
}
