package com.juzgon.ui.components

import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.unit.dp
import com.juzgon.ui.theme.JuzgonTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class JuzgonGradientScoreBarTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun rendersWithoutCrashAtDefaultSize() {
        composeRule.setContent {
            JuzgonTheme(darkTheme = true, dynamicColor = false) {
                JuzgonGradientScoreBar(progress = 0.5f)
            }
        }

        composeRule.onNodeWithContentDescription("Score: 50%").assertIsDisplayed()
    }

    @Test
    fun progressOfZeroRendersWithZeroPercentDescription() {
        composeRule.setContent {
            JuzgonTheme(darkTheme = true, dynamicColor = false) {
                JuzgonGradientScoreBar(progress = 0f)
            }
        }

        composeRule.onNodeWithContentDescription("Score: 0%").assertIsDisplayed()
    }

    @Test
    fun progressOfOneRendersWithFullPercentDescription() {
        composeRule.setContent {
            JuzgonTheme(darkTheme = true, dynamicColor = false) {
                JuzgonGradientScoreBar(progress = 1f)
            }
        }

        composeRule.onNodeWithContentDescription("Score: 100%").assertIsDisplayed()
    }

    @Test
    fun progressBelowZeroIsClamped() {
        composeRule.setContent {
            JuzgonTheme(darkTheme = true, dynamicColor = false) {
                JuzgonGradientScoreBar(progress = -0.5f)
            }
        }

        composeRule.onNodeWithContentDescription("Score: 0%").assertIsDisplayed()
    }

    @Test
    fun progressAboveOneIsClamped() {
        composeRule.setContent {
            JuzgonTheme(darkTheme = true, dynamicColor = false) {
                JuzgonGradientScoreBar(progress = 1.5f)
            }
        }

        composeRule.onNodeWithContentDescription("Score: 100%").assertIsDisplayed()
    }

    @Test
    fun configurableHeightIsApplied() {
        composeRule.setContent {
            JuzgonTheme(darkTheme = true, dynamicColor = false) {
                JuzgonGradientScoreBar(progress = 0.7f, height = 14.dp)
            }
        }

        composeRule
            .onNodeWithContentDescription("Score: 70%")
            .assertHeightIsAtLeast(14.dp)
    }

    @Test
    fun defaultHeightIs12dp() {
        composeRule.setContent {
            JuzgonTheme(darkTheme = true, dynamicColor = false) {
                JuzgonGradientScoreBar(progress = 0.3f)
            }
        }

        composeRule
            .onNodeWithContentDescription("Score: 30%")
            .assertHeightIsAtLeast(12.dp)
    }
}
