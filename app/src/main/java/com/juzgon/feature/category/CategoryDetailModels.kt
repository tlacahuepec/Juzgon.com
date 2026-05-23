package com.juzgon.feature.category

import com.juzgon.domain.AttributeType
import com.juzgon.domain.Category
import com.juzgon.domain.RankedRatedItem
import com.juzgon.domain.RatedItem
import java.util.Locale

sealed interface CategoryDetailSortOption {
    data object Score : CategoryDetailSortOption

    data object Name : CategoryDetailSortOption

    data class Attribute(
        val attributeId: String,
    ) : CategoryDetailSortOption
}

sealed interface CategoryDetailNavigationEvent {
    data object NavigateToEditCategory : CategoryDetailNavigationEvent

    data object NavigateBack : CategoryDetailNavigationEvent
}

data class CategoryDetailItemUiModel(
    val rank: Int,
    val id: String,
    val averageScoreText: String,
    val imageValue: String? = null,
)

data class CategoryDetailUiState(
    val categoryName: String = "",
    val attributeSummary: String = "",
    val items: List<CategoryDetailItemUiModel> = emptyList(),
    val sortOptions: List<CategoryDetailSortOptionUiModel> = defaultSortOptions(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val sortOption: CategoryDetailSortOption = CategoryDetailSortOption.Score,
    val showDeleteConfirmDialog: Boolean = false,
    val showDeleteWithItemsWarning: Boolean = false,
    val isDeleting: Boolean = false,
) {
    val hasItems: Boolean = items.isNotEmpty()
}

data class CategoryDetailSortOptionUiModel(
    val option: CategoryDetailSortOption,
    val label: String,
    val contentDescription: String,
)

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
                sortOptions = defaultSortOptions(),
                isLoading = false,
                errorMessage = "Category not found",
            )
        }

        val sortOptions = category.toSortOptions()
        val selectedSortOption =
            sortOption.takeIf { option ->
                sortOptions.any { it.option == option }
            } ?: CategoryDetailSortOption.Score
        val sortedItems =
            when (selectedSortOption) {
                CategoryDetailSortOption.Score ->
                    rankedItems.sortedWith(
                        compareByDescending<RankedRatedItem> { it.aggregateScore }
                            .thenBy { it.item.id },
                    )
                CategoryDetailSortOption.Name ->
                    rankedItems.sortedBy { it.item.id }
                is CategoryDetailSortOption.Attribute ->
                    rankedItems.sortedByAttribute(
                        category = category,
                        attributeId = selectedSortOption.attributeId,
                    )
            }

        return CategoryDetailUiState(
            categoryName = category.name,
            attributeSummary = category.attributes.size.toAttributeSummary(),
            items =
                sortedItems.mapIndexed { index, rankedItem ->
                    CategoryDetailItemUiModel(
                        rank = index + 1,
                        id = rankedItem.item.id,
                        averageScoreText = rankedItem.aggregateScore.toAverageScoreText(),
                        imageValue = rankedItem.item.primaryImageValue(category),
                    )
                },
            isLoading = false,
            sortOption = selectedSortOption,
            sortOptions = sortOptions,
        )
    }
}

private fun Category.toSortOptions(): List<CategoryDetailSortOptionUiModel> =
    defaultSortOptions() +
        attributes
            .filterNot { attribute -> attribute.type == AttributeType.IMAGE }
            .map { attribute ->
                CategoryDetailSortOptionUiModel(
                    option = CategoryDetailSortOption.Attribute(attribute.id),
                    label = attribute.id,
                    contentDescription = "Sort items by ${attribute.id}",
                )
            }

private fun defaultSortOptions(): List<CategoryDetailSortOptionUiModel> =
    listOf(
        CategoryDetailSortOptionUiModel(
            option = CategoryDetailSortOption.Score,
            label = "Score",
            contentDescription = "Sort items by score",
        ),
        CategoryDetailSortOptionUiModel(
            option = CategoryDetailSortOption.Name,
            label = "Name",
            contentDescription = "Sort items by name",
        ),
    )

private fun List<RankedRatedItem>.sortedByAttribute(
    category: Category,
    attributeId: String,
): List<RankedRatedItem> {
    val attribute =
        category.attributes.firstOrNull { current ->
            current.id == attributeId && current.type != AttributeType.IMAGE
        } ?: return this
    return if (attribute.type == AttributeType.NUMBER) {
        sortedWith(
            compareBy<RankedRatedItem> { current ->
                current.item.scoreForAttribute(attribute.id) == null
            }.thenByDescending { current ->
                current.item.scoreForAttribute(attribute.id)
            }.thenBy { current ->
                current.item.id.lowercase(Locale.US)
            }.thenBy { current ->
                current.item.id
            },
        )
    } else {
        sortedWith(
            compareBy<RankedRatedItem> { current ->
                current.item.textValueForAttribute(attribute.id) == null
            }.thenBy { current ->
                current.item.textValueForAttribute(attribute.id)?.lowercase(Locale.US)
            }.thenBy { current ->
                current.item.textValueForAttribute(attribute.id)
            }.thenBy { current ->
                current.item.id.lowercase(Locale.US)
            }.thenBy { current ->
                current.item.id
            },
        )
    }
}

private fun RatedItem.scoreForAttribute(attributeId: String): Int? =
    scores
        .firstOrNull { score ->
            score.attribute.id == attributeId
        }?.score

private fun RatedItem.textValueForAttribute(attributeId: String): String? =
    values
        .firstOrNull { value ->
            value.attribute.id == attributeId
        }?.value
        ?.trim()
        ?.takeIf { valueText -> valueText.isNotEmpty() }

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
