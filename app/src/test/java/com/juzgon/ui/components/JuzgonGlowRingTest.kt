@file:Suppress("FunctionName")

package com.juzgon.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
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
class JuzgonGlowRingTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun rendersWithoutCrashAtDefaultSize() {
        composeRule.setContent {
            JuzgonTheme(darkTheme = true, dynamicColor = false) {
                JuzgonGlowRing(contentDescription = "Avatar") {
                    Text("A")
                }
            }
        }

        composeRule.onNodeWithContentDescription("Avatar").assertIsDisplayed()
    }

    @Test
    fun innerContentSlotIsDisplayed() {
        composeRule.setContent {
            JuzgonTheme(darkTheme = true, dynamicColor = false) {
                JuzgonGlowRing(contentDescription = "Profile") {
                    Text("Inner Content")
                }
            }
        }

        composeRule.onNodeWithText("Inner Content").assertIsDisplayed()
    }

    @Test
    fun minimumTouchTargetIs48dp() {
        composeRule.setContent {
            JuzgonTheme(darkTheme = true, dynamicColor = false) {
                JuzgonGlowRing(
                    contentDescription = "Small ring",
                    modifier = Modifier.size(40.dp),
                ) {
                    Text("S")
                }
            }
        }

        composeRule
            .onNodeWithContentDescription("Small ring")
            .assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun worksAtSmallSize() {
        composeRule.setContent {
            JuzgonTheme(darkTheme = true, dynamicColor = false) {
                JuzgonGlowRing(
                    contentDescription = "Thumbnail",
                    modifier = Modifier.size(40.dp),
                ) {
                    Text("S")
                }
            }
        }

        composeRule.onNodeWithContentDescription("Thumbnail").assertIsDisplayed()
    }

    @Test
    fun worksAtMediumSize() {
        composeRule.setContent {
            JuzgonTheme(darkTheme = true, dynamicColor = false) {
                JuzgonGlowRing(
                    contentDescription = "Medium avatar",
                    modifier = Modifier.size(56.dp),
                ) {
                    Text("M")
                }
            }
        }

        composeRule.onNodeWithContentDescription("Medium avatar").assertIsDisplayed()
    }

    @Test
    fun worksAtLargeSize() {
        composeRule.setContent {
            JuzgonTheme(darkTheme = true, dynamicColor = false) {
                JuzgonGlowRing(
                    contentDescription = "Profile avatar",
                    modifier = Modifier.size(120.dp),
                ) {
                    Text("L")
                }
            }
        }

        composeRule.onNodeWithContentDescription("Profile avatar").assertIsDisplayed()
    }
}
