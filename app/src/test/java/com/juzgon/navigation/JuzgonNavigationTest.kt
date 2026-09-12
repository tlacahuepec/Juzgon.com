@file:Suppress("FunctionName")

package com.juzgon.navigation

import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
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
                    homeContent = { _, _, _ -> Text("Home route") },
                    createCategoryContent = { _, _ -> Text("Create category route") },
                    categoryDetailContent = { categoryName, _, _, _, _, _, _ -> Text("Detail route $categoryName") },
                    itemFormContent = { categoryName, _, _, _, _, _ -> Text("Add item route $categoryName") },
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
                    homeContent = { onCreateCategory, _, _ ->
                        Button(onClick = onCreateCategory) {
                            Text("Open create")
                        }
                    },
                    createCategoryContent = { onBack, _ ->
                        Button(onClick = onBack) {
                            Text("Back home")
                        }
                    },
                    categoryDetailContent = { categoryName, _, _, _, _, _, _ -> Text("Detail route $categoryName") },
                    itemFormContent = { categoryName, _, _, _, _, _ -> Text("Add item route $categoryName") },
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
                    homeContent = { _, onOpenCategory, _ ->
                        Button(onClick = { onOpenCategory("Fast Cars / SUVs") }) {
                            Text("Open detail")
                        }
                    },
                    createCategoryContent = { _, _ -> Text("Create category route") },
                    categoryDetailContent = { categoryName, onBack, _, _, _, _, _ ->
                        Button(onClick = onBack) {
                            Text("Detail route $categoryName")
                        }
                    },
                    itemFormContent = { categoryName, _, _, _, _, _ -> Text("Add item route $categoryName") },
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
                    homeContent = { _, onOpenCategory, _ ->
                        Button(onClick = { onOpenCategory("Fast Cars / SUVs") }) {
                            Text("Open detail")
                        }
                    },
                    createCategoryContent = { _, _ -> Text("Create category route") },
                    categoryDetailContent = { categoryName, _, onAddItem, _, _, _, _ ->
                        Button(onClick = onAddItem) {
                            Text("Detail route $categoryName")
                        }
                    },
                    itemFormContent = { categoryName, _, _, onSaveCompleted, _, _ ->
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
    fun categoryDetailOpensItemDetailOnItemTap() {
        lateinit var navController: TestNavHostController

        composeRule.setContent {
            MaterialTheme {
                navController = rememberTestNavController()
                JuzgonNavHost(
                    navController = navController,
                    homeContent = { _, onOpenCategory, _ ->
                        Button(onClick = { onOpenCategory("Fast Cars / SUVs") }) {
                            Text("Open detail")
                        }
                    },
                    createCategoryContent = { _, _ -> Text("Create category route") },
                    categoryDetailContent = { categoryName, _, _, onItemClick, _, _, _ ->
                        Button(onClick = { onItemClick("Roadster / 2026", null) }) {
                            Text("Detail route $categoryName")
                        }
                    },
                    itemDetailContent = { itemId, _, _, _, _, _ -> Text("Item detail $itemId") },
                    itemFormContent = { categoryName, _, _, _, _, _ -> Text("Add item route $categoryName") },
                )
            }
        }

        composeRule.onNodeWithText("Open detail").performClick()
        composeRule.onNodeWithText("Detail route Fast Cars / SUVs").performClick()
        composeRule.onNodeWithText("Item detail Roadster / 2026").assertIsDisplayed()
        composeRule.runOnIdle {
            assertEquals(JuzgonRoutes.ITEM_DETAIL, navController.currentDestination?.route)
        }
    }

    @Test
    fun itemDetailOpensEditItemOnEditTap() {
        lateinit var navController: TestNavHostController

        composeRule.setContent {
            MaterialTheme {
                navController = rememberTestNavController()
                JuzgonNavHost(
                    navController = navController,
                    homeContent = { _, onOpenCategory, _ ->
                        Button(onClick = { onOpenCategory("Fast Cars / SUVs") }) {
                            Text("Open detail")
                        }
                    },
                    createCategoryContent = { _, _ -> Text("Create category route") },
                    categoryDetailContent = { categoryName, _, _, onItemClick, _, _, _ ->
                        Button(onClick = { onItemClick("Roadster / 2026", null) }) {
                            Text("Detail route $categoryName")
                        }
                    },
                    itemDetailContent = { _, _, _, _, onEditClick, _ ->
                        Button(onClick = onEditClick) {
                            Text("Item detail route")
                        }
                    },
                    itemFormContent = { categoryName, itemId, _, _, _, _ ->
                        Text("Edit item route $categoryName $itemId")
                    },
                )
            }
        }

        composeRule.onNodeWithText("Open detail").performClick()
        composeRule.onNodeWithText("Detail route Fast Cars / SUVs").performClick()
        composeRule.onNodeWithText("Item detail route").performClick()
        composeRule.onNodeWithText("Edit item route Fast Cars / SUVs Roadster / 2026").assertIsDisplayed()
        composeRule.runOnIdle {
            assertEquals(JuzgonRoutes.EDIT_ITEM, navController.currentDestination?.route)
        }
    }

    @Test
    fun itemDetailReturnsToCategoryAfterDelete() {
        lateinit var navController: TestNavHostController

        composeRule.setContent {
            MaterialTheme {
                navController = rememberTestNavController()
                JuzgonNavHost(
                    navController = navController,
                    homeContent = { _, onOpenCategory, _ ->
                        Button(onClick = { onOpenCategory("Fast Cars / SUVs") }) {
                            Text("Open detail")
                        }
                    },
                    createCategoryContent = { _, _ -> Text("Create category route") },
                    categoryDetailContent = { categoryName, _, _, onItemClick, _, _, _ ->
                        Button(onClick = { onItemClick("Roadster / 2026", null) }) {
                            Text("Detail route $categoryName")
                        }
                    },
                    itemDetailContent = { _, _, _, _, _, onDeleteCompleted ->
                        Button(onClick = onDeleteCompleted) {
                            Text("Item detail route")
                        }
                    },
                    itemFormContent = { categoryName, _, _, _, _, _ -> Text("Add item route $categoryName") },
                )
            }
        }

        composeRule.onNodeWithText("Open detail").performClick()
        composeRule.onNodeWithText("Detail route Fast Cars / SUVs").performClick()
        composeRule.onNodeWithText("Item detail route").performClick()

        composeRule.onNodeWithText("Detail route Fast Cars / SUVs").assertIsDisplayed()
        composeRule.runOnIdle {
            assertEquals(JuzgonRoutes.CATEGORY_DETAIL, navController.currentDestination?.route)
        }
    }

    @Test
    fun navRoutesContainCatalogsAndSettings() {
        assertEquals("catalogs", JuzgonRoutes.CATALOGS)
        assertEquals("settings", JuzgonRoutes.SETTINGS)
    }

    @Test
    fun navHostSupportsCatalogsAndSettingsDestinations() {
        lateinit var navController: TestNavHostController

        composeRule.setContent {
            MaterialTheme {
                navController = rememberTestNavController()
                JuzgonNavHost(
                    navController = navController,
                    homeContent = { _, _, _ -> Text("Home route") },
                    catalogsContent = { onOpenCategory, _ ->
                        Button(onClick = { onOpenCategory("Cars") }) {
                            Text("Catalogs route")
                        }
                    },
                    settingsContent = { _, onGeminiSettings ->
                        Button(onClick = onGeminiSettings) {
                            Text("Settings route")
                        }
                    },
                    categoryDetailContent = { categoryName, _, _, _, _, _, _ -> Text("Detail route $categoryName") },
                    geminiKeySettingsContent = { _ -> Text("Gemini key settings route") },
                )
            }
        }

        // Navigate to Catalogs
        composeRule.runOnIdle {
            navController.navigate(JuzgonRoutes.CATALOGS)
        }
        composeRule.onNodeWithText("Catalogs route").assertIsDisplayed()
        composeRule.runOnIdle {
            assertEquals(JuzgonRoutes.CATALOGS, navController.currentDestination?.route)
        }

        // Catalogs opens category detail
        composeRule.onNodeWithText("Catalogs route").performClick()
        composeRule.onNodeWithText("Detail route Cars").assertIsDisplayed()
        composeRule.runOnIdle {
            assertEquals(JuzgonRoutes.CATEGORY_DETAIL, navController.currentDestination?.route)
        }

        // Navigate to Settings
        composeRule.runOnIdle {
            navController.navigate(JuzgonRoutes.SETTINGS)
        }
        composeRule.onNodeWithText("Settings route").assertIsDisplayed()
        composeRule.runOnIdle {
            assertEquals(JuzgonRoutes.SETTINGS, navController.currentDestination?.route)
        }

        // Settings opens Gemini key settings
        composeRule.onNodeWithText("Settings route").performClick()
        composeRule.onNodeWithText("Gemini key settings route").assertIsDisplayed()
        composeRule.runOnIdle {
            assertEquals(JuzgonRoutes.GEMINI_KEY_SETTINGS, navController.currentDestination?.route)
        }
    }

    @Test
    fun juzgonAppNavigatesBetweenTopLevelTabs() {
        lateinit var navController: TestNavHostController

        composeRule.setContent {
            MaterialTheme {
                navController = rememberTestNavController()
                JuzgonApp(
                    navController = navController,
                    navHost = { controller, modifier ->
                        JuzgonNavHost(
                            navController = controller,
                            modifier = modifier,
                            homeContent = { _, _, _ -> Text("Home content") },
                            catalogsContent = { _, _ -> Text("Catalogs content") },
                            settingsContent = { _, _ -> Text("Settings content") },
                        )
                    },
                )
            }
        }

        // Initially Home is displayed and Discover tab is selected
        composeRule.onNodeWithText("Home content").assertIsDisplayed()
        composeRule.onNodeWithText("Discover").assertIsSelected()
        composeRule.onNodeWithText("Catalogs").assertIsNotSelected()
        composeRule.onNodeWithText("Settings").assertIsNotSelected()

        // Tap Catalogs tab
        composeRule.onNodeWithText("Catalogs").performClick()
        composeRule.onNodeWithText("Catalogs content").assertIsDisplayed()
        composeRule.onNodeWithText("Catalogs").assertIsSelected()
        composeRule.onNodeWithText("Discover").assertIsNotSelected()
        composeRule.runOnIdle {
            assertEquals(JuzgonRoutes.CATALOGS, navController.currentDestination?.route)
        }

        // Tap Settings tab
        composeRule.onNodeWithText("Settings").performClick()
        composeRule.onNodeWithText("Settings content").assertIsDisplayed()
        composeRule.onNodeWithText("Settings").assertIsSelected()
        composeRule.onNodeWithText("Catalogs").assertIsNotSelected()
        composeRule.runOnIdle {
            assertEquals(JuzgonRoutes.SETTINGS, navController.currentDestination?.route)
        }

        // Tap Discover tab to return Home
        composeRule.onNodeWithText("Discover").performClick()
        composeRule.onNodeWithText("Home content").assertIsDisplayed()
        composeRule.onNodeWithText("Discover").assertIsSelected()
        composeRule.runOnIdle {
            assertEquals(JuzgonRoutes.HOME, navController.currentDestination?.route)
        }
    }

    @Test
    fun juzgonAppHidesBottomNavOnSubRoutes() {
        lateinit var navController: TestNavHostController

        composeRule.setContent {
            MaterialTheme {
                navController = rememberTestNavController()
                JuzgonApp(
                    navController = navController,
                    navHost = { controller, modifier ->
                        JuzgonNavHost(
                            navController = controller,
                            modifier = modifier,
                            homeContent = { onCreate, _, _ ->
                                Button(onClick = onCreate) { Text("Create Category") }
                            },
                            createCategoryContent = { onBack, _ ->
                                Button(onClick = onBack) { Text("Back From Create") }
                            },
                        )
                    },
                )
            }
        }

        // On Home route: bottom bar is visible
        composeRule.onNodeWithText("Discover").assertIsDisplayed()
        composeRule.onNodeWithText("Catalogs").assertIsDisplayed()
        composeRule.onNodeWithText("Settings").assertIsDisplayed()

        // Navigate to sub-route (Create Category)
        composeRule.onNodeWithText("Create Category").performClick()
        composeRule.onNodeWithText("Back From Create").assertIsDisplayed()
        composeRule.runOnIdle {
            assertEquals(JuzgonRoutes.CREATE_CATEGORY, navController.currentDestination?.route)
        }

        // Bottom bar items are hidden
        composeRule.onNodeWithText("Discover").assertDoesNotExist()
        composeRule.onNodeWithText("Catalogs").assertDoesNotExist()
        composeRule.onNodeWithText("Settings").assertDoesNotExist()

        // Pop back to Home
        composeRule.onNodeWithText("Back From Create").performClick()
        composeRule.onNodeWithText("Create Category").assertIsDisplayed()

        // Bottom bar is visible again
        composeRule.onNodeWithText("Discover").assertIsDisplayed()
        composeRule.onNodeWithText("Catalogs").assertIsDisplayed()
        composeRule.onNodeWithText("Settings").assertIsDisplayed()
    }

    @Test
    fun juzgonAppBottomNavPreservesHomeAsRootOnBack() {
        lateinit var navController: TestNavHostController

        composeRule.setContent {
            MaterialTheme {
                navController = rememberTestNavController()
                JuzgonApp(
                    navController = navController,
                    navHost = { controller, modifier ->
                        JuzgonNavHost(
                            navController = controller,
                            modifier = modifier,
                            homeContent = { _, _, _ -> Text("Home content") },
                            catalogsContent = { _, _ -> Text("Catalogs content") },
                            settingsContent = { _, _ -> Text("Settings content") },
                        )
                    },
                )
            }
        }

        // Tap Catalogs tab
        composeRule.onNodeWithText("Catalogs").performClick()
        composeRule.onNodeWithText("Catalogs content").assertIsDisplayed()

        // Press Back
        composeRule.runOnIdle {
            navController.popBackStack()
        }

        // Returns to Home
        composeRule.onNodeWithText("Home content").assertIsDisplayed()
        composeRule.runOnIdle {
            assertEquals(JuzgonRoutes.HOME, navController.currentDestination?.route)
        }
    }

    @Test
    fun catalogsCanOpenCreateCategoryRoute() {
        lateinit var navController: TestNavHostController

        composeRule.setContent {
            MaterialTheme {
                navController = rememberTestNavController()
                JuzgonNavHost(
                    navController = navController,
                    homeContent = { _, _, _ -> Text("Home route") },
                    catalogsContent = { _, onCreateCategory ->
                        Button(onClick = onCreateCategory) {
                            Text("Create from Catalogs")
                        }
                    },
                    createCategoryContent = { _, _ -> Text("Create category route") },
                )
            }
        }

        composeRule.runOnIdle {
            navController.navigate(JuzgonRoutes.CATALOGS)
        }
        composeRule.onNodeWithText("Create from Catalogs").performClick()
        composeRule.onNodeWithText("Create category route").assertIsDisplayed()
        composeRule.runOnIdle {
            assertEquals(JuzgonRoutes.CREATE_CATEGORY, navController.currentDestination?.route)
        }
    }

    @Test
    fun homeRouteCanOpenItemDetailRoute() {
        lateinit var navController: TestNavHostController

        composeRule.setContent {
            MaterialTheme {
                navController = rememberTestNavController()
                JuzgonNavHost(
                    navController = navController,
                    homeContent = { _, _, onOpenItem ->
                        Button(onClick = { onOpenItem("Formula 1", "Max Verstappen") }) {
                            Text("Open Item")
                        }
                    },
                    itemDetailContent = { itemId, categoryName, _, _, _, _ ->
                        Text("Detail for $itemId in $categoryName")
                    },
                )
            }
        }

        composeRule.onNodeWithText("Open Item").performClick()
        composeRule.onNodeWithText("Detail for Max Verstappen in Formula 1").assertIsDisplayed()
        composeRule.runOnIdle {
            assertEquals(JuzgonRoutes.ITEM_DETAIL, navController.currentDestination?.route)
            assertEquals("Formula 1", navController.currentBackStackEntry?.arguments?.getString("categoryName"))
            assertEquals("Max Verstappen", navController.currentBackStackEntry?.arguments?.getString("itemId"))
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
