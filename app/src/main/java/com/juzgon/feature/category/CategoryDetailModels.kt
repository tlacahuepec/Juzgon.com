package com.juzgon.feature.category

import com.juzgon.domain.Category
import com.juzgon.domain.RankedRatedItem
import java.util.Locale

enum class CategoryDetailSortOption {
    Score,
    Name,
}

data class CategoryDetailItemUiModel(
    val id: String,
    val averageScoreText: String,
)

data class CategoryDetailUiState(
    val categoryName: String = "",
    val attributeSummary: String = "",
    val items: List<CategoryDetailItemUiModel> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val sortOption: CategoryDetailSortOption = CategoryDetailSortOption.Score,
) {
    val hasItems: Boolean = items.isNotEmpty()
}

object CategoryDetailReducer {
    fun loading(categoryName: String): CategoryDetailUiState = CategoryDetailUiState(categoryName = categoryName)

    fun reduce(
        categoryName: String,
        category: Category?,
        rankedItems: List<RankedRatedItem>,
        sortOption: CategoryDetailSortOption,
    ): CategoryDetailUiState {
        if (category == null) {
            return CategoryDetailUiState(
                categoryName = categoryName,
                isLoading = false,
                errorMessage = "Category not found",
            )
        }

        val sortedItems =
            when (sortOption) {
                CategoryDetailSortOption.Score ->
                    rankedItems.sortedWith(
                        compareByDescending<RankedRatedItem> { it.aggregateScore }
                            .thenBy { it.item.id },
                    )
                CategoryDetailSortOption.Name ->
                    rankedItems.sortedBy { it.item.id }
            }

        return CategoryDetailUiState(
            categoryName = category.name,
            attributeSummary = category.attributes.size.toAttributeSummary(),
            items =
                sortedItems.map { rankedItem ->
                    CategoryDetailItemUiModel(
                        id = rankedItem.item.id,
                        averageScoreText = rankedItem.aggregateScore.toAverageScoreText(),
                    )
                },
            isLoading = false,
            sortOption = sortOption,
        )
    }
}

private fun Int.toAttributeSummary(): String =
    if (this == 1) {
        "1 attribute"
    } else {
        "$this attributes"
    }

private fun Double.toAverageScoreText(): String = String.format(Locale.US, "%.1f", this)
