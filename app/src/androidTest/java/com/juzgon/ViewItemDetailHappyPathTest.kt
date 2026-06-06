package com.juzgon

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.juzgon.data.local.JuzgonDatabase
import com.juzgon.data.local.entity.AttributeEntity
import com.juzgon.data.local.entity.CategoryEntity
import com.juzgon.data.local.entity.ItemEntity
import com.juzgon.data.local.entity.RatingEntity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject

@HiltAndroidTest
class ViewItemDetailHappyPathTest {
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
                    AttributeEntity(
                        id = "Soccer Players/Dribbling",
                        categoryName = "Soccer Players",
                        weight = 1.0,
                        position = 2,
                        type = "NUMBER",
                        isRequired = true,
                        displayInDiamond = true,
                        diamondOrder = 2,
                    ),
                    AttributeEntity(
                        id = "Soccer Players/Shooting",
                        categoryName = "Soccer Players",
                        weight = 1.0,
                        position = 3,
                        type = "NUMBER",
                        isRequired = true,
                        displayInDiamond = true,
                        diamondOrder = 3,
                    ),
                ),
            )
            database.itemDao().upsertItem(
                ItemEntity(
                    id = "Messi",
                    notes = "GOAT",
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                ),
            )
            database.itemDao().upsertRatings(
                listOf(
                    RatingEntity(itemId = "Messi", attributeId = "Soccer Players/Speed", score = 9),
                    RatingEntity(itemId = "Messi", attributeId = "Soccer Players/Dribbling", score = 10),
                    RatingEntity(itemId = "Messi", attributeId = "Soccer Players/Shooting", score = 9),
                ),
            )
        }
    }

    @Test
    fun viewItemDetail_showsDiamondChartAndRankedAttributes() {
        composeRule.onNodeWithText("Soccer Players").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Messi").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription("Attribute diamond chart").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Ranked attribute score list").assertIsDisplayed()
        composeRule.onNodeWithTag("RankedAttributeCard:Rank1:1").assertIsDisplayed()
    }
}
