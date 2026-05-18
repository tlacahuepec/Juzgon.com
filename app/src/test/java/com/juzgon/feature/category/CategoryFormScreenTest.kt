package com.juzgon.feature.category

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.juzgon.domain.Attribute
import com.juzgon.domain.Category
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class CategoryFormScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun createFormRendersEmptyState() {
        setContent(CategoryFormReducer.createState())

        composeRule.onNodeWithText("Create category").assertIsDisplayed()
        composeRule.onNodeWithText("Category name").assertIsDisplayed()
        composeRule.onNodeWithText("Attributes").assertIsDisplayed()
        composeRule.onAllNodesWithText("Add attribute").assertCountEquals(1)
        composeRule.onNodeWithText("Save category").assertIsNotEnabled()
    }

    @Test
    fun validationErrorsAreVisible() {
        setContent(CategoryFormReducer.createState())

        composeRule.onNodeWithText("Category name is required").assertIsDisplayed()
        composeRule.onNodeWithText("Attribute name is required").assertIsDisplayed()
    }

    @Test
    fun editFormRendersExistingContent() {
        setContent(
            CategoryFormReducer.editState(
                Category(
                    name = "Food",
                    attributes =
                        listOf(
                            Attribute(id = "Taste", weight = 1.5),
                            Attribute(id = "Service", weight = 1.0),
                        ),
                ),
            ),
        )

        composeRule.onNodeWithText("Edit category").assertIsDisplayed()
        composeRule.onNodeWithText("Food").assertIsDisplayed()
        composeRule.onNodeWithText("Taste").assertIsDisplayed()
        composeRule.onNodeWithText("1.5").assertIsDisplayed()
        composeRule.onAllNodesWithText("Service").assertCountEquals(1)
    }

    @Test
    fun saveButtonIsDisabledWhenInvalid() {
        setContent(CategoryFormReducer.createState())

        composeRule.onNodeWithText("Save category").assertIsNotEnabled()
    }

    @Test
    fun saveButtonIsEnabledWhenValid() {
        setContent(
            CategoryFormUiState(
                name = "Food",
                attributes = listOf(CategoryAttributeInput(key = 0L, name = "Taste")),
            ),
        )
        composeRule.onNodeWithText("Save category").assertIsEnabled()
    }

    @Test
    fun backButtonInvokesCallback() {
        var backClicked = false
        setContent(
            state = CategoryFormReducer.createState(),
            onBackClick = { backClicked = true },
        )

        composeRule.onNodeWithContentDescription("Back").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Back").performClick()

        assertTrue(backClicked)
    }

    private fun setContent(
        state: CategoryFormUiState,
        onBackClick: () -> Unit = {},
    ) {
        composeRule.setContent {
            MaterialTheme {
                CategoryFormScreen(
                    state = state,
                    onNameChange = {},
                    onAttributeNameChange = { _, _ -> },
                    onAttributeWeightChange = { _, _ -> },
                    onAddAttribute = {},
                    onRemoveAttribute = {},
                    onMoveAttributeUp = {},
                    onMoveAttributeDown = {},
                    onSaveClick = {},
                    onBackClick = onBackClick,
                )
            }
        }
    }
}
