@file:Suppress("FunctionName")

package com.juzgon.ui.components

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.juzgon.ui.theme.JuzgonTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class JuzgonScoreBarListTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun rendersCorrectNumberOfRows() {
        val items =
            listOf(
                ScoreBarItem(label = "Chest", icon = "\u2B50", value = 10, maxValue = 10),
                ScoreBarItem(label = "Eyes", icon = "\uD83D\uDCA7", value = 9, maxValue = 10),
                ScoreBarItem(label = "Hair", icon = null, value = 6, maxValue = 10),
            )

        composeRule.setContent {
            JuzgonTheme(darkTheme = true, dynamicColor = false) {
                JuzgonScoreBarList(items = items)
            }
        }

        composeRule.onNodeWithText("Chest").assertIsDisplayed()
        composeRule.onNodeWithText("Eyes").assertIsDisplayed()
        composeRule.onNodeWithText("Hair").assertIsDisplayed()
    }

    @Test
    fun eachRowDisplaysCorrectLabelAndScore() {
        val items =
            listOf(
                ScoreBarItem(label = "Chest", icon = "\u2B50", value = 10, maxValue = 10),
                ScoreBarItem(label = "Legs", icon = null, value = 5, maxValue = 10),
            )

        composeRule.setContent {
            JuzgonTheme(darkTheme = true, dynamicColor = false) {
                JuzgonScoreBarList(items = items)
            }
        }

        composeRule.onNodeWithText("Chest").assertIsDisplayed()
        composeRule.onNodeWithText("10/10").assertIsDisplayed()
        composeRule.onNodeWithText("Legs").assertIsDisplayed()
        composeRule.onNodeWithText("5/10").assertIsDisplayed()
    }

    @Test
    fun emptyListRendersNoRows() {
        composeRule.setContent {
            JuzgonTheme(darkTheme = true, dynamicColor = false) {
                JuzgonScoreBarList(items = emptyList())
            }
        }

        composeRule.onNodeWithText("Chest").assertDoesNotExist()
    }

    @Test
    fun headerSlotContentIsDisplayed() {
        val items =
            listOf(
                ScoreBarItem(label = "Chest", icon = null, value = 10, maxValue = 10),
            )

        composeRule.setContent {
            JuzgonTheme(darkTheme = true, dynamicColor = false) {
                JuzgonScoreBarList(
                    items = items,
                    header = { Text("Attributes") },
                )
            }
        }

        composeRule.onNodeWithText("Attributes").assertIsDisplayed()
    }

    @Test
    fun semanticContainerDescribesItemCount() {
        val items =
            listOf(
                ScoreBarItem(label = "Chest", icon = null, value = 10, maxValue = 10),
                ScoreBarItem(label = "Eyes", icon = null, value = 9, maxValue = 10),
                ScoreBarItem(label = "Hair", icon = null, value = 6, maxValue = 10),
            )

        composeRule.setContent {
            JuzgonTheme(darkTheme = true, dynamicColor = false) {
                JuzgonScoreBarList(items = items)
            }
        }

        composeRule
            .onNodeWithContentDescription("Score list, 3 attributes")
            .assertIsDisplayed()
    }
}
