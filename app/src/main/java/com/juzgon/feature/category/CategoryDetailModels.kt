package com.juzgon.feature.category

import com.juzgon.domain.AttributeType
import com.juzgon.domain.Category
import com.juzgon.domain.NationalityDataset
import com.juzgon.domain.RankedRatedItem
import com.juzgon.domain.RatedItem
import com.juzgon.domain.RatingSystem
import com.juzgon.domain.ScoreProfile
import com.juzgon.domain.usecase.CalculateProfileRankedItemsUseCase
import com.juzgon.feature.item.decodeItemImageReferences
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
    val nationalityBadge: String? = null,
    val metricLabel: String = "Score",
    val metricValueText: String = averageScoreText,
)

data class ProfileOption(
    val id: String?,
    val name: String,
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
    val profiles: List<ProfileOption> = emptyList(),
    val activeProfileId: String? = null,
    val activeProfileLabel: String? = null,
    val searchQuery: String = "",
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

    @Suppress("LongParameterList")
    fun reduce(
        categoryName: String,
        category: Category?,
        rankedItems: List<RankedRatedItem>,
        sortOption: CategoryDetailSortOption,
        profiles: List<ScoreProfile> = emptyList(),
        activeProfileId: String? = null,
        calculateProfileRankedItems: CalculateProfileRankedItemsUseCase? = null,
        searchQuery: String = "",
    ): CategoryDetailUiState {
        if (category == null) {
            return CategoryDetailUiState(
                categoryName = categoryName,
                sortOptions = defaultSortOptions(),
                isLoading = false,
                errorMessage = "Category not found",
            )
        }

        val effectiveRankedItems =
            applyProfileRankingIfNeeded(
                category = category,
                rankedItems = rankedItems,
                activeProfileId = activeProfileId,
                profiles = profiles,
                calculateProfileRankedItems = calculateProfileRankedItems,
            )

        val sortOptions = category.toSortOptions()
        val selectedSortOption = resolveSelectedSortOption(sortOption, sortOptions)

        val sortedItems = sortItems(effectiveRankedItems, selectedSortOption, category)
        val filteredItems = applySearchFilter(sortedItems, searchQuery)

        val activeProfile = activeProfileId?.let { id -> profiles.firstOrNull { it.id == id } }

        return CategoryDetailUiState(
            categoryName = category.name,
            attributeSummary = category.attributes.size.toAttributeSummary(),
            items =
                filteredItems.mapIndexed { index, rankedItem ->
                    buildItemUiModel(rankedItem, index, category, selectedSortOption)
                },
            isLoading = false,
            sortOption = selectedSortOption,
            sortOptions = sortOptions,
            profiles = buildProfileOptions(profiles),
            activeProfileId = activeProfile?.id,
            activeProfileLabel = activeProfile?.let { "Ranking: ${it.name}" },
            searchQuery = searchQuery,
        )
    }

    private fun applyProfileRankingIfNeeded(
        category: Category,
        rankedItems: List<RankedRatedItem>,
        activeProfileId: String?,
        profiles: List<ScoreProfile>,
        calculateProfileRankedItems: CalculateProfileRankedItemsUseCase?,
    ): List<RankedRatedItem> =
        if (activeProfileId == null || calculateProfileRankedItems == null) {
            rankedItems
        } else {
            profiles
                .firstOrNull { it.id == activeProfileId }
                ?.let { profile ->
                    val ratingSystem = RatingSystem(category.attributes.filter { it.isRankable })
                    val ratedItems = rankedItems.map { it.item }
                    calculateProfileRankedItems(profile, ratingSystem, ratedItems)
                } ?: rankedItems
        }

    private fun resolveSelectedSortOption(
        requested: CategoryDetailSortOption,
        available: List<CategoryDetailSortOptionUiModel>,
    ): CategoryDetailSortOption =
        requested.takeIf { option -> available.any { it.option == option } }
            ?: CategoryDetailSortOption.Score

    private fun sortItems(
        items: List<RankedRatedItem>,
        sortOption: CategoryDetailSortOption,
        category: Category,
    ): List<RankedRatedItem> =
        when (sortOption) {
            CategoryDetailSortOption.Score ->
                items.sortedWith(
                    compareByDescending<RankedRatedItem> { it.aggregateScore }.thenBy { it.item.id },
                )
            CategoryDetailSortOption.Name -> items.sortedBy { it.item.id }
            is CategoryDetailSortOption.Attribute -> items.sortedByAttribute(category, sortOption.attributeId)
        }

    private val TEXT_SEARCHABLE_TYPES =
        setOf(
            AttributeType.NATIONALITY,
            AttributeType.NOTES,
            AttributeType.DROPDOWN,
            AttributeType.URL,
            AttributeType.BOOLEAN,
        )

    private fun applySearchFilter(
        items: List<RankedRatedItem>,
        query: String,
    ): List<RankedRatedItem> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return items
        return items.filter { ranked ->
            ranked.item.id.contains(trimmed, ignoreCase = true) ||
                ranked.item.notes.contains(trimmed, ignoreCase = true) ||
                ranked.item.values.any { value ->
                    value.attribute.type in TEXT_SEARCHABLE_TYPES &&
                        value.value.contains(trimmed, ignoreCase = true)
                }
        }
    }

    private fun buildItemUiModel(
        rankedItem: RankedRatedItem,
        index: Int,
        category: Category,
        sortOption: CategoryDetailSortOption,
    ): CategoryDetailItemUiModel {
        val metric = rankedItem.toCardMetric(category, sortOption)
        return CategoryDetailItemUiModel(
            rank = index + 1,
            id = rankedItem.item.id,
            averageScoreText = rankedItem.aggregateScore.toAverageScoreText(),
            imageValue = rankedItem.item.primaryImageValue(category),
            nationalityBadge = rankedItem.item.resolveNationalityBadge(category),
            metricLabel = metric.label,
            metricValueText = metric.valueText,
        )
    }

    private fun buildProfileOptions(profiles: List<ScoreProfile>): List<ProfileOption> =
        listOf(ProfileOption(id = null, name = "All Attributes")) +
            profiles.map { ProfileOption(id = it.id, name = it.name) }
}

