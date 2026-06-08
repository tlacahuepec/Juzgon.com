@file:Suppress("FunctionName")

package com.juzgon.ui.components

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import com.juzgon.ui.theme.JuzgonTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class JuzgonScoreRowTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun rendersLabelAndScoreText() {
        composeRule.setContent {
            JuzgonTheme(darkTheme = true, dynamicColor = false) {
                JuzgonScoreRow(label = "Chest", value = 10, maxValue = 10)
            }
        }

        composeRule.onNodeWithText("Chest").assertIsDisplayed()
        composeRule.onNodeWithText("10/10").assertIsDisplayed()
    }

    @Test
    fun iconSlotContentDisplayedWhenProvided() {
        composeRule.setContent {
            JuzgonTheme(darkTheme = true, dynamicColor = false) {
                JuzgonScoreRow(
                    label = "Chest",
                    value = 10,
                    maxValue = 10,
                    icon = { Text("\u2B50") },
                )
            }
        }

        composeRule.onNodeWithText("\u2B50").assertIsDisplayed()
    }

    @Test
    fun iconSlotAbsentWhenNotProvided() {
        composeRule.setContent {
            JuzgonTheme(darkTheme = true, dynamicColor = false) {
                JuzgonScoreRow(label = "Arms", value = 7, maxValue = 10)
            }
        }

        composeRule.onNodeWithText("Arms").assertIsDisplayed()
        composeRule.onNodeWithText("7/10").assertIsDisplayed()
    }

    @Test
    fun gradientBarRendersWithCorrectProgress() {
        composeRule.setContent {
            JuzgonTheme(darkTheme = true, dynamicColor = false) {
                JuzgonScoreRow(label = "Chest", value = 10, maxValue = 10)
            }
        }

        composeRule.onNodeWithContentDescription("Score: 100%").assertIsDisplayed()
    }

    @Test
    fun mergedSemanticDescriptionMatchesFormat() {
        composeRule.setContent {
            JuzgonTheme(darkTheme = true, dynamicColor = false) {
                JuzgonScoreRow(label = "Chest", value = 10, maxValue = 10)
            }
        }

        composeRule
            .onNodeWithContentDescription("Chest, score 10 out of 10")
            .assertIsDisplayed()
    }

    @Test
    fun rowHasMinimum48dpHeight() {
        composeRule.setContent {
            JuzgonTheme(darkTheme = true, dynamicColor = false) {
                JuzgonScoreRow(label = "Legs", value = 5, maxValue = 10)
            }
        }

        composeRule
            .onNodeWithContentDescription("Legs, score 5 out of 10")
            .assertHeightIsAtLeast(48.dp)
    }
}
