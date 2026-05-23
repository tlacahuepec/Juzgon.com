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
import com.juzgon.feature.scoreprofile.ScoreProfileFormRoute
import com.juzgon.feature.scoreprofile.ScoreProfileListRoute

private const val CATEGORY_NAME_ARGUMENT = "categoryName"
private const val ITEM_ID_ARGUMENT = "itemId"
private const val PROFILE_ID_ARGUMENT = "profileId"
private const val ACTIVE_PROFILE_ID_ARGUMENT = "activeProfileId"

object JuzgonRoutes {
    const val HOME = "home"
    const val CREATE_CATEGORY = "category/create"
    const val EDIT_CATEGORY = "category/edit/{$CATEGORY_NAME_ARGUMENT}"
    const val CATEGORY_DETAIL = "category/{$CATEGORY_NAME_ARGUMENT}"
    const val ITEM_DETAIL =
        "item/detail/{$CATEGORY_NAME_ARGUMENT}/{$ITEM_ID_ARGUMENT}" +
            "?$ACTIVE_PROFILE_ID_ARGUMENT={$ACTIVE_PROFILE_ID_ARGUMENT}"
    const val CREATE_ITEM = "item/create/{$CATEGORY_NAME_ARGUMENT}"
    const val EDIT_ITEM = "item/edit/{$CATEGORY_NAME_ARGUMENT}/{$ITEM_ID_ARGUMENT}"
    const val SCORE_PROFILES = "score-profiles/{$CATEGORY_NAME_ARGUMENT}"
    const val SCORE_PROFILE_FORM =
        "score-profile/edit/{$CATEGORY_NAME_ARGUMENT}?$PROFILE_ID_ARGUMENT={$PROFILE_ID_ARGUMENT}"

    fun categoryDetail(categoryName: String): String = "category/${Uri.encode(categoryName)}"

    fun editCategory(categoryName: String): String = "category/edit/${Uri.encode(categoryName)}"

    fun itemDetail(
        categoryName: String,
        itemId: String,
        activeProfileId: String? = null,
    ): String {
        val base = "item/detail/${Uri.encode(categoryName)}/${Uri.encode(itemId)}"
        return if (activeProfileId != null) {
            "$base?$ACTIVE_PROFILE_ID_ARGUMENT=${Uri.encode(activeProfileId)}"
        } else {
            base
        }
    }

    fun createItem(categoryName: String): String = "item/create/${Uri.encode(categoryName)}"

    fun editItem(
        categoryName: String,
        itemId: String,
    ): String = "item/edit/${Uri.encode(categoryName)}/${Uri.encode(itemId)}"

    fun scoreProfiles(categoryName: String): String = "score-profiles/${Uri.encode(categoryName)}"

    fun scoreProfileForm(
        categoryName: String,
        profileId: String? = null,
    ): String {
        val base = "score-profile/edit/${Uri.encode(categoryName)}"
        return if (profileId != null) "$base?$PROFILE_ID_ARGUMENT=${Uri.encode(profileId)}" else base
    }
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
        onEditItem: (String, String?) -> Unit,
        onEditCategory: () -> Unit,
        onDeleteComplete: () -> Unit,
        onScoreProfiles: () -> Unit,
    ) -> Unit = {
        categoryName,
        onBack,
        onAddItem,
        onEditItem,
        onEditCategory,
        onDeleteComplete,
        onScoreProfiles,
        ->
        CategoryDetailRoute(
            categoryName = categoryName,
            onBackClick = onBack,
            onAddItemClick = onAddItem,
            onEditItemClick = onEditItem,
            onEditCategoryClick = onEditCategory,
            onDeleteCategoryComplete = onDeleteComplete,
            onScoreProfilesClick = onScoreProfiles,
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
        categoryName: String,
        activeProfileId: String?,
        onBack: () -> Unit,
        onEditClick: () -> Unit,
        onDeleteCompleted: () -> Unit,
    ) -> Unit = { itemId, categoryName, activeProfileId, onBack, onEditClick, onDeleteCompleted ->
        ItemDetailRoute(
            itemId = itemId,
            categoryName = categoryName,
            activeProfileId = activeProfileId,
            onBackClick = onBack,
            onEditClick = onEditClick,
            onDeleteCompleted = onDeleteCompleted,
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
            val openItemDetail = { itemId: String, activeProfileId: String? ->
                navController.navigate(
                    JuzgonRoutes.itemDetail(categoryName, itemId, activeProfileId),
                ) {
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
            val openScoreProfiles = {
                navController.navigate(JuzgonRoutes.scoreProfiles(categoryName)) {
                    launchSingleTop = true
                }
            }
            categoryDetailContent(
                categoryName,
                returnBack,
                openAddItem,
                openItemDetail,
                openEditCategory,
                returnHome,
                openScoreProfiles,
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
                    navArgument(ACTIVE_PROFILE_ID_ARGUMENT) {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                ),
        ) { backStackEntry ->
            val categoryName =
                Uri.decode(backStackEntry.arguments?.getString(CATEGORY_NAME_ARGUMENT).orEmpty())
            val itemId = Uri.decode(backStackEntry.arguments?.getString(ITEM_ID_ARGUMENT).orEmpty())
            val activeProfileId = backStackEntry.arguments?.getString(ACTIVE_PROFILE_ID_ARGUMENT)
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
            itemDetailContent(itemId, categoryName, activeProfileId, returnBack, openEdit, returnBack)
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
        composable(
            route = JuzgonRoutes.SCORE_PROFILES,
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
                    navController.navigate(JuzgonRoutes.categoryDetail(categoryName)) {
                        launchSingleTop = true
                    }
                }
            }
            val openCreate = {
                navController.navigate(JuzgonRoutes.scoreProfileForm(categoryName)) {
                    launchSingleTop = true
                }
            }
            val openEdit = { profileId: String ->
                navController.navigate(
                    JuzgonRoutes.scoreProfileForm(categoryName, profileId),
                ) {
                    launchSingleTop = true
                }
            }
            ScoreProfileListRoute(
                categoryName = categoryName,
                onBackClick = returnBack,
                onCreateClick = openCreate,
                onEditClick = openEdit,
            )
        }
        composable(
            route = JuzgonRoutes.SCORE_PROFILE_FORM,
            arguments =
                listOf(
                    navArgument(CATEGORY_NAME_ARGUMENT) {
                        type = NavType.StringType
                    },
                    navArgument(PROFILE_ID_ARGUMENT) {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                ),
        ) { backStackEntry ->
            val categoryName =
                Uri.decode(backStackEntry.arguments?.getString(CATEGORY_NAME_ARGUMENT).orEmpty())
            val profileId =
                backStackEntry.arguments?.getString(PROFILE_ID_ARGUMENT)?.let { Uri.decode(it) }
            val returnToList = {
                if (!navController.popBackStack(
                        JuzgonRoutes.SCORE_PROFILES,
                        inclusive = false,
                    )
                ) {
                    navController.navigate(JuzgonRoutes.scoreProfiles(categoryName)) {
                        launchSingleTop = true
                    }
                }
            }
            ScoreProfileFormRoute(
                categoryName = categoryName,
                profileId = profileId,
                onBackClick = returnToList,
                onSaveCompleted = returnToList,
            )
        }
    }
}
