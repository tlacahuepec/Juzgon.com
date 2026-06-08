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
class JuzgonItemThumbnailTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun rendersScoreText() {
        composeRule.setContent {
            JuzgonTheme(darkTheme = true, dynamicColor = false) {
                JuzgonItemThumbnail(
                    scoreText = "9.4/10",
                    contentDescription = "Sakura, score 9.4/10",
                    onClick = {},
                    image = { Text("IMG") },
                )
            }
        }

        composeRule.onNodeWithText("9.4/10").assertIsDisplayed()
    }

    @Test
    fun imageSlotContentIsDisplayed() {
        composeRule.setContent {
            JuzgonTheme(darkTheme = true, dynamicColor = false) {
                JuzgonItemThumbnail(
                    scoreText = "8.0/10",
                    contentDescription = "Test, score 8.0/10",
                    onClick = {},
                    image = { Text("PHOTO") },
                )
            }
        }

        composeRule.onNodeWithText("PHOTO").assertIsDisplayed()
    }

    @Test
    fun cardIsClickableAndTriggersCallback() {
        var clicked = false

        composeRule.setContent {
            JuzgonTheme(darkTheme = true, dynamicColor = false) {
                JuzgonItemThumbnail(
                    scoreText = "9.4/10",
                    contentDescription = "Sakura, score 9.4/10",
                    onClick = { clicked = true },
                    image = { Text("IMG") },
                )
            }
        }

        composeRule
            .onNodeWithContentDescription("Sakura, score 9.4/10")
            .assertHasClickAction()
            .performClick()
        assertTrue(clicked)
    }

    @Test
    fun semanticContentDescriptionContainsNameAndScore() {
        composeRule.setContent {
            JuzgonTheme(darkTheme = true, dynamicColor = false) {
                JuzgonItemThumbnail(
                    scoreText = "9.4/10",
                    contentDescription = "Sakura, score 9.4/10",
                    onClick = {},
                    image = { Text("IMG") },
                )
            }
        }

        composeRule
            .onNodeWithContentDescription("Sakura, score 9.4/10")
            .assertIsDisplayed()
    }

    @Test
    fun cardHasMinimumTouchTarget() {
        composeRule.setContent {
            JuzgonTheme(darkTheme = true, dynamicColor = false) {
                JuzgonItemThumbnail(
                    scoreText = "7.5/10",
                    contentDescription = "Test, score 7.5/10",
                    onClick = {},
                    image = { Text("IMG") },
                )
            }
        }

        composeRule
            .onNodeWithContentDescription("Test, score 7.5/10")
            .assertHeightIsAtLeast(48.dp)
    }
}
