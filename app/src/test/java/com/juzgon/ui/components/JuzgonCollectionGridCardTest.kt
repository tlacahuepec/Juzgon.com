@file:Suppress("FunctionName")

package com.juzgon.ui.components

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.juzgon.ui.theme.JuzgonTheme
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class JuzgonCollectionGridCardTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun rendersNameAndScore() {
        composeRule.setContent {
            JuzgonTheme(darkTheme = true, dynamicColor = false) {
                JuzgonCollectionGridCard(
                    name = "Luna",
                    tierLabel = "S-Tier",
                    scoreText = "9.3/10",
                    onClick = {},
                    onFavoriteClick = {},
                    image = { Text("IMG") },
                )
            }
        }

        composeRule.onNodeWithText("Luna").assertIsDisplayed()
        composeRule.onNodeWithText("S-Tier \u2605 9.3/10").assertIsDisplayed()
    }

    @Test
    fun imageContentSlotIsDisplayed() {
        composeRule.setContent {
            JuzgonTheme(darkTheme = true, dynamicColor = false) {
                JuzgonCollectionGridCard(
                    name = "Aiko",
                    tierLabel = "A-Tier",
                    scoreText = "8.7/10",
                    onClick = {},
                    onFavoriteClick = {},
                    image = { Text("AVATAR") },
                )
            }
        }

        composeRule.onNodeWithText("AVATAR").assertIsDisplayed()
    }

    @Test
    fun attributePreviewsRenderWhenProvided() {
        val attributes =
            listOf(
                GridCardAttribute(emoji = "\uD83D\uDCA7", label = "Age", scoreText = "10/10"),
                GridCardAttribute(emoji = "\uD83D\uDFE2", label = "Eyes", scoreText = "10/10"),
                GridCardAttribute(emoji = "\uD83E\uDE77", label = "Lips", scoreText = "10/10"),
            )

        composeRule.setContent {
            JuzgonTheme(darkTheme = true, dynamicColor = false) {
                JuzgonCollectionGridCard(
                    name = "Yumi",
                    tierLabel = "S-Tier",
                    scoreText = "8.9/10",
                    onClick = {},
                    onFavoriteClick = {},
                    attributes = attributes,
                    image = { Text("IMG") },
                )
            }
        }

        composeRule.onNodeWithText("\uD83D\uDCA7 Age \u2605 10/10").assertIsDisplayed()
        composeRule.onNodeWithText("\uD83D\uDFE2 Eyes \u2605 10/10").assertIsDisplayed()
        composeRule.onNodeWithText("\uD83E\uDE77 Lips \u2605 10/10").assertIsDisplayed()
    }

    @Test
    fun attributePreviewsAbsentWhenNotProvided() {
        composeRule.setContent {
            JuzgonTheme(darkTheme = true, dynamicColor = false) {
                JuzgonCollectionGridCard(
                    name = "Luna",
                    tierLabel = "S-Tier",
                    scoreText = "9.3/10",
                    onClick = {},
                    onFavoriteClick = {},
                    image = { Text("IMG") },
                )
            }
        }

        composeRule.onNodeWithText("Luna").assertIsDisplayed()
        composeRule.onNodeWithText("\uD83D\uDCA7 Age \u2605 10/10").assertDoesNotExist()
    }

    @Test
    fun favoriteButtonClickTriggersSeparateCallback() {
        var cardClicked = false
        var favoriteClicked = false

        composeRule.setContent {
            JuzgonTheme(darkTheme = true, dynamicColor = false) {
                JuzgonCollectionGridCard(
                    name = "Luna",
                    tierLabel = "S-Tier",
                    scoreText = "9.3/10",
                    onClick = { cardClicked = true },
                    onFavoriteClick = { favoriteClicked = true },
                    image = { Text("IMG") },
                )
            }
        }

        composeRule
            .onNodeWithContentDescription("Toggle favorite")
            .assertHasClickAction()
            .performClick()
        assertTrue(favoriteClicked)
        assertFalse(cardClicked)
    }

    @Test
    fun cardClickTriggersNavigationCallback() {
        var clicked = false

        composeRule.setContent {
            JuzgonTheme(darkTheme = true, dynamicColor = false) {
                JuzgonCollectionGridCard(
                    name = "Luna",
                    tierLabel = "S-Tier",
                    scoreText = "9.3/10",
                    onClick = { clicked = true },
                    onFavoriteClick = {},
                    image = { Text("IMG") },
                )
            }
        }

        composeRule
            .onNodeWithContentDescription("Luna, S-Tier 9.3/10")
            .assertHasClickAction()
            .performClick()
        assertTrue(clicked)
    }

    @Test
    fun longNameTruncatesWithEllipsis() {
        val longName = "A Very Long Character Name That Should Overflow"

        composeRule.setContent {
            JuzgonTheme(darkTheme = true, dynamicColor = false) {
                JuzgonCollectionGridCard(
                    name = longName,
                    tierLabel = "B-Tier",
                    scoreText = "6.0/10",
                    onClick = {},
                    onFavoriteClick = {},
                    image = { Text("IMG") },
                )
            }
        }

        composeRule
            .onNodeWithContentDescription("$longName, B-Tier 6.0/10")
            .assertIsDisplayed()
    }

    @Test
    fun semanticDescriptionContainsNameAndScore() {
        composeRule.setContent {
            JuzgonTheme(darkTheme = true, dynamicColor = false) {
                JuzgonCollectionGridCard(
                    name = "Sora",
                    tierLabel = "A-Tier",
                    scoreText = "8.7/10",
                    onClick = {},
                    onFavoriteClick = {},
                    image = { Text("IMG") },
                )
            }
        }

        composeRule
            .onNodeWithContentDescription("Sora, A-Tier 8.7/10")
            .assertIsDisplayed()
    }
}
