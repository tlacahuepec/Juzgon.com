package com.juzgon

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class CreateCategoryHappyPathTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun createCategoryWithNumberAttribute_appearsInHomeList() {
        composeRule.onNodeWithContentDescription("Create category").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription("Category name").performTextInput("Test Athletes")
        composeRule.onNodeWithContentDescription("Attribute 1 name").performTextInput("Speed")

        composeRule.onNodeWithContentDescription("Save category").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Test Athletes").assertIsDisplayed()
    }
}
