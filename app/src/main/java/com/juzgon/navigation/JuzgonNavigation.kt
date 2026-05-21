@file:Suppress("FunctionName", "LongMethod", "LongParameterList", "MatchingDeclarationName")

package com.juzgon.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.juzgon.feature.category.CategoryDetailRoute
import com.juzgon.feature.category.CategoryFormRoute
import com.juzgon.feature.home.HomeRoute
import com.juzgon.feature.item.ItemDetailRoute
import com.juzgon.feature.item.ItemFormRoute

private const val CATEGORY_ID_ARGUMENT = "categoryId"
private const val ITEM_ID_ARGUMENT = "itemId"

object JuzgonRoutes {
    const val HOME = "home"
    const val CREATE_CATEGORY = "category/create"
    const val CATEGORY_DETAIL = "category/{$CATEGORY_ID_ARGUMENT}"
    const val ITEM_DETAIL = "item/detail/{$CATEGORY_ID_ARGUMENT}/{$ITEM_ID_ARGUMENT}"
    const val CREATE_ITEM = "item/create/{$CATEGORY_ID_ARGUMENT}"
    const val EDIT_ITEM = "item/edit/{$CATEGORY_ID_ARGUMENT}/{$ITEM_ID_ARGUMENT}"

    fun categoryDetail(categoryId: String): String = "category/${Uri.encode(categoryId)}"

    fun itemDetail(
        categoryId: String,
        itemId: String,
    ): String = "item/detail/${Uri.encode(categoryId)}/${Uri.encode(itemId)}"

    fun createItem(categoryId: String): String = "item/create/${Uri.encode(categoryId)}"

    fun editItem(
        categoryId: String,
        itemId: String,
    ): String = "item/edit/${Uri.encode(categoryId)}/${Uri.encode(itemId)}"
}

@Composable
fun JuzgonApp(modifier: Modifier = Modifier) {
    JuzgonNavHost(
        navController = rememberNavController(),
        modifier = modifier,
    )
}

