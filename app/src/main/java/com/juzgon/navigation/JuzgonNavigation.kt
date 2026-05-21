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

private const val CATEGORY_NAME_ARGUMENT = "categoryName"
private const val ITEM_ID_ARGUMENT = "itemId"

object JuzgonRoutes {
    const val HOME = "home"
    const val CREATE_CATEGORY = "category/create"
    const val EDIT_CATEGORY = "category/edit/{$CATEGORY_NAME_ARGUMENT}"
    const val CATEGORY_DETAIL = "category/{$CATEGORY_NAME_ARGUMENT}"
    const val ITEM_DETAIL = "item/detail/{$CATEGORY_NAME_ARGUMENT}/{$ITEM_ID_ARGUMENT}"
    const val CREATE_ITEM = "item/create/{$CATEGORY_NAME_ARGUMENT}"
    const val EDIT_ITEM = "item/edit/{$CATEGORY_NAME_ARGUMENT}/{$ITEM_ID_ARGUMENT}"

    fun categoryDetail(categoryName: String): String = "category/${Uri.encode(categoryName)}"

    fun editCategory(categoryName: String): String = "category/edit/${Uri.encode(categoryName)}"

    fun itemDetail(
        categoryName: String,
        itemId: String,
    ): String = "item/detail/${Uri.encode(categoryName)}/${Uri.encode(itemId)}"

    fun createItem(categoryName: String): String = "item/create/${Uri.encode(categoryName)}"

