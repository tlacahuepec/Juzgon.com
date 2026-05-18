package com.juzgon.feature.home

import com.juzgon.domain.Category
import java.util.Locale

enum class HomeSortOption {
    Recent,
    Name,
}

sealed interface HomeNavigationEvent {
    data object CreateCategory : HomeNavigationEvent

    data class OpenCategory(
        val categoryName: String,
    ) : HomeNavigationEvent
}

data class HomeCategoryUiModel(
    val name: String,
    val attributeCount: Int,
)

data class HomeScreenActions(
    val onSearchQueryChange: (String) -> Unit,
    val onSortOptionSelected: (HomeSortOption) -> Unit,
    val onCreateCategoryClick: () -> Unit,
    val onCategoryClick: (String) -> Unit,
)

data class HomeUiState(
    val searchQuery: String = "",
    val sortOption: HomeSortOption = HomeSortOption.Recent,
    val categories: List<HomeCategoryUiModel> = emptyList(),
) {
    val hasSearchQuery: Boolean = searchQuery.isNotBlank()
    val isEmpty: Boolean = categories.isEmpty()
}

object HomeStateReducer {
    fun reduce(
        categories: List<Category>,
        searchQuery: String,
        sortOption: HomeSortOption,
    ): HomeUiState {
        val normalizedQuery = searchQuery.trim()
        val visibleCategories =
            categories
                .filter { category ->
                    normalizedQuery.isBlank() ||
                        category.name.contains(normalizedQuery, ignoreCase = true)
                }.let { filteredCategories ->
                    when (sortOption) {
                        HomeSortOption.Recent -> filteredCategories
                        HomeSortOption.Name ->
                            filteredCategories.sortedWith(
                                compareBy<Category> { it.name.lowercase(Locale.ROOT) }
                                    .thenBy { it.name },
                            )
                    }
                }.map { category ->
                    HomeCategoryUiModel(
                        name = category.name,
                        attributeCount = category.attributes.size,
                    )
                }

        return HomeUiState(
            searchQuery = searchQuery,
            sortOption = sortOption,
            categories = visibleCategories,
        )
    }
}
