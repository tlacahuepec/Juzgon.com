package com.juzgon.feature.category

import com.juzgon.domain.CatalogType
import com.juzgon.domain.Category
import java.util.Locale

data class CatalogCardUiModel(
    val name: String,
    val itemCount: Int = 0,
    val averageRating: Double? = null,
    val averageRatingText: String =
        averageRating?.let { "★ " + String.format(Locale.US, "%.1f", it) } ?: "★ —",
    val itemCountText: String = if (itemCount == 1) "1 item" else "$itemCount items",
    val type: CatalogType? = null,
)

data class CatalogTypeFilterChip(
    val id: String,
    val label: String,
    val type: CatalogType? = null,
    val isSelected: Boolean = false,
)

data class CatalogsScreenActions(
    val onSearchQueryChange: (String) -> Unit,
    val onFilterSelected: (CatalogType?) -> Unit,
    val onCreateCategoryClick: () -> Unit,
    val onCategoryClick: (String) -> Unit,
    val onRetry: () -> Unit,
)

data class CatalogsUiState(
    val categories: List<CatalogCardUiModel> = emptyList(),
    val searchQuery: String = "",
    val selectedType: CatalogType? = null,
    val filterChips: List<CatalogTypeFilterChip> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
) {
    val isEmpty: Boolean = categories.isEmpty() && !isLoading && errorMessage == null
    val hasSearchQuery: Boolean = searchQuery.isNotBlank()
}

sealed interface CatalogsNavigationEvent {
    data object CreateCategory : CatalogsNavigationEvent

    data class OpenCategory(
        val categoryName: String,
    ) : CatalogsNavigationEvent
}

object CatalogsStateReducer {
    private val DEFAULT_FILTER_TYPES =
        listOf(
            CatalogType.PERSON,
            CatalogType.MOVIE,
            CatalogType.PRODUCT,
        )

    fun reduce(
        categoriesWithRatings: List<Pair<Category, Double?>>,
        searchQuery: String,
        selectedType: CatalogType?,
    ): CatalogsUiState {
        val normalizedQuery = searchQuery.trim()

        val filtered =
            categoriesWithRatings
                .filter { (category, _) ->
                    val matchesSearch =
                        normalizedQuery.isBlank() ||
                            category.name.contains(normalizedQuery, ignoreCase = true)
                    val matchesType = selectedType == null || category.type == selectedType
                    matchesSearch && matchesType
                }.map { (category, avgRating) ->
                    CatalogCardUiModel(
                        name = category.name,
                        itemCount = category.itemCount,
                        averageRating = avgRating,
                        type = category.type,
                    )
                }

        val presentTypes = categoriesWithRatings.mapNotNull { it.first.type }.distinct()
        val allTypes = (DEFAULT_FILTER_TYPES + presentTypes).distinct()

        val chips =
            buildList {
                add(
                    CatalogTypeFilterChip(
                        id = "all",
                        label = "All",
                        type = null,
                        isSelected = selectedType == null,
                    ),
                )
                allTypes.forEach { type ->
                    add(
                        CatalogTypeFilterChip(
                            id = type.name,
                            label = type.toFilterLabel(),
                            type = type,
                            isSelected = selectedType == type,
                        ),
                    )
                }
            }

        return CatalogsUiState(
            categories = filtered,
            searchQuery = searchQuery,
            selectedType = selectedType,
            filterChips = chips,
            isLoading = false,
        )
    }
}

fun CatalogType.toFilterLabel(): String =
    when (this) {
        CatalogType.PERSON -> "People"
        CatalogType.MOVIE -> "Films"
        CatalogType.PRODUCT -> "Products"
        CatalogType.CHARACTER -> "Characters"
        CatalogType.TV_SHOW -> "TV Shows"
        CatalogType.VIDEO_GAME -> "Games"
        CatalogType.SONG -> "Songs"
        CatalogType.ALBUM -> "Albums"
        CatalogType.SPORTS_TEAM -> "Sports"
        CatalogType.COMPANY -> "Companies"
        CatalogType.PLACE -> "Places"
        CatalogType.OTHER -> "Other"
    }
