package com.juzgon.feature.item

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
class ItemDetailScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun loadedScreenRendersItemIdAndOverallScore() {
        setContent(loadedState())

        composeRule.onNodeWithText("Roadster").assertIsDisplayed()
        composeRule.onNodeWithText("7.5").assertIsDisplayed()
    }

    @Test
    fun loadedScreenRendersAttributeScoreRows() {
        setContent(loadedState())

        composeRule.onNodeWithText("Speed").assertIsDisplayed()
        composeRule.onNodeWithText("8").assertIsDisplayed()
        composeRule.onNodeWithText("Brakes").assertIsDisplayed()
        composeRule.onNodeWithText("7").assertIsDisplayed()
    }

    @Test
    fun notesShownWhenPresent() {
        setContent(loadedState().copy(notes = "weekend car"))

        composeRule.onNodeWithText("weekend car").assertIsDisplayed()
    }

    @Test
    fun notesSectionHiddenWhenEmpty() {
        setContent(loadedState().copy(notes = ""))

        composeRule.onNodeWithContentDescription("Notes").assertDoesNotExist()
    }

    @Test
    fun editButtonIsDisplayed() {
        setContent(loadedState())

        composeRule.onNodeWithContentDescription("Edit item").assertIsDisplayed()
    }

    @Test
    fun editButtonInvokesCallback() {
        var editClicked = false
        setContent(loadedState(), onEditClick = { editClicked = true })

        composeRule.onNodeWithContentDescription("Edit item").performClick()

        assertTrue(editClicked)
    }

    @Test
    fun backButtonInvokesCallback() {
        var backClicked = false
        setContent(loadedState(), onBackClick = { backClicked = true })

        composeRule.onNodeWithContentDescription("Back").performClick()

        assertTrue(backClicked)
    }

    @Test
    fun screenActionsMeetMinimumTouchTargetSize() {
        setContent(loadedState())

        composeRule.onNodeWithContentDescription("Back").assertMinimumTouchTarget()
        composeRule.onNodeWithContentDescription("Edit item").assertMinimumTouchTarget()
    }

    private fun setContent(
        state: ItemDetailUiState,
        onBackClick: () -> Unit = {},
        onEditClick: () -> Unit = {},
    ) {
        composeRule.setContent {
            MaterialTheme {
                ItemDetailScreen(
                    state = state,
                    onBackClick = onBackClick,
                    onEditClick = onEditClick,
                )
            }
        }
    }

    private fun loadedState(): ItemDetailUiState =
        ItemDetailUiState(
            itemId = "Roadster",
            overallScoreText = "7.5",
            attributeScores =
                listOf(
                    ItemDetailAttributeScore(label = "Speed", score = 8),
                    ItemDetailAttributeScore(label = "Brakes", score = 7),
                ),
            notes = "",
            isLoading = false,
        )

    private fun androidx.compose.ui.test.SemanticsNodeInteraction.assertMinimumTouchTarget() {
        assertWidthIsAtLeast(48.dp)
        assertHeightIsAtLeast(48.dp)
    }
}