private const val MISSING_ATTRIBUTE_VALUE_TEXT = "Not rated"

private data class CategoryDetailCardMetric(
    val label: String,
    val valueText: String,
)

private fun RankedRatedItem.toCardMetric(
    category: Category,
    selectedSortOption: CategoryDetailSortOption,
): CategoryDetailCardMetric =
    when (selectedSortOption) {
        is CategoryDetailSortOption.Attribute -> {
            val attribute =
                category.attributes.firstOrNull { current ->
                    current.id == selectedSortOption.attributeId && current.type != AttributeType.IMAGE
                } ?: return CategoryDetailCardMetric(label = "Score", valueText = aggregateScore.toAverageScoreText())

            val valueText =
                if (attribute.type == AttributeType.NUMBER) {
                    item.scoreForAttribute(attribute.id)?.toString()
                } else {
                    item.textValueForAttribute(attribute.id)
                } ?: MISSING_ATTRIBUTE_VALUE_TEXT
            CategoryDetailCardMetric(label = attribute.displayName, valueText = valueText)
        }

        else -> CategoryDetailCardMetric(label = "Score", valueText = aggregateScore.toAverageScoreText())
    }

private fun Category.toSortOptions(): List<CategoryDetailSortOptionUiModel> =
    defaultSortOptions() +
        attributes
            .filterNot { attribute ->
                attribute.type == AttributeType.IMAGE
            }.map { attribute ->
                CategoryDetailSortOptionUiModel(
                    option = CategoryDetailSortOption.Attribute(attribute.id),
                    label = attribute.displayName,
                    contentDescription = "Sort items by ${attribute.displayName}",
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

private fun RatedItem.primaryImageValue(category: Category): String? =
    category.attributes
        .firstOrNull { it.type == AttributeType.IMAGE }
        ?.let { imgAttr ->
            values
                .firstOrNull { it.attribute.id == imgAttr.id }
                ?.value
                ?.takeIf { it.isNotBlank() }
                ?.let { decodeItemImageReferences(it).firstOrNull()?.sourceUri }
        }

private fun Int.toAttributeSummary(): String =
    if (this == 1) {
        "1 attribute"
    } else {
        "$this attributes"
    }

private fun Double.toAverageScoreText(): String = String.format(Locale.US, "%.1f", this)

private fun RatedItem.resolveNationalityBadge(category: Category): String? =
    category.attributes
        .firstOrNull { it.type == AttributeType.NATIONALITY }
        ?.let { natAttr ->
            values
                .firstOrNull { it.attribute.id == natAttr.id }
                ?.value
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?.let { NationalityDataset.findByCode(it)?.flagEmoji }
        }
