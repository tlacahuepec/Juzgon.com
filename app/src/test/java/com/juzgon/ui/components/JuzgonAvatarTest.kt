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
class JuzgonAvatarTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun fallbackAvatarDisplaysInitialAndDefaultDescription() {
        composeRule.setContent {
            JuzgonTheme(darkTheme = true, dynamicColor = false) {
                JuzgonAvatar(name = "Ada Lovelace")
            }
        }

        composeRule.onNodeWithText("A").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Ada Lovelace avatar").assertIsDisplayed()
    }

    @Test
    fun blankNameDisplaysSafeFallbackAndCustomDescription() {
        composeRule.setContent {
            JuzgonTheme(darkTheme = true, dynamicColor = false) {
                JuzgonAvatar(name = "   ", contentDescription = "Profile image")
            }
        }

        composeRule.onNodeWithText("?").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Profile image").assertIsDisplayed()
    }
}
