@file:Suppress("FunctionName")

package com.juzgon.ui.components

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.juzgon.ui.theme.JuzgonTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class JuzgonHeroCardTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun rendersTitleAndScorePill() {
        composeRule.setContent {
            JuzgonTheme(darkTheme = true, dynamicColor = false) {
                JuzgonHeroCard(
                    title = "Top Ranked",
                    tierLabel = "S-Tier",
                    scoreText = "9.4/10",
                    onClick = {},
                    image = { Text("IMG") },
                )
            }
        }

        composeRule.onNodeWithText("Top Ranked").assertIsDisplayed()
        composeRule.onNodeWithText("S-Tier").assertIsDisplayed()
        composeRule.onNodeWithText("9.4/10").assertIsDisplayed()
    }

    @Test
    fun imageContentSlotIsDisplayed() {
        composeRule.setContent {
            JuzgonTheme(darkTheme = true, dynamicColor = false) {
                JuzgonHeroCard(
                    title = "Featured",
                    tierLabel = "A-Tier",
                    scoreText = "8.5/10",
                    onClick = {},
                    image = { Text("AVATAR") },
                )
            }
        }

        composeRule.onNodeWithText("AVATAR").assertIsDisplayed()
    }

    @Test
    fun clickingCardTriggersOnClick() {
        var clicked = false

        composeRule.setContent {
            JuzgonTheme(darkTheme = true, dynamicColor = false) {
                JuzgonHeroCard(
                    title = "Top Ranked",
                    tierLabel = "S-Tier",
                    scoreText = "9.4/10",
                    onClick = { clicked = true },
                    image = { Text("IMG") },
                )
            }
        }

        composeRule
            .onNodeWithContentDescription("Top Ranked, S-Tier 9.4/10")
            .assertHasClickAction()
            .performClick()
        assertTrue(clicked)
    }

    @Test
    fun semanticDescriptionContainsTitleAndScore() {
        composeRule.setContent {
            JuzgonTheme(darkTheme = true, dynamicColor = false) {
                JuzgonHeroCard(
                    title = "Diamond of the Day",
                    tierLabel = "S-Tier",
                    scoreText = "9.8/10",
                    onClick = {},
                    image = { Text("IMG") },
                )
            }
        }

        composeRule
            .onNodeWithContentDescription("Diamond of the Day, S-Tier 9.8/10")
            .assertIsDisplayed()
    }

    @Test
    fun cardHasMinimumExpectedHeight() {
        composeRule.setContent {
            JuzgonTheme(darkTheme = true, dynamicColor = false) {
                JuzgonHeroCard(
                    title = "Featured",
                    tierLabel = "A-Tier",
                    scoreText = "8.0/10",
                    onClick = {},
                    image = { Text("IMG") },
                )
            }
        }

        composeRule
            .onNodeWithContentDescription("Featured, A-Tier 8.0/10")
            .assertHeightIsAtLeast(200.dp)
    }

    @Test
    fun allTextContentComesFromParams() {
        composeRule.setContent {
            JuzgonTheme(darkTheme = true, dynamicColor = false) {
                JuzgonHeroCard(
                    title = "Custom Title",
                    tierLabel = "B-Tier",
                    scoreText = "6.0/10",
                    onClick = {},
                    image = { Text("IMG") },
                )
            }
        }

        composeRule.onNodeWithText("Custom Title").assertIsDisplayed()
        composeRule.onNodeWithText("B-Tier").assertIsDisplayed()
        composeRule.onNodeWithText("6.0/10").assertIsDisplayed()
    }
}
