@file:Suppress("FunctionName", "MatchingDeclarationName")

package com.juzgon.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.juzgon.feature.category.CategoryFormRoute
import com.juzgon.feature.home.HomeRoute

object JuzgonRoutes {
    const val HOME = "home"
    const val CREATE_CATEGORY = "category/create"
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
    homeContent: @Composable (onCreateCategory: () -> Unit) -> Unit = { onCreateCategory ->
        HomeRoute(onNavigateToCreateCategory = onCreateCategory)
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
) {
    NavHost(
        navController = navController,
        startDestination = JuzgonRoutes.HOME,
        modifier = modifier,
    ) {
        composable(JuzgonRoutes.HOME) {
            homeContent {
                navController.navigate(JuzgonRoutes.CREATE_CATEGORY) {
                    launchSingleTop = true
                }
            }
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
    }
}