@Composable
internal fun JuzgonNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    homeContent: @Composable (
        onCreateCategory: () -> Unit,
        onOpenCategory: (String) -> Unit,
    ) -> Unit = { onCreateCategory, onOpenCategory ->
        HomeRoute(
            onNavigateToCreateCategory = onCreateCategory,
            onNavigateToCategory = onOpenCategory,
        )
    },
    createCategoryContent: @Composable (
        onBack: () -> Unit,
        onSaveCompleted: () -> Unit,
    ) -> Unit = { onBack, onSaveCompleted ->
        CategoryFormRoute(
            onBackClick = onBack,
            onSaveCompleted = onSaveCompleted,
        )
    },
    categoryDetailContent: @Composable (
        categoryId: String,
        onBack: () -> Unit,
        onAddItem: () -> Unit,
        onEditItem: (String) -> Unit,
    ) -> Unit = { categoryId, onBack, onAddItem, onEditItem ->
        CategoryDetailRoute(
            categoryId = categoryId,
            onBackClick = onBack,
            onAddItemClick = onAddItem,
            onEditItemClick = onEditItem,
        )
    },
    itemFormContent: @Composable (
        categoryId: String,
        itemId: String?,
        onBack: () -> Unit,
        onSaveCompleted: () -> Unit,
        onDeleteCompleted: () -> Unit,
    ) -> Unit = { categoryId, itemId, onBack, onSaveCompleted, onDeleteCompleted ->
        ItemFormRoute(
            categoryId = categoryId,
            itemId = itemId,
            onBackClick = onBack,
            onSaveCompleted = onSaveCompleted,
            onDeleteCompleted = onDeleteCompleted,
        )
    },
    itemDetailContent: @Composable (
        itemId: String,
        onBack: () -> Unit,
        onEditClick: () -> Unit,
    ) -> Unit = { itemId, onBack, onEditClick ->
        ItemDetailRoute(
            itemId = itemId,
            onBackClick = onBack,
            onEditClick = onEditClick,
        )
    },
) {
    NavHost(
        navController = navController,
        startDestination = JuzgonRoutes.HOME,
        modifier = modifier,
    ) {
        composable(JuzgonRoutes.HOME) {
            homeContent(
                {
                    navController.navigate(JuzgonRoutes.CREATE_CATEGORY) {
                        launchSingleTop = true
                    }
                },
                { categoryId ->
                    navController.navigate(JuzgonRoutes.categoryDetail(categoryId)) {
                        launchSingleTop = true
                    }
                },
            )
        }
        composable(JuzgonRoutes.CREATE_CATEGORY) {
            val returnToHome = {
                if (!navController.popBackStack(JuzgonRoutes.HOME, inclusive = false)) {
                    navController.navigate(JuzgonRoutes.HOME) {
                        launchSingleTop = true
                    }
                }
            }
            createCategoryContent(
                returnToHome,
                returnToHome,
            )
        }
        composable(
            route = JuzgonRoutes.CATEGORY_DETAIL,
            arguments =
                listOf(
                    navArgument(CATEGORY_ID_ARGUMENT) {
                        type = NavType.StringType
                    },
                ),
        ) { backStackEntry ->
            val categoryId =
                Uri.decode(backStackEntry.arguments?.getString(CATEGORY_ID_ARGUMENT).orEmpty())
            val returnBack = {
                if (!navController.navigateUp()) {
                    navController.navigate(JuzgonRoutes.HOME) {
                        launchSingleTop = true
                    }
                }
            }
            val openAddItem = {
                navController.navigate(JuzgonRoutes.createItem(categoryId)) {
                    launchSingleTop = true
                }
            }
            val openItemDetail = { itemId: String ->
                navController.navigate(JuzgonRoutes.itemDetail(categoryId, itemId)) {
                    launchSingleTop = true
                }
            }
            categoryDetailContent(
                categoryId,
                returnBack,
                openAddItem,
                openItemDetail,
            )
        }
        composable(
            route = JuzgonRoutes.ITEM_DETAIL,
            arguments =
                listOf(
                    navArgument(CATEGORY_ID_ARGUMENT) {
                        type = NavType.StringType
                    },
                    navArgument(ITEM_ID_ARGUMENT) {
                        type = NavType.StringType
                    },
                ),
        ) { backStackEntry ->
            val categoryId =
                Uri.decode(backStackEntry.arguments?.getString(CATEGORY_ID_ARGUMENT).orEmpty())
            val itemId = Uri.decode(backStackEntry.arguments?.getString(ITEM_ID_ARGUMENT).orEmpty())
            val returnBack = {
                if (!navController.navigateUp()) {
                    navController.navigate(JuzgonRoutes.categoryDetail(categoryId)) {
                        launchSingleTop = true
                    }
                }
            }
            val openEdit = {
                navController.navigate(JuzgonRoutes.editItem(categoryId, itemId)) {
                    launchSingleTop = true
                }
            }
            itemDetailContent(itemId, returnBack, openEdit)
        }
        composable(
            route = JuzgonRoutes.CREATE_ITEM,
            arguments =
                listOf(
                    navArgument(CATEGORY_ID_ARGUMENT) {
                        type = NavType.StringType
                    },
                ),
        ) { backStackEntry ->
            val categoryId =
                Uri.decode(backStackEntry.arguments?.getString(CATEGORY_ID_ARGUMENT).orEmpty())
            val returnToCategory = {
                if (!navController.popBackStack(JuzgonRoutes.CATEGORY_DETAIL, inclusive = false)) {
                    navController.navigate(JuzgonRoutes.categoryDetail(categoryId)) {
                        launchSingleTop = true
                    }
                }
            }
            itemFormContent(
                categoryId,
                null,
                returnToCategory,
                returnToCategory,
                returnToCategory,
            )
        }
        composable(
            route = JuzgonRoutes.EDIT_ITEM,
            arguments =
                listOf(
                    navArgument(CATEGORY_ID_ARGUMENT) {
                        type = NavType.StringType
                    },
                    navArgument(ITEM_ID_ARGUMENT) {
                        type = NavType.StringType
                    },
                ),
        ) { backStackEntry ->
            val categoryId =
                Uri.decode(backStackEntry.arguments?.getString(CATEGORY_ID_ARGUMENT).orEmpty())
            val itemId = Uri.decode(backStackEntry.arguments?.getString(ITEM_ID_ARGUMENT).orEmpty())
            val returnToCategory = {
                if (!navController.popBackStack(JuzgonRoutes.CATEGORY_DETAIL, inclusive = false)) {
                    navController.navigate(JuzgonRoutes.categoryDetail(categoryId)) {
                        launchSingleTop = true
                    }
                }
            }
            itemFormContent(
                categoryId,
                itemId,
                returnToCategory,
                returnToCategory,
                returnToCategory,
            )
        }
    }
}
