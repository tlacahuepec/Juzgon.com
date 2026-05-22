package com.juzgon.feature.category

import com.juzgon.domain.AttributeType
import com.juzgon.domain.Category
import com.juzgon.domain.RankedRatedItem
import com.juzgon.domain.RatedItem
import java.util.Locale

enum class CategoryDetailSortOption {
    Score,
    Name,
}

sealed interface CategoryDetailNavigationEvent {
    data object NavigateToEditCategory : CategoryDetailNavigationEvent

    data object NavigateBack : CategoryDetailNavigationEvent
}

data class CategoryDetailItemUiModel(
    val id: String,
    val averageScoreText: String,
    val imageValue: String? = null,
)

data class CategoryDetailUiState(
    val categoryName: String = "",
    val attributeSummary: String = "",
    val items: List<CategoryDetailItemUiModel> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val sortOption: CategoryDetailSortOption = CategoryDetailSortOption.Score,
    val showDeleteConfirmDialog: Boolean = false,
    val showDeleteWithItemsWarning: Boolean = false,
    val isDeleting: Boolean = false,
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
                        imageValue = rankedItem.item.primaryImageValue(category),
                    )
                },
            isLoading = false,
            sortOption = sortOption,
        )
    }
}

private fun RatedItem.primaryImageValue(category: Category): String? {
    val imageAttribute =
        category.attributes.firstOrNull { attribute ->
            attribute.type == AttributeType.IMAGE
        } ?: return null
    return values
        .firstOrNull { value -> value.attribute.id == imageAttribute.id }
        ?.value
        ?.takeIf { value -> value.isNotBlank() }
}

private fun Int.toAttributeSummary(): String =
    if (this == 1) {
        "1 attribute"
    } else {
        "$this attributes"
    }

private fun Double.toAverageScoreText(): String = String.format(Locale.US, "%.1f", this)
