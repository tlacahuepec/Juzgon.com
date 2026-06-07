@file:Suppress("FunctionName")

package com.juzgon.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.juzgon.ui.theme.JuzgonTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class JuzgonBottomNavBarTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val items =
        listOf(
            BottomNavItem(Icons.Filled.Home, "Discover"),
            BottomNavItem(Icons.AutoMirrored.Filled.List, "Collection"),
            BottomNavItem(Icons.Filled.Favorite, "Favorites", enabled = false),
            BottomNavItem(Icons.Filled.Person, "Profile", enabled = false),
        )

    @Test
    fun rendersAllFourItemsWithCorrectLabels() {
        composeRule.setContent {
            JuzgonTheme(darkTheme = true, dynamicColor = false) {
                JuzgonBottomNavBar(
                    items = items,
                    selectedIndex = 0,
                    onItemSelected = {},
                )
            }
        }

        composeRule.onNodeWithText("Discover").assertIsDisplayed()
        composeRule.onNodeWithText("Collection").assertIsDisplayed()
        composeRule.onNodeWithText("Favorites").assertIsDisplayed()
        composeRule.onNodeWithText("Profile").assertIsDisplayed()
    }

    @Test
    fun selectedItemHasSelectedSemanticState() {
        composeRule.setContent {
            JuzgonTheme(darkTheme = true, dynamicColor = false) {
                JuzgonBottomNavBar(
                    items = items,
                    selectedIndex = 0,
                    onItemSelected = {},
                )
            }
        }

        composeRule.onNodeWithText("Discover").assertIsSelected()
        composeRule.onNodeWithText("Collection").assertIsNotSelected()
        composeRule.onNodeWithText("Favorites").assertIsNotSelected()
        composeRule.onNodeWithText("Profile").assertIsNotSelected()
    }

    @Test
    fun clickingEnabledItemTriggersOnSelected() {
        var selectedIndex = -1

        composeRule.setContent {
            JuzgonTheme(darkTheme = true, dynamicColor = false) {
                JuzgonBottomNavBar(
                    items = items,
                    selectedIndex = 0,
                    onItemSelected = { selectedIndex = it },
                )
            }
        }

        composeRule.onNodeWithText("Collection").performClick()
        assertEquals(1, selectedIndex)
    }

    @Test
    fun clickingDisabledItemDoesNotTriggerCallback() {
        var selectedIndex = -1

        composeRule.setContent {
            JuzgonTheme(darkTheme = true, dynamicColor = false) {
                JuzgonBottomNavBar(
                    items = items,
                    selectedIndex = 0,
                    onItemSelected = { selectedIndex = it },
                )
            }
        }

        composeRule.onNodeWithText("Favorites").performClick()
        assertEquals(-1, selectedIndex)
    }

    @Test
    fun eachItemHasMinimum48dpTouchTarget() {
        composeRule.setContent {
            JuzgonTheme(darkTheme = true, dynamicColor = false) {
                JuzgonBottomNavBar(
                    items = items,
                    selectedIndex = 0,
                    onItemSelected = {},
                )
            }
        }

        composeRule.onNodeWithText("Discover").assertHeightIsAtLeast(48.dp)
        composeRule.onNodeWithText("Collection").assertHeightIsAtLeast(48.dp)
        composeRule.onNodeWithText("Favorites").assertHeightIsAtLeast(48.dp)
        composeRule.onNodeWithText("Profile").assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun correctItemHighlightedBasedOnSelectedIndex() {
        composeRule.setContent {
            JuzgonTheme(darkTheme = true, dynamicColor = false) {
                JuzgonBottomNavBar(
                    items = items,
                    selectedIndex = 1,
                    onItemSelected = {},
                )
            }
        }

        composeRule.onNodeWithText("Discover").assertIsNotSelected()
        composeRule.onNodeWithText("Collection").assertIsSelected()
    }
}
