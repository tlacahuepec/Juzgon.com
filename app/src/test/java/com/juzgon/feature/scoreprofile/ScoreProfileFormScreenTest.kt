package com.juzgon.feature.scoreprofile

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.juzgon.domain.AttributeType
import com.juzgon.domain.ScoringDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ScoreProfileFormScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun createFormRendersEmptyState() {
        setContent(defaultState())

        composeRule.onNodeWithText("Create score profile").assertIsDisplayed()
        composeRule.onNodeWithText("Profile name").assertIsDisplayed()
        composeRule.onNodeWithText("Save").assertIsNotEnabled()
    }

    @Test
    fun rendersAttributeCheckboxes() {
        setContent(
            defaultState().copy(
                attributes =
                    listOf(
                        RankableAttributeCheckbox(
                            id = "taste",
                            name = "taste",
                            type = AttributeType.NUMBER,
                        ),
                        RankableAttributeCheckbox(
                            id = "released",
                            name = "released",
                            type = AttributeType.DATE,
                            scoringDirection = ScoringDirection.NEWER_IS_BETTER,
                        ),
                    ),
            ),
        )

        composeRule.onNodeWithText("taste").assertIsDisplayed()
        composeRule.onNodeWithText("released").assertIsDisplayed()
        composeRule.onNodeWithText("Number").assertIsDisplayed()
        composeRule.onNodeWithText("Date · Newer is better").assertIsDisplayed()
    }

    @Test
    fun selectedAttributeShowsCheckedState() {
        setContent(
            defaultState().copy(
                attributes =
                    listOf(
                        RankableAttributeCheckbox(
                            id = "taste",
                            name = "taste",
                            type = AttributeType.NUMBER,
                            isSelected = true,
                        ),
                        RankableAttributeCheckbox(
                            id = "service",
                            name = "service",
                            type = AttributeType.NUMBER,
                            isSelected = false,
                        ),
                    ),
            ),
        )

        composeRule.onNodeWithContentDescription("Toggle taste").assertIsOn()
        composeRule.onNodeWithContentDescription("Toggle service").assertIsOff()
    }

    @Test
    fun selectionCountUpdates() {
        setContent(
            defaultState().copy(
                profileName = "Test",
                attributes =
                    listOf(
                        RankableAttributeCheckbox(
                            id = "taste",
                            name = "taste",
                            type = AttributeType.NUMBER,
                            isSelected = true,
                        ),
                        RankableAttributeCheckbox(
                            id = "service",
                            name = "service",
                            type = AttributeType.NUMBER,
                            isSelected = true,
                        ),
                        RankableAttributeCheckbox(
                            id = "ambiance",
                            name = "ambiance",
                            type = AttributeType.NUMBER,
                            isSelected = false,
                        ),
                    ),
            ),
        )

        composeRule.onNodeWithText("2 of 3 attributes selected").assertIsDisplayed()
    }

    @Test
    fun nameValidationErrorShownAfterSaveAttempt() {
        setContent(
            defaultState().copy(
                showValidationErrors = true,
                attributes =
                    listOf(
                        RankableAttributeCheckbox(
                            id = "taste",
                            name = "taste",
                            type = AttributeType.NUMBER,
                            isSelected = true,
                        ),
                    ),
            ),
        )

        composeRule.onNodeWithText("Profile name is required").assertIsDisplayed()
    }

    @Test
    fun selectionValidationErrorShownAfterSaveAttempt() {
        setContent(
            defaultState().copy(
                profileName = "Test",
                showValidationErrors = true,
                attributes =
                    listOf(
                        RankableAttributeCheckbox(
                            id = "taste",
                            name = "taste",
                            type = AttributeType.NUMBER,
                            isSelected = false,
                        ),
                    ),
            ),
        )

        composeRule.onNodeWithText("Select at least one attribute").assertIsDisplayed()
    }

    @Test
    fun validationErrorsHiddenBeforeSaveAttempt() {
        setContent(defaultState())

        composeRule.onAllNodesWithText("Profile name is required").assertCountEquals(0)
        composeRule.onAllNodesWithText("Select at least one attribute").assertCountEquals(0)
    }

    @Test
    fun saveButtonEnabledWhenValid() {
        setContent(
            defaultState().copy(
                profileName = "Physical Only",
                attributes =
                    listOf(
                        RankableAttributeCheckbox(
                            id = "taste",
                            name = "taste",
                            type = AttributeType.NUMBER,
                            isSelected = true,
                        ),
                    ),
            ),
        )

        composeRule.onNodeWithText("Save").assertIsEnabled()
    }

    @Test
    fun saveButtonDisabledWhileSaving() {
        setContent(
            defaultState().copy(
                profileName = "Physical Only",
                isSaving = true,
                attributes =
                    listOf(
                        RankableAttributeCheckbox(
                            id = "taste",
                            name = "taste",
                            type = AttributeType.NUMBER,
                            isSelected = true,
                        ),
                    ),
            ),
        )

        composeRule.onNodeWithText("Saving…").assertIsNotEnabled()
    }

    @Test
    fun toggleCallbackInvoked() {
        var toggledId = ""
        var toggledValue = false
        setContent(
            state =
                defaultState().copy(
                    attributes =
                        listOf(
                            RankableAttributeCheckbox(
                                id = "taste",
                                name = "taste",
                                type = AttributeType.NUMBER,
                            ),
                        ),
                ),
            onAttributeToggled = { id, selected ->
                toggledId = id
                toggledValue = selected
            },
        )

        composeRule.onNodeWithContentDescription("Toggle taste").performClick()

        assertEquals("taste", toggledId)
        assertTrue(toggledValue)
    }

    @Test
    fun backButtonInvokesCallback() {
        var backClicked = false
        setContent(
            state = defaultState(),
            onBackClick = { backClicked = true },
        )

        composeRule.onNodeWithContentDescription("Back").performClick()
        assertTrue(backClicked)
    }

    @Test
    fun editModeShowsEditTitle() {
        setContent(
            defaultState().copy(mode = ScoreProfileFormMode.Edit),
        )

        composeRule.onNodeWithText("Edit score profile").assertIsDisplayed()
    }

    @Test
    fun errorMessageIsDisplayed() {
        setContent(
            defaultState().copy(errorMessage = "A profile named 'X' already exists"),
        )

        composeRule.onNodeWithText("A profile named 'X' already exists").assertIsDisplayed()
    }

    @Test
    fun formActionsMeetMinimumTouchTargetSize() {
        setContent(
            defaultState().copy(
                attributes =
                    listOf(
                        RankableAttributeCheckbox(
                            id = "taste",
                            name = "taste",
                            type = AttributeType.NUMBER,
                        ),
                    ),
            ),
        )

        composeRule.onNodeWithContentDescription("Back").assertMinimumTouchTarget()
        composeRule.onNodeWithContentDescription("Toggle taste").assertMinimumTouchTarget()
        composeRule.onNodeWithContentDescription("Save").assertMinimumTouchTarget()
    }

    private fun defaultState() =
        ScoreProfileFormUiState(
            isLoading = false,
            categoryName = "Food",
        )

    private fun setContent(
        state: ScoreProfileFormUiState,
        onBackClick: () -> Unit = {},
        onNameChange: (String) -> Unit = {},
        onAttributeToggled: (String, Boolean) -> Unit = { _, _ -> },
        onSaveClick: () -> Unit = {},
    ) {
        composeRule.setContent {
            MaterialTheme {
                ScoreProfileFormScreen(
                    state = state,
                    onNameChange = onNameChange,
                    onAttributeToggled = onAttributeToggled,
                    onSaveClick = onSaveClick,
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
