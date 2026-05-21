package com.juzgon.feature.item

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.juzgon.domain.Attribute
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ItemFormScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun loadedFormRendersTitleNotesAndScoreInputs() {
        setContent(loadedState())

        composeRule.onNodeWithText("Add item").assertIsDisplayed()
        composeRule.onNodeWithText("Cars").assertIsDisplayed()
        composeRule.onNodeWithText("Item title").assertIsDisplayed()
        composeRule.onNodeWithText("Notes").assertIsDisplayed()
        composeRule.onNodeWithText("Speed score").assertIsDisplayed()
        composeRule.onNodeWithText("Brakes score").assertIsDisplayed()
    }

    @Test
    fun editFormRendersPrefilledContent() {
        setContent(
            loadedState().copy(
                mode = ItemFormMode.Edit,
                title = "Roadster",
                notes = "weekend car",
                scores =
                    listOf(
                        ItemScoreInput(Attribute("Speed"), "6"),
                        ItemScoreInput(Attribute("Brakes"), "10"),
                    ),
            ),
        )

        composeRule.onNodeWithText("Edit item").assertIsDisplayed()
        composeRule.onNodeWithText("Roadster").assertIsDisplayed()
        composeRule.onNodeWithText("weekend car").assertIsDisplayed()
        composeRule.onNodeWithText("6").assertIsDisplayed()
        composeRule.onNodeWithText("10").assertIsDisplayed()
    }

    @Test
    fun saveButtonIsDisabledWhenInvalid() {
        setContent(loadedState())

        composeRule.onNodeWithText("Save item").assertIsNotEnabled()
    }

    @Test
    fun saveButtonIsEnabledWhenValid() {
        setContent(
            loadedState().copy(
                title = "Roadster",
                scores =
                    listOf(
                        ItemScoreInput(Attribute("Speed"), "1"),
                        ItemScoreInput(Attribute("Brakes"), "10"),
                    ),
            ),
        )
        composeRule.onNodeWithText("Save item").assertIsEnabled()
    }

    @Test
    fun validationErrorsRenderAfterSaveAttempt() {
        setContent(loadedState().copy(showValidationErrors = true))

        composeRule.onNodeWithText("Title is required").assertIsDisplayed()
        composeRule.onAllNodesWithText("Score is required").assertCountEquals(2)
    }

    @Test
    fun formExposesAccessibleSemantics() {
        setContent(loadedState())

        composeRule.onNodeWithContentDescription("Back").assertHasClickAction()
        composeRule.onNodeWithContentDescription("Item title").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Item notes").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Speed score").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Brakes score").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Save item").assertIsNotEnabled()
    }

    @Test
    fun formActionsMeetMinimumTouchTargetSize() {
        setContent(loadedState())

        composeRule.onNodeWithContentDescription("Back").assertMinimumTouchTarget()
        composeRule.onNodeWithContentDescription("Save item").assertMinimumTouchTarget()
    }

    @Test
    fun backButtonInvokesCallback() {
        var backClicked = false
        setContent(loadedState(), onBackClick = { backClicked = true })

        composeRule.onNodeWithContentDescription("Back").performClick()

        assertTrue(backClicked)
    }

    @Test
    fun deleteButtonNotShownInCreateMode() {
        setContent(loadedState())

        composeRule.onNodeWithContentDescription("Delete item").assertDoesNotExist()
    }

    @Test
    fun deleteButtonShownInEditMode() {
        setContent(loadedState().copy(mode = ItemFormMode.Edit, originalItemId = "Roadster"))

        composeRule.onNodeWithContentDescription("Delete item").assertIsDisplayed()
    }

    @Test
    fun deleteButtonMeetsMinimumTouchTargetSize() {
        setContent(loadedState().copy(mode = ItemFormMode.Edit, originalItemId = "Roadster"))

        composeRule.onNodeWithContentDescription("Delete item").assertMinimumTouchTarget()
    }

    @Test
    fun deleteButtonInvokesCallback() {
        var deleteClicked = false
        setContent(
            loadedState().copy(mode = ItemFormMode.Edit, originalItemId = "Roadster"),
            onDeleteClick = { deleteClicked = true },
        )

        composeRule.onNodeWithContentDescription("Delete item").performClick()

        assertTrue(deleteClicked)
    }

    @Test
    fun confirmationDialogNotShownByDefault() {
        setContent(loadedState().copy(mode = ItemFormMode.Edit, originalItemId = "Roadster"))

        composeRule.onNodeWithText("Delete item?").assertDoesNotExist()
    }

    @Test
    fun confirmationDialogShownWhenShowDeleteDialogIsTrue() {
        setContent(
            loadedState().copy(mode = ItemFormMode.Edit, originalItemId = "Roadster", showDeleteDialog = true),
        )

        composeRule.onNodeWithText("Delete item?").assertIsDisplayed()
        composeRule.onNodeWithText("Delete").assertIsDisplayed()
        composeRule.onNodeWithText("Cancel").assertIsDisplayed()
    }

    @Test
    fun dialogCancelInvokesCallback() {
        var cancelClicked = false
        setContent(
            loadedState().copy(mode = ItemFormMode.Edit, originalItemId = "Roadster", showDeleteDialog = true),
            onDeleteCancel = { cancelClicked = true },
        )

        composeRule.onNodeWithText("Cancel").performClick()

        assertTrue(cancelClicked)
    }

    @Test
    fun dialogConfirmInvokesCallback() {
        var confirmClicked = false
        setContent(
            loadedState().copy(mode = ItemFormMode.Edit, originalItemId = "Roadster", showDeleteDialog = true),
            onDeleteConfirm = { confirmClicked = true },
        )

        composeRule.onNodeWithText("Delete").performClick()

        assertTrue(confirmClicked)
    }

    private fun setContent(
        state: ItemFormUiState,
        onBackClick: () -> Unit = {},
        onDeleteClick: () -> Unit = {},
        onDeleteCancel: () -> Unit = {},
        onDeleteConfirm: () -> Unit = {},
    ) {
        composeRule.setContent {
            MaterialTheme {
                ItemFormScreen(
                    state = state,
                    onTitleChange = {},
                    onNotesChange = {},
                    onScoreChange = { _, _ -> },
                    onSaveClick = {},
                    onBackClick = onBackClick,
                    onDeleteClick = onDeleteClick,
                    onDeleteCancel = onDeleteCancel,
                    onDeleteConfirm = onDeleteConfirm,
                )
            }
        }
    }

    private fun loadedState(): ItemFormUiState =
        ItemFormUiState(
            categoryName = "Cars",
            scores =
                listOf(
                    ItemScoreInput(Attribute("Speed")),
                    ItemScoreInput(Attribute("Brakes")),
                ),
            isLoading = false,
        )

    private fun androidx.compose.ui.test.SemanticsNodeInteraction.assertMinimumTouchTarget() {
        assertWidthIsAtLeast(48.dp)
        assertHeightIsAtLeast(48.dp)
    }
}
