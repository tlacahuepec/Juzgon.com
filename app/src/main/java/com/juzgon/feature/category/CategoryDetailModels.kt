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

    @Suppress("LongParameterList", "LongMethod")
    fun reduce(
        categoryName: String,
        category: Category?,
        rankedItems: List<RankedRatedItem>,
        sortOption: CategoryDetailSortOption,
        profiles: List<ScoreProfile> = emptyList(),
        activeProfileId: String? = null,
        calculateProfileRankedItems: CalculateProfileRankedItemsUseCase? = null,
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
            resolveRankedItems(
                category = category,
                rankedItems = rankedItems,
                activeProfileId = activeProfileId,
                profiles = profiles,
                calculateProfileRankedItems = calculateProfileRankedItems,
            )

        val sortOptions = category.toSortOptions()
        val selectedSortOption =
            sortOption.takeIf { option ->
                sortOptions.any { it.option == option }
            } ?: CategoryDetailSortOption.Score
        val sortedItems =
            when (selectedSortOption) {
                CategoryDetailSortOption.Score ->
                    effectiveRankedItems.sortedWith(
                        compareByDescending<RankedRatedItem> { it.aggregateScore }
                            .thenBy { it.item.id },
                    )
                CategoryDetailSortOption.Name ->
                    effectiveRankedItems.sortedBy { it.item.id }
                is CategoryDetailSortOption.Attribute ->
                    effectiveRankedItems.sortedByAttribute(
                        category = category,
                        attributeId = selectedSortOption.attributeId,
                    )
            }

        val activeProfile =
            activeProfileId?.let { id ->
                profiles.firstOrNull { it.id == id }
            }

        return CategoryDetailUiState(
            categoryName = category.name,
            attributeSummary = category.attributes.size.toAttributeSummary(),
            items =
                sortedItems.mapIndexed { index, rankedItem ->
                    val cardMetric =
                        rankedItem.toCardMetric(
                            category = category,
                            selectedSortOption = selectedSortOption,
                        )
                    CategoryDetailItemUiModel(
                        rank = index + 1,
                        id = rankedItem.item.id,
                        averageScoreText = rankedItem.aggregateScore.toAverageScoreText(),
                        imageValue = rankedItem.item.primaryImageValue(category),
                        nationalityBadge = rankedItem.item.resolveNationalityBadge(category),
                        metricLabel = cardMetric.label,
                        metricValueText = cardMetric.valueText,
                    )
                },
            isLoading = false,
            sortOption = selectedSortOption,
            sortOptions = sortOptions,
            profiles = buildProfileOptions(profiles),
            activeProfileId = activeProfile?.id,
            activeProfileLabel = activeProfile?.let { "Ranking: ${it.name}" },
        )
    }

    private fun buildProfileOptions(profiles: List<ScoreProfile>): List<ProfileOption> =
        listOf(ProfileOption(id = null, name = "All Attributes")) +
            profiles.map { ProfileOption(id = it.id, name = it.name) }

    @Suppress("ReturnCount")
    private fun resolveRankedItems(
        category: Category,
        rankedItems: List<RankedRatedItem>,
        activeProfileId: String?,
        profiles: List<ScoreProfile>,
        calculateProfileRankedItems: CalculateProfileRankedItemsUseCase?,
    ): List<RankedRatedItem> {
        if (activeProfileId == null || calculateProfileRankedItems == null) return rankedItems
        val profile = profiles.firstOrNull { it.id == activeProfileId } ?: return rankedItems
        val ratingSystem =
            RatingSystem(
                category.attributes.filter { it.isRankable },
            )
        val ratedItems = rankedItems.map { it.item }
        return calculateProfileRankedItems(profile, ratingSystem, ratedItems)
    }
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
                attribute.type == AttributeType.IMAGE || attribute.type == AttributeType.NATIONALITY
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

@Suppress("ReturnCount")
private fun RatedItem.primaryImageValue(category: Category): String? {
    val imageAttribute =
        category.attributes.firstOrNull { attribute ->
            attribute.type == AttributeType.IMAGE
        } ?: return null
    val rawValue =
        values
            .firstOrNull { value -> value.attribute.id == imageAttribute.id }
            ?.value
            ?.takeIf { value -> value.isNotBlank() }
            ?: return null
    return decodeItemImageReferences(rawValue).firstOrNull()?.sourceUri
}

private fun Int.toAttributeSummary(): String =
    if (this == 1) {
        "1 attribute"
    } else {
        "$this attributes"
    }

private fun Double.toAverageScoreText(): String = String.format(Locale.US, "%.1f", this)

@Suppress("ReturnCount")
private fun RatedItem.resolveNationalityBadge(category: Category): String? {
    val nationalityAttribute =
        category.attributes.firstOrNull { attribute ->
            attribute.type == AttributeType.NATIONALITY
        } ?: return null
    val code =
        values
            .firstOrNull { value -> value.attribute.id == nationalityAttribute.id }
            ?.value
            ?.trim()
            ?.takeIf { value -> value.isNotEmpty() }
            ?: return null
    val option = NationalityDataset.findByCode(code) ?: return null
    return "${option.flagEmoji} ${option.nationality}"
}
