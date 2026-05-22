@file:Suppress("LongParameterList")

package com.juzgon.feature.item

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import com.juzgon.domain.AttributeType
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

        composeRule.onNodeWithText("Ranked attributes").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Speed").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("8 / 10").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Brakes").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("7 / 10").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun loadedScreenRendersImageAttributePreview() {
        setContent(
            loadedState().copy(
                attributeValues =
                    listOf(
                        ItemDetailAttributeValue(
                            label = "Photo",
                            value = "content://images/roadster",
                            type = AttributeType.IMAGE,
                        ),
                    ),
            ),
        )

        composeRule.onNodeWithText("Attributes").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Photo image preview").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("content://images/roadster").assertDoesNotExist()
    }

    @Test
    fun loadedScreenRendersProminentPrimaryImage() {
        setContent(loadedState().copy(primaryImageValue = "content://images/roadster"))

        composeRule.onNodeWithContentDescription("Roadster image preview").assertIsDisplayed()
    }

    @Test
    fun loadedScreenRendersPrimaryImagePlaceholderWhenMissing() {
        setContent(loadedState())

        composeRule.onNodeWithContentDescription("Roadster image placeholder").assertIsDisplayed()
    }

    @Test
    fun loadedScreenRendersNonImageAttributeValues() {
        setContent(
            loadedState().copy(
                attributeValues =
                    listOf(
                        ItemDetailAttributeValue(
                            label = "Details",
                            value = "Very fast car",
                            type = AttributeType.NOTES,
                        ),
                    ),
            ),
        )

        composeRule.onNodeWithText("Attributes").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Details").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Very fast car").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun loadedScreenRendersFormattedTypedAttributeValues() {
        setContent(
            loadedState().copy(
                attributeValues =
                    listOf(
                        ItemDetailAttributeValue(
                            label = "Available",
                            value = "true",
                            type = AttributeType.BOOLEAN,
                            displayValue = "Yes",
                        ),
                        ItemDetailAttributeValue(
                            label = "Release",
                            value = "2026-01-02",
                            type = AttributeType.DATE,
                            displayValue = "Jan 2, 2026",
                        ),
                    ),
            ),
        )

        composeRule.onNodeWithText("Yes").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Jan 2, 2026").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun loadedScreenRendersUrlAttributeAsClickable() {
        setContent(
            loadedState().copy(
                attributeValues =
                    listOf(
                        ItemDetailAttributeValue(
                            label = "Website",
                            value = "https://example.com",
                            type = AttributeType.URL,
                        ),
                    ),
            ),
        )

        composeRule
            .onNodeWithContentDescription("Open Website URL")
            .performScrollTo()
            .assertHasClickAction()
    }

    @Test
    fun loadedScreenRendersRankedAttributeCardSemantics() {
        setContent(loadedState())

        composeRule
            .onNodeWithContentDescription("Rank 1, Speed, 8 out of 10, 80 percent")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun loadedScreenRendersMovementIndicators() {
        setContent(
            loadedState().copy(
                rankedAttributes =
                    listOf(
                        RankedAttributeCardUiModel(
                            rank = 1,
                            label = "Speed",
                            valueText = "8",
                            maxText = "10",
                            progressPercent = 80,
                            progressFraction = 0.8f,
                            sizeVariant = AttributeRankSizeVariant.Rank1,
                            movement =
                                AttributeMovement(
                                    rank = AttributeMovementDirection.Improved,
                                    value = AttributeMovementDirection.Declined,
                                ),
                        ),
                        RankedAttributeCardUiModel(
                            rank = 2,
                            label = "Brakes",
                            valueText = "7",
                            maxText = "10",
                            progressPercent = 70,
                            progressFraction = 0.7f,
                            sizeVariant = AttributeRankSizeVariant.Rank2,
                            movement =
                                AttributeMovement(
                                    rank = AttributeMovementDirection.Unchanged,
                                    value = AttributeMovementDirection.Unchanged,
                                ),
                        ),
                    ),
            ),
        )

        composeRule.onNodeWithText("Rank ↑").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Value ↓").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Rank =").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Value =").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun loadedScreenOmitsMovementIndicatorsWithoutHistory() {
        setContent(loadedState())

        composeRule.onNodeWithText("Rank ↑").assertDoesNotExist()
        composeRule.onNodeWithText("Rank ↓").assertDoesNotExist()
        composeRule.onNodeWithText("Rank =").assertDoesNotExist()
        composeRule.onNodeWithText("Value ↑").assertDoesNotExist()
        composeRule.onNodeWithText("Value ↓").assertDoesNotExist()
        composeRule.onNodeWithText("Value =").assertDoesNotExist()
    }

    @Test
    fun loadedScreenAppliesRankedAttributeSizeVariants() {
        setContent(loadedState())

        composeRule.onNodeWithTag("RankedAttributeCard:Rank1:1").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("RankedAttributeCard:Rank2:2").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun notesShownWhenPresent() {
        setContent(loadedState().copy(notes = "weekend car"))

        composeRule.onNodeWithText("weekend car").performScrollTo().assertIsDisplayed()
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
    fun deleteButtonInvokesCallback() {
        var deleteClicked = false
        setContent(loadedState(), onDeleteClick = { deleteClicked = true })

        composeRule.onNodeWithContentDescription("Delete item").performClick()

        assertTrue(deleteClicked)
    }

    @Test
    fun deleteDialogIsShownWhenFlagSet() {
        setContent(loadedState().copy(showDeleteConfirmDialog = true))

        composeRule.onNodeWithContentDescription("Confirm delete item").assertIsDisplayed()
    }

    @Test
    fun confirmDeleteInvokesCallback() {
        var deleteConfirmed = false
        setContent(
            loadedState().copy(showDeleteConfirmDialog = true),
            onDeleteConfirmed = { deleteConfirmed = true },
        )

        composeRule.onNodeWithContentDescription("Confirm delete item").performClick()

        assertTrue(deleteConfirmed)
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
        composeRule.onNodeWithContentDescription("Delete item").assertMinimumTouchTarget()
    }

    private fun setContent(
        state: ItemDetailUiState,
        onBackClick: () -> Unit = {},
        onEditClick: () -> Unit = {},
        onDeleteClick: () -> Unit = {},
        onDeleteConfirmed: () -> Unit = {},
        onDeleteDialogDismissed: () -> Unit = {},
    ) {
        composeRule.setContent {
            MaterialTheme {
                ItemDetailScreen(
                    state = state,
                    onBackClick = onBackClick,
                    onEditClick = onEditClick,
                    onDeleteClick = onDeleteClick,
                    onDeleteConfirmed = onDeleteConfirmed,
                    onDeleteDialogDismissed = onDeleteDialogDismissed,
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
            rankedAttributes =
                listOf(
                    RankedAttributeCardUiModel(
                        rank = 1,
                        label = "Speed",
                        valueText = "8",
                        maxText = "10",
                        progressPercent = 80,
                        progressFraction = 0.8f,
                        sizeVariant = AttributeRankSizeVariant.Rank1,
                    ),
                    RankedAttributeCardUiModel(
                        rank = 2,
                        label = "Brakes",
                        valueText = "7",
                        maxText = "10",
                        progressPercent = 70,
                        progressFraction = 0.7f,
                        sizeVariant = AttributeRankSizeVariant.Rank2,
                    ),
                ),
            notes = "",
            isLoading = false,
        )

    private fun androidx.compose.ui.test.SemanticsNodeInteraction.assertMinimumTouchTarget() {
        assertWidthIsAtLeast(48.dp)
        assertHeightIsAtLeast(48.dp)
    }
}
