package com.juzgon.feature.category

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
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import com.juzgon.domain.Attribute
import com.juzgon.domain.AttributeType
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
    fun validationErrorsAreHiddenBeforeSaveAttempt() {
        setContent(CategoryFormReducer.createState())

        composeRule.onNodeWithText("Category name").assertIsDisplayed()
        composeRule.onNodeWithText("Attribute name").assertIsDisplayed()
        composeRule.onNodeWithText("Save category").assertIsNotEnabled()
        composeRule.onAllNodesWithText("Category name is required").assertCountEquals(0)
        composeRule.onAllNodesWithText("Attribute name is required").assertCountEquals(0)
    }

    @Test
    fun validationErrorsAreVisibleAfterSaveAttempt() {
        setContent(CategoryFormReducer.createState().copy(showValidationErrors = true))

        composeRule.onNodeWithText("Category name is required").assertIsDisplayed()
        composeRule.onNodeWithText("Attribute name is required").performScrollTo().assertIsDisplayed()
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
        composeRule.onNodeWithText("1.5").assertExists()
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

    @Test
    fun createFormExposesAccessibleSemantics() {
        setContent(CategoryFormReducer.createState())

        composeRule.onNodeWithContentDescription("Category name").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Attribute 1 name").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Attribute 1 weight").assertExists()
        composeRule.onNodeWithContentDescription("Add attribute").assertHasClickAction()
        composeRule.onNodeWithContentDescription("Save category").assertIsNotEnabled()
        composeRule.onNodeWithContentDescription("Remove attribute 1").assertHasClickAction()
    }

    @Test
    fun editFormExposesContextualAttributeActionSemantics() {
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

        composeRule.onNodeWithContentDescription("Move Taste down").assertHasClickAction()
        composeRule.onNodeWithContentDescription("Remove Taste").assertHasClickAction()
        composeRule.onNodeWithContentDescription("Move Service up").assertHasClickAction()
        composeRule.onNodeWithContentDescription("Remove Service").assertHasClickAction()
    }

    @Test
    fun formActionsMeetMinimumTouchTargetSize() {
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

        composeRule.onNodeWithContentDescription("Back").assertMinimumTouchTarget()
        composeRule.onNodeWithContentDescription("Add attribute").assertMinimumTouchTarget()
        composeRule.onNodeWithContentDescription("Save category").assertMinimumTouchTarget()
        composeRule.onNodeWithContentDescription("Move Taste down").assertMinimumTouchTarget()
        composeRule.onNodeWithContentDescription("Remove Taste").assertMinimumTouchTarget()
        composeRule.onNodeWithContentDescription("Move Service up").assertMinimumTouchTarget()
        composeRule.onNodeWithContentDescription("Remove Service").assertMinimumTouchTarget()
    }

    @Test
    fun typePickerIsRenderedForEachAttribute() {
        setContent(CategoryFormReducer.createState())

        composeRule.onNodeWithContentDescription("Attribute 1 type").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun skinTypeAttributeDisplaysReadableTypeLabel() {
        setContent(
            CategoryFormUiState(
                name = "People",
                attributes =
                    listOf(
                        CategoryAttributeInput(
                            key = 0L,
                            name = "Skin Type",
                            type = AttributeType.SKIN_TYPE,
                        ),
                    ),
            ),
        )

        composeRule.onNodeWithText("Skin Type").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun requiredToggleIsRenderedForEachAttribute() {
        setContent(CategoryFormReducer.createState())

        composeRule.onNodeWithContentDescription("Attribute 1 required").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun diamondChartControlsRenderForNumericAttributesOnly() {
        setContent(
            CategoryFormUiState(
                name = "Cars",
                attributes =
                    listOf(
                        CategoryAttributeInput(key = 0L, name = "Speed", type = AttributeType.NUMBER),
                        CategoryAttributeInput(key = 1L, name = "Photo", type = AttributeType.IMAGE),
                    ),
            ),
        )

        composeRule.onNodeWithContentDescription("Attribute 1 diamond chart").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Attribute 1 diamond order").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Attribute 2 diamond chart").assertDoesNotExist()
    }

    @Test
    fun typeChangeWarningDialogIsShownWhenFlagSet() {
        setContent(
            CategoryFormUiState(
                name = "Food",
                attributes =
                    listOf(
                        CategoryAttributeInput(
                            key = 0L,
                            name = "Taste",
                            type = AttributeType.NUMBER,
                        ),
                    ),
                showTypeChangeWarning = true,
                pendingTypeChange = AttributeType.DATE,
            ),
        )

        composeRule
            .onNodeWithText("Changing the attribute type may affect existing data. Continue?")
            .assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Confirm type change").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Cancel type change").assertIsDisplayed()
    }

    @Test
    fun attributeDeleteWarningDialogIsShownWhenFlagSet() {
        setContent(
            CategoryFormUiState(
                name = "Food",
                attributes =
                    listOf(CategoryAttributeInput(key = 0L, name = "Taste", type = AttributeType.NUMBER)),
                showAttributeDeleteWarning = true,
            ),
        )

        composeRule
            .onNodeWithText("Deleting this attribute will remove its values from all items. Continue?")
            .assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Confirm delete attribute").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Cancel delete attribute").assertIsDisplayed()
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
                    onDescriptionChange = {},
                    onCatalogTypeChange = {},
                    onAttributeNameChange = { _, _ -> },
                    onAttributeWeightChange = { _, _ -> },
                    onAttributeTypeChange = { _, _ -> },
                    onAttributeRequiredChange = { _, _ -> },
                    onAttributeDisplayInDiamondChange = { _, _ -> },
                    onAttributeDiamondOrderChange = { _, _ -> },
                    onAttributeScoringDirectionChange = { _, _ -> },
                    onAddAttribute = {},
                    onRemoveAttribute = {},
                    onMoveAttributeUp = {},
                    onMoveAttributeDown = {},
                    onTypeChangeConfirmed = {},
                    onTypeChangeDeclined = {},
                    onAttributeDeleteConfirmed = {},
                    onAttributeDeleteDeclined = {},
                    onSaveClick = {},
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
