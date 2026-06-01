package com.juzgon.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.assertHasClickAction
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
class JuzgonCollectionCardTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun rendersImageSlotAndMetadataInStableClickableShell() {
        composeRule.setContent {
            MaterialTheme {
                JuzgonCollectionCard(
                    metadata =
                        JuzgonCollectionCardMetadata(
                            title = "  Grand    Touring   Sedan  ",
                            rankLabel = "#1",
                            metric = JuzgonCollectionCardMetric(label = "Score", value = "8.7"),
                            badge = "US",
                            contentDescription = "Rated item Grand Touring Sedan, rank 1, Score 8.7",
                        ),
                    onClick = {},
                    visualContent = {
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxSize()
                                    .semantics { contentDescription = "sedan image preview" },
                        )
                    },
                )
            }
        }

        composeRule.onNodeWithContentDescription("sedan image preview").assertIsDisplayed()
        composeRule.onNodeWithText("Grand").assertIsDisplayed()
        composeRule.onNodeWithText("Touring Sedan").assertIsDisplayed()
        composeRule.onNodeWithText("#1").assertIsDisplayed()
        composeRule.onNodeWithText("Score").assertIsDisplayed()
        composeRule.onNodeWithText("8.7").assertIsDisplayed()
        composeRule.onNodeWithText("US").assertIsDisplayed()
        composeRule
            .onNodeWithContentDescription("Rated item Grand Touring Sedan, rank 1, Score 8.7")
            .assertHasClickAction()
            .assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun rendersFallbackVisualSlotAndInvokesClick() {
        var clicked = false
        composeRule.setContent {
            MaterialTheme {
                JuzgonCollectionCard(
                    metadata =
                        JuzgonCollectionCardMetadata(
                            title = "Coupe",
                            rankLabel = "#2",
                            metric = JuzgonCollectionCardMetric(label = "Speed", value = "9"),
                            contentDescription = "Rated item Coupe, rank 2, Speed 9",
                        ),
                    onClick = { clicked = true },
                    visualContent = {
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxSize()
                                    .semantics { contentDescription = "coupe image placeholder" },
                        )
                    },
                )
            }
        }

        composeRule.onNodeWithContentDescription("coupe image placeholder").assertIsDisplayed()
        composeRule.onNodeWithText("Coupe").assertIsDisplayed()
        composeRule.onNodeWithText("Speed").assertIsDisplayed()
        composeRule.onNodeWithText("9").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Rated item Coupe, rank 2, Speed 9").performClick()

        assertTrue(clicked)
    }
}
