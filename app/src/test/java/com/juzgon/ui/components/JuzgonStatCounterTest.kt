@file:Suppress("FunctionName")

package com.juzgon.ui.components

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
class JuzgonStatCounterTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun rendersValueAndLabelText() {
        composeRule.setContent {
            JuzgonTheme(darkTheme = true, dynamicColor = false) {
                JuzgonStatCounter(value = "12", label = "S-Tier")
            }
        }

        composeRule.onNodeWithText("12").assertIsDisplayed()
        composeRule.onNodeWithText("S-Tier").assertIsDisplayed()
    }

    @Test
    fun mergedSemanticContentDescriptionFormat() {
        composeRule.setContent {
            JuzgonTheme(darkTheme = true, dynamicColor = false) {
                JuzgonStatCounter(value = "12", label = "S-Tier")
            }
        }

        composeRule.onNodeWithContentDescription("S-Tier: 12").assertIsDisplayed()
    }

    @Test
    fun worksWithIntegerValue() {
        composeRule.setContent {
            JuzgonTheme(darkTheme = true, dynamicColor = false) {
                JuzgonStatCounter(value = "47", label = "Total Rated")
            }
        }

        composeRule.onNodeWithContentDescription("Total Rated: 47").assertIsDisplayed()
    }

    @Test
    fun worksWithDecimalValue() {
        composeRule.setContent {
            JuzgonTheme(darkTheme = true, dynamicColor = false) {
                JuzgonStatCounter(value = "9.1", label = "Avg Score")
            }
        }

        composeRule.onNodeWithContentDescription("Avg Score: 9.1").assertIsDisplayed()
    }

    @Test
    fun worksWithTextValue() {
        composeRule.setContent {
            JuzgonTheme(darkTheme = true, dynamicColor = false) {
                JuzgonStatCounter(value = "N/A", label = "Pending")
            }
        }

        composeRule.onNodeWithContentDescription("Pending: N/A").assertIsDisplayed()
    }
}
