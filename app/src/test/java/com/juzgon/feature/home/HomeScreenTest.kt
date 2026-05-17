package com.juzgon.feature.home

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
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
        composeRule.setContent {
            MaterialTheme {
                HomeScreen(
                    state = HomeUiState(),
                    onSearchQueryChange = {},
                    onSortOptionSelected = {},
                )
            }
        }

        composeRule.onNodeWithText("No categories yet").assertIsDisplayed()
    }

    @Test
    fun homeScreenShowsContentState() {
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
    }
}
