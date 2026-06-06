package com.juzgon

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.juzgon.data.local.JuzgonDatabase
import com.juzgon.data.local.entity.AttributeEntity
import com.juzgon.data.local.entity.CategoryEntity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject

@HiltAndroidTest
class CreateItemHappyPathTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Inject
    lateinit var database: JuzgonDatabase

    @Before
    fun setUp() {
        hiltRule.inject()
        runBlocking {
            database.categoryDao().upsertCategory(CategoryEntity(name = "Soccer Players"))
            database.categoryDao().upsertAttributes(
                listOf(
                    AttributeEntity(
                        id = "Soccer Players/Speed",
                        categoryName = "Soccer Players",
                        weight = 1.0,
                        position = 1,
                        type = "NUMBER",
                        isRequired = true,
                        displayInDiamond = true,
                        diamondOrder = 1,
                    ),
                ),
            )
        }
    }

    @Test
    fun createItemInCategory_appearsInItemList() {
        composeRule.onNodeWithText("Soccer Players").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription("Add item").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription("Item title").performTextInput("Messi")
        composeRule
            .onNodeWithContentDescription("Soccer Players/Speed score")
            .performTextInput("8")

        composeRule.onNodeWithContentDescription("Save item").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Messi").assertIsDisplayed()
    }
}
