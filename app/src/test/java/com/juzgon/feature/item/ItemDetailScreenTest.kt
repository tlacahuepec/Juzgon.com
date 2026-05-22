package com.juzgon.feature.item

import androidx.compose.material3.MaterialTheme
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

        composeRule.onNodeWithText("Ranked attributes").assertIsDisplayed()
        composeRule.onNodeWithText("Speed").assertIsDisplayed()
        composeRule.onNodeWithText("8 / 10").assertIsDisplayed()
        composeRule.onNodeWithText("Brakes").assertIsDisplayed()
        composeRule.onNodeWithText("7 / 10").assertIsDisplayed()
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
    fun loadedScreenRendersRankedAttributeCardSemantics() {
        setContent(loadedState())

        composeRule
            .onNodeWithContentDescription("Rank 1, Speed, 8 out of 10, 80 percent")
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

        composeRule.onNodeWithText("Rank ↑").assertIsDisplayed()
        composeRule.onNodeWithText("Value ↓").assertIsDisplayed()
        composeRule.onNodeWithText("Rank =").assertIsDisplayed()
        composeRule.onNodeWithText("Value =").assertIsDisplayed()
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

        composeRule.onNodeWithTag("RankedAttributeCard:Rank1:1").assertIsDisplayed()
        composeRule.onNodeWithTag("RankedAttributeCard:Rank2:2").assertIsDisplayed()
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
