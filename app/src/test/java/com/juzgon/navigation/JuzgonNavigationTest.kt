@file:Suppress("FunctionName")

package com.juzgon.navigation

import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.testing.TestNavHostController
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class JuzgonNavigationTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun navHostStartsOnHomeRoute() {
        lateinit var navController: TestNavHostController

        composeRule.setContent {
            MaterialTheme {
                navController = rememberTestNavController()
                JuzgonNavHost(
                    navController = navController,
                    homeContent = { _, _ -> Text("Home route") },
                    createCategoryContent = { _, _ -> Text("Create category route") },
                    categoryDetailContent = { categoryName, _, _, _ -> Text("Detail route $categoryName") },
                    itemFormContent = { categoryName, _, _, _ -> Text("Add item route $categoryName") },
                )
            }
        }

        composeRule.onNodeWithText("Home route").assertIsDisplayed()
        composeRule.runOnIdle {
            assertEquals(JuzgonRoutes.HOME, navController.currentDestination?.route)
        }
    }

    @Test
    fun createRouteCanPopBackToHomeRoute() {
        lateinit var navController: TestNavHostController

        composeRule.setContent {
            MaterialTheme {
                navController = rememberTestNavController()
                JuzgonNavHost(
                    navController = navController,
                    homeContent = { onCreateCategory, _ ->
                        Button(onClick = onCreateCategory) {
                            Text("Open create")
                        }
                    },
                    createCategoryContent = { onBack, _ ->
                        Button(onClick = onBack) {
                            Text("Back home")
                        }
                    },
                    categoryDetailContent = { categoryName, _, _, _ -> Text("Detail route $categoryName") },
                    itemFormContent = { categoryName, _, _, _ -> Text("Add item route $categoryName") },
                )
            }
        }

        composeRule.onNodeWithText("Open create").performClick()
        composeRule.runOnIdle {
            assertEquals(JuzgonRoutes.CREATE_CATEGORY, navController.currentDestination?.route)
        }

        composeRule.onNodeWithText("Back home").performClick()
        composeRule.runOnIdle {
            assertEquals(JuzgonRoutes.HOME, navController.currentDestination?.route)
        }
    }

    @Test
    fun homeRouteCanOpenEncodedCategoryDetailRouteAndReturn() {
        lateinit var navController: TestNavHostController

        composeRule.setContent {
            MaterialTheme {
                navController = rememberTestNavController()
                JuzgonNavHost(
                    navController = navController,
                    homeContent = { _, onOpenCategory ->
                        Button(onClick = { onOpenCategory("Fast Cars / SUVs") }) {
                            Text("Open detail")
                        }
                    },
                    createCategoryContent = { _, _ -> Text("Create category route") },
                    categoryDetailContent = { categoryName, onBack, _, _ ->
                        Button(onClick = onBack) {
                            Text("Detail route $categoryName")
                        }
                    },
                    itemFormContent = { categoryName, _, _, _ -> Text("Add item route $categoryName") },
                )
            }
        }

        composeRule.onNodeWithText("Open detail").performClick()
        composeRule.onNodeWithText("Detail route Fast Cars / SUVs").assertIsDisplayed()
        composeRule.runOnIdle {
            assertEquals(JuzgonRoutes.CATEGORY_DETAIL, navController.currentDestination?.route)
        }

        composeRule.onNodeWithText("Detail route Fast Cars / SUVs").performClick()
        composeRule.runOnIdle {
            assertEquals(JuzgonRoutes.HOME, navController.currentDestination?.route)
        }
    }

    @Test
    fun categoryDetailCanOpenEncodedAddItemRouteAndReturnAfterSave() {
        lateinit var navController: TestNavHostController

        composeRule.setContent {
            MaterialTheme {
                navController = rememberTestNavController()
                JuzgonNavHost(
                    navController = navController,
                    homeContent = { _, onOpenCategory ->
                        Button(onClick = { onOpenCategory("Fast Cars / SUVs") }) {
                            Text("Open detail")
                        }
                    },
                    createCategoryContent = { _, _ -> Text("Create category route") },
                    categoryDetailContent = { categoryName, _, onAddItem, _ ->
                        Button(onClick = onAddItem) {
                            Text("Detail route $categoryName")
                        }
                    },
                    itemFormContent = { categoryName, _, _, onSaveCompleted ->
                        Button(onClick = onSaveCompleted) {
                            Text("Add item route $categoryName")
                        }
                    },
                )
            }
        }

        composeRule.onNodeWithText("Open detail").performClick()
        composeRule.onNodeWithText("Detail route Fast Cars / SUVs").performClick()
        composeRule.onNodeWithText("Add item route Fast Cars / SUVs").assertIsDisplayed()
        composeRule.runOnIdle {
            assertEquals(JuzgonRoutes.CREATE_ITEM, navController.currentDestination?.route)
        }

        composeRule.onNodeWithText("Add item route Fast Cars / SUVs").performClick()
        composeRule.runOnIdle {
            assertEquals(JuzgonRoutes.CATEGORY_DETAIL, navController.currentDestination?.route)
        }
    }

    @Test
    fun categoryDetailCanOpenEncodedEditItemRouteAndReturnAfterSave() {
        lateinit var navController: TestNavHostController

        composeRule.setContent {
            MaterialTheme {
                navController = rememberTestNavController()
                JuzgonNavHost(
                    navController = navController,
                    homeContent = { _, onOpenCategory ->
                        Button(onClick = { onOpenCategory("Fast Cars / SUVs") }) {
                            Text("Open detail")
                        }
                    },
                    createCategoryContent = { _, _ -> Text("Create category route") },
                    categoryDetailContent = { categoryName, _, _, onEditItem ->
                        Button(onClick = { onEditItem("Roadster / 2026") }) {
                            Text("Detail route $categoryName")
                        }
                    },
                    itemFormContent = { categoryName, itemId, _, onSaveCompleted ->
                        Button(onClick = onSaveCompleted) {
                            Text("Edit item route $categoryName $itemId")
                        }
                    },
                )
            }
        }

        composeRule.onNodeWithText("Open detail").performClick()
        composeRule.onNodeWithText("Detail route Fast Cars / SUVs").performClick()
        composeRule.onNodeWithText("Edit item route Fast Cars / SUVs Roadster / 2026").assertIsDisplayed()
        composeRule.runOnIdle {
            assertEquals(JuzgonRoutes.EDIT_ITEM, navController.currentDestination?.route)
        }

        composeRule.onNodeWithText("Edit item route Fast Cars / SUVs Roadster / 2026").performClick()
        composeRule.runOnIdle {
            assertEquals(JuzgonRoutes.CATEGORY_DETAIL, navController.currentDestination?.route)
        }
    }

    @Composable
    private fun rememberTestNavController(): TestNavHostController {
        val context = LocalContext.current
        return remember {
            TestNavHostController(context).apply {
                navigatorProvider.addNavigator(ComposeNavigator())
            }
        }
    }
}