    fun editItem(
        categoryName: String,
        itemId: String,
    ): String = "item/edit/${Uri.encode(categoryName)}/${Uri.encode(itemId)}"
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
    editCategoryContent: @Composable (
        categoryName: String,
        onBack: () -> Unit,
        onSaveCompleted: () -> Unit,
    ) -> Unit = { categoryName, onBack, onSaveCompleted ->
        CategoryFormRoute(
            categoryName = categoryName,
            onBackClick = onBack,
            onSaveCompleted = onSaveCompleted,
        )
    },
    categoryDetailContent: @Composable (
        categoryName: String,
        onBack: () -> Unit,
        onAddItem: () -> Unit,
        onEditItem: (String) -> Unit,
        onEditCategory: () -> Unit,
        onDeleteComplete: () -> Unit,
    ) -> Unit = { categoryName, onBack, onAddItem, onEditItem, onEditCategory, onDeleteComplete ->
        CategoryDetailRoute(
            categoryName = categoryName,
            onBackClick = onBack,
            onAddItemClick = onAddItem,
            onEditItemClick = onEditItem,
            onEditCategoryClick = onEditCategory,
            onDeleteCategoryComplete = onDeleteComplete,
        )
    },
    itemFormContent: @Composable (
        categoryName: String,
        itemId: String?,
        onBack: () -> Unit,
        onSaveCompleted: () -> Unit,
        onDeleteCompleted: () -> Unit,
    ) -> Unit = { categoryName, itemId, onBack, onSaveCompleted, onDeleteCompleted ->
        ItemFormRoute(
            categoryName = categoryName,
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
                { categoryName ->
                    navController.navigate(JuzgonRoutes.categoryDetail(categoryName)) {
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
            route = JuzgonRoutes.EDIT_CATEGORY,
            arguments =
                listOf(
                    navArgument(CATEGORY_NAME_ARGUMENT) {
                        type = NavType.StringType
                    },
                ),
        ) { backStackEntry ->
            val categoryName =
                Uri.decode(backStackEntry.arguments?.getString(CATEGORY_NAME_ARGUMENT).orEmpty())
            val returnUp = {
                if (!navController.navigateUp()) {
                    navController.navigate(JuzgonRoutes.HOME) {
                        launchSingleTop = true
                    }
                }
            }
            val returnHome = {
                if (!navController.popBackStack(JuzgonRoutes.HOME, inclusive = false)) {
                    navController.navigate(JuzgonRoutes.HOME) {
                        launchSingleTop = true
                    }
                }
            }
            editCategoryContent(categoryName, returnUp, returnHome)
        }
        composable(
            route = JuzgonRoutes.CATEGORY_DETAIL,
            arguments =
                listOf(
                    navArgument(CATEGORY_NAME_ARGUMENT) {
                        type = NavType.StringType
                    },
                ),
        ) { backStackEntry ->
            val categoryName =
                Uri.decode(backStackEntry.arguments?.getString(CATEGORY_NAME_ARGUMENT).orEmpty())
            val returnBack = {
                if (!navController.navigateUp()) {
                    navController.navigate(JuzgonRoutes.HOME) {
                        launchSingleTop = true
                    }
                }
            }
            val openAddItem = {
                navController.navigate(JuzgonRoutes.createItem(categoryName)) {
                    launchSingleTop = true
                }
            }
            val openItemDetail = { itemId: String ->
                navController.navigate(JuzgonRoutes.itemDetail(categoryName, itemId)) {
                    launchSingleTop = true
                }
            }
            val openEditCategory = {
                navController.navigate(JuzgonRoutes.editCategory(categoryName)) {
                    launchSingleTop = true
                }
            }
            val returnHome = {
                if (!navController.popBackStack(JuzgonRoutes.HOME, inclusive = false)) {
                    navController.navigate(JuzgonRoutes.HOME) {
                        launchSingleTop = true
                    }
                }
            }
            categoryDetailContent(
                categoryName,
                returnBack,
                openAddItem,
                openItemDetail,
                openEditCategory,
                returnHome,
            )
        }
        composable(
            route = JuzgonRoutes.ITEM_DETAIL,
            arguments =
                listOf(
                    navArgument(CATEGORY_NAME_ARGUMENT) {
                        type = NavType.StringType
                    },
                    navArgument(ITEM_ID_ARGUMENT) {
                        type = NavType.StringType
                    },
                ),
        ) { backStackEntry ->
            val categoryName =
                Uri.decode(backStackEntry.arguments?.getString(CATEGORY_NAME_ARGUMENT).orEmpty())
            val itemId = Uri.decode(backStackEntry.arguments?.getString(ITEM_ID_ARGUMENT).orEmpty())
            val returnBack = {
                if (!navController.navigateUp()) {
                    navController.navigate(JuzgonRoutes.categoryDetail(categoryName)) {
                        launchSingleTop = true
                    }
                }
            }
            val openEdit = {
                navController.navigate(JuzgonRoutes.editItem(categoryName, itemId)) {
                    launchSingleTop = true
                }
            }
            itemDetailContent(itemId, returnBack, openEdit)
        }
        composable(
            route = JuzgonRoutes.CREATE_ITEM,
            arguments =
                listOf(
                    navArgument(CATEGORY_NAME_ARGUMENT) {
                        type = NavType.StringType
                    },
                ),
        ) { backStackEntry ->
            val categoryName =
                Uri.decode(backStackEntry.arguments?.getString(CATEGORY_NAME_ARGUMENT).orEmpty())
            val returnToCategory = {
                if (!navController.popBackStack(JuzgonRoutes.CATEGORY_DETAIL, inclusive = false)) {
                    navController.navigate(JuzgonRoutes.categoryDetail(categoryName)) {
                        launchSingleTop = true
                    }
                }
            }
            itemFormContent(
                categoryName,
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
                    navArgument(CATEGORY_NAME_ARGUMENT) {
                        type = NavType.StringType
                    },
                    navArgument(ITEM_ID_ARGUMENT) {
                        type = NavType.StringType
                    },
                ),
        ) { backStackEntry ->
            val categoryName =
                Uri.decode(backStackEntry.arguments?.getString(CATEGORY_NAME_ARGUMENT).orEmpty())
            val itemId = Uri.decode(backStackEntry.arguments?.getString(ITEM_ID_ARGUMENT).orEmpty())
            val returnToCategory = {
                if (!navController.popBackStack(JuzgonRoutes.CATEGORY_DETAIL, inclusive = false)) {
                    navController.navigate(JuzgonRoutes.categoryDetail(categoryName)) {
                        launchSingleTop = true
                    }
                }
            }
            itemFormContent(
                categoryName,
                itemId,
                returnToCategory,
                returnToCategory,
                returnToCategory,
            )
        }
    }
}
