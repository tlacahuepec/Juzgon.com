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
import com.juzgon.feature.item.ItemFormRoute

private const val CATEGORY_NAME_ARGUMENT = "categoryName"

object JuzgonRoutes {
    const val HOME = "home"
    const val CREATE_CATEGORY = "category/create"
    const val CATEGORY_DETAIL = "category/{$CATEGORY_NAME_ARGUMENT}"
    const val CREATE_ITEM = "item/create/{$CATEGORY_NAME_ARGUMENT}"

    fun categoryDetail(categoryName: String): String = "category/${Uri.encode(categoryName)}"

    fun createItem(categoryName: String): String = "item/create/${Uri.encode(categoryName)}"
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
        categoryName: String,
        onBack: () -> Unit,
        onAddItem: () -> Unit,
    ) -> Unit = { categoryName, onBack, onAddItem ->
        CategoryDetailRoute(
            categoryName = categoryName,
            onBackClick = onBack,
            onAddItemClick = onAddItem,
        )
    },
    itemFormContent: @Composable (
        categoryName: String,
        onBack: () -> Unit,
        onSaveCompleted: () -> Unit,
    ) -> Unit = { categoryName, onBack, onSaveCompleted ->
        ItemFormRoute(
            categoryName = categoryName,
            onBackClick = onBack,
            onSaveCompleted = onSaveCompleted,
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
            categoryDetailContent(
                categoryName,
                returnBack,
                openAddItem,
            )
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
                returnToCategory,
                returnToCategory,
            )
        }
    }
}
