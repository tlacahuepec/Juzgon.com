@file:Suppress("FunctionName")

package com.juzgon.ui.components

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsAtLeast
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
class JuzgonScorePillTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun rendersTierAndScoreText() {
        composeRule.setContent {
            JuzgonTheme(darkTheme = true, dynamicColor = false) {
                JuzgonScorePill(tierText = "S-Tier", scoreText = "9.2/10")
            }
        }

        composeRule.onNodeWithText("S-Tier").assertIsDisplayed()
        composeRule.onNodeWithText("9.2/10").assertIsDisplayed()
    }

    @Test
    fun leadingIconSlotDisplayedWhenProvided() {
        composeRule.setContent {
            JuzgonTheme(darkTheme = true, dynamicColor = false) {
                JuzgonScorePill(
                    tierText = "A-Tier",
                    scoreText = "8.0/10",
                    icon = { Text("\uD83D\uDC8E") },
                )
            }
        }

        composeRule.onNodeWithText("\uD83D\uDC8E").assertIsDisplayed()
    }

    @Test
    fun mergedSemanticNodeContainsContentDescription() {
        composeRule.setContent {
            JuzgonTheme(darkTheme = true, dynamicColor = false) {
                JuzgonScorePill(tierText = "S-Tier", scoreText = "9.2/10")
            }
        }

        composeRule
            .onNodeWithContentDescription("S-Tier, 9.2/10")
            .assertIsDisplayed()
    }

    @Test
    fun clickableVariantHas48dpMinTouchTarget() {
        composeRule.setContent {
            JuzgonTheme(darkTheme = true, dynamicColor = false) {
                JuzgonScorePill(
                    tierText = "B-Tier",
                    scoreText = "6.5/10",
                    onClick = {},
                )
            }
        }

        composeRule
            .onNodeWithContentDescription("B-Tier, 6.5/10")
            .assertHasClickAction()
            .assertHeightIsAtLeast(48.dp)
            .assertWidthIsAtLeast(48.dp)
    }

    @Test
    fun nonClickableVariantRendersWithoutClickAction() {
        composeRule.setContent {
            JuzgonTheme(darkTheme = true, dynamicColor = false) {
                JuzgonScorePill(tierText = "C-Tier", scoreText = "4.0/10")
            }
        }

        composeRule
            .onNodeWithContentDescription("C-Tier, 4.0/10")
            .assertHasNoClickAction()
    }
}
