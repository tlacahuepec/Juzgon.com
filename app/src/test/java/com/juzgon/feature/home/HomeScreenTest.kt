package com.juzgon.feature.home

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class HomeScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun homeScreenShowsEmptyState() {
        var createClicked = false

        composeRule.setContent {
            MaterialTheme {
                HomeScreen(
                    state = HomeUiState(),
                    onSearchQueryChange = {},
                    onSortOptionSelected = {},
                    onCreateCategoryClick = { createClicked = true },
                )
            }
        }

        composeRule.onNodeWithText("No categories yet").assertIsDisplayed()
        composeRule.onNodeWithText("Create category").assertIsDisplayed()
        composeRule.onNodeWithText("Create category").performClick()

        assertTrue(createClicked)
    }

    @Test
    fun homeScreenShowsContentState() {
        var createClicked = false

        composeRule.setContent {
            MaterialTheme {
                HomeScreen(
                    state =
                        HomeUiState(
                            categories =
                                listOf(
                                    HomeCategoryUiModel(name = "Food", attributeCount = 2),
                                    HomeCategoryUiModel(name = "Travel", attributeCount = 1),
                                ),
                        ),
                    onSearchQueryChange = {},
                    onSortOptionSelected = {},
                    onCreateCategoryClick = { createClicked = true },
                )
            }
        }

        composeRule.onNodeWithText("Search categories").assertIsDisplayed()
        composeRule.onNodeWithText("Recent").assertIsDisplayed()
        composeRule.onNodeWithText("Name").assertIsDisplayed()
        composeRule.onNodeWithText("Food").assertIsDisplayed()
        composeRule.onNodeWithText("2 attributes").assertIsDisplayed()
        composeRule.onNodeWithText("Travel").assertIsDisplayed()
        composeRule.onNodeWithText("1 attribute").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Create category").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Create category").performClick()

        assertTrue(createClicked)
    }
}
