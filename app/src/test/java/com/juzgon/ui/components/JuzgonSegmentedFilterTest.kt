@file:Suppress("FunctionName")

package com.juzgon.ui.components

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
class JuzgonSegmentedFilterTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun rendersAllItems() {
        composeRule.setContent {
            JuzgonTheme(darkTheme = true, dynamicColor = false) {
                JuzgonSegmentedFilter(
                    items = listOf("Cards", "Bars", "Diamond"),
                    selectedIndex = 0,
                    onSelected = {},
                )
            }
        }

        composeRule.onNodeWithText("Cards").assertIsDisplayed()
        composeRule.onNodeWithText("Bars").assertIsDisplayed()
        composeRule.onNodeWithText("Diamond").assertIsDisplayed()
    }

    @Test
    fun selectedItemHasSelectedSemanticState() {
        composeRule.setContent {
            JuzgonTheme(darkTheme = true, dynamicColor = false) {
                JuzgonSegmentedFilter(
                    items = listOf("Cards", "Bars", "Diamond"),
                    selectedIndex = 1,
                    onSelected = {},
                )
            }
        }

        composeRule.onNodeWithText("Cards").assertIsNotSelected()
        composeRule.onNodeWithText("Bars").assertIsSelected()
        composeRule.onNodeWithText("Diamond").assertIsNotSelected()
    }

    @Test
    fun clickingUnselectedItemTriggersOnSelectedWithCorrectIndex() {
        var selectedIndex = -1

        composeRule.setContent {
            JuzgonTheme(darkTheme = true, dynamicColor = false) {
                JuzgonSegmentedFilter(
                    items = listOf("Cards", "Bars", "Diamond"),
                    selectedIndex = 0,
                    onSelected = { selectedIndex = it },
                )
            }
        }

        composeRule.onNodeWithText("Diamond").performClick()
        assertEquals(2, selectedIndex)
    }

    @Test
    fun minimumTouchTarget48dpPerSegment() {
        composeRule.setContent {
            JuzgonTheme(darkTheme = true, dynamicColor = false) {
                JuzgonSegmentedFilter(
                    items = listOf("A", "B"),
                    selectedIndex = 0,
                    onSelected = {},
                )
            }
        }

        composeRule.onNodeWithText("A").assertHeightIsAtLeast(48.dp)
        composeRule.onNodeWithText("B").assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun singleItemRendersWithoutCrash() {
        composeRule.setContent {
            JuzgonTheme(darkTheme = true, dynamicColor = false) {
                JuzgonSegmentedFilter(
                    items = listOf("Only"),
                    selectedIndex = 0,
                    onSelected = {},
                )
            }
        }

        composeRule.onNodeWithText("Only").assertIsDisplayed().assertIsSelected()
    }

    @Test
    fun manyItemsRenderWithoutCrash() {
        val items = (1..8).map { "Tab $it" }

        composeRule.setContent {
            JuzgonTheme(darkTheme = true, dynamicColor = false) {
                JuzgonSegmentedFilter(
                    items = items,
                    selectedIndex = 3,
                    onSelected = {},
                )
            }
        }

        composeRule.onNodeWithText("Tab 1").assertIsDisplayed()
        composeRule.onNodeWithText("Tab 4").assertIsSelected()
    }
}
