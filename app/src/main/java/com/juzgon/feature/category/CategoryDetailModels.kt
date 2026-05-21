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
    val name: String,
    val averageScoreText: String,
)

data class CategoryDetailUiState(
    val categoryId: String = "",
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
    fun loading(categoryId: String): CategoryDetailUiState = CategoryDetailUiState(categoryId = categoryId)

    fun reduce(
        categoryId: String,
        category: Category?,
        rankedItems: List<RankedRatedItem>,
        sortOption: CategoryDetailSortOption,
    ): CategoryDetailUiState {
        if (category == null) {
            return CategoryDetailUiState(
                categoryId = categoryId,
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
            categoryId = category.id,
            categoryName = category.name,
            attributeSummary = category.attributes.size.toAttributeSummary(),
            items =
                sortedItems.map { rankedItem ->
                    CategoryDetailItemUiModel(
                        id = rankedItem.item.id,
                        name = rankedItem.item.name,
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
