@file:Suppress("TooManyFunctions")

package com.juzgon.feature.category

import com.juzgon.domain.AttributeType
import com.juzgon.domain.Category
import com.juzgon.domain.ItemAttributeValue
import com.juzgon.domain.NationalityCodes
import com.juzgon.domain.NationalityDataset
import com.juzgon.domain.RankedRatedItem
import com.juzgon.domain.RatedItem
import com.juzgon.domain.RatingSystem
import com.juzgon.domain.ScoreProfile
import com.juzgon.domain.SkinTypeValues
import com.juzgon.domain.social.SocialNetworkCodec
import com.juzgon.domain.social.SocialPlatformIcons
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

sealed interface CategoryDetailVisibleRange {
    val limit: Int?

    data object Top10 : CategoryDetailVisibleRange {
        override val limit = LIMIT_TOP_10
    }

    data object Top20 : CategoryDetailVisibleRange {
        override val limit = LIMIT_TOP_20
    }

    data object Top50 : CategoryDetailVisibleRange {
        override val limit = LIMIT_TOP_50
    }

    data object All : CategoryDetailVisibleRange {
        override val limit: Int? = null
    }

    companion object {
        private const val LIMIT_TOP_10 = 10
        private const val LIMIT_TOP_20 = 20
        private const val LIMIT_TOP_50 = 50
    }
}

sealed interface CategoryDetailNavigationEvent {
    data object NavigateToEditCategory : CategoryDetailNavigationEvent

    data object NavigateBack : CategoryDetailNavigationEvent
}

sealed interface AttributeFilter {
    val attributeId: String

    data class Nationality(
        override val attributeId: String,
        val selectedCodes: Set<String>,
    ) : AttributeFilter

    data class NumberRange(
        override val attributeId: String,
        val min: Int,
        val max: Int,
    ) : AttributeFilter

    data class DateRange(
        override val attributeId: String,
        val startDate: String?,
        val endDate: String?,
    ) : AttributeFilter

    data class Dropdown(
        override val attributeId: String,
        val selectedValues: Set<String>,
    ) : AttributeFilter

    data class BooleanFilter(
        override val attributeId: String,
        val value: Boolean,
    ) : AttributeFilter

    data class SkinType(
        override val attributeId: String,
        val selectedValues: Set<String>,
    ) : AttributeFilter
}

data class CategoryDetailItemUiModel(
    val rank: Int,
    val id: String,
    val averageScoreText: String,
    val imageValue: String? = null,
    val nationalityBadge: String? = null,
    val socialBadgeIcons: List<Int> = emptyList(),
    val metricLabel: String = "Score",
    val metricValueText: String = averageScoreText,
    val metricColorHex: String? = null,
)

data class ProfileOption(
    val id: String?,
    val name: String,
)

data class FilterChipUiModel(
    val attributeId: String,
    val label: String,
    val type: AttributeType,
    val isActive: Boolean,
    val activeLabel: String? = null,
    val availableValues: List<String> = emptyList(),
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
    val filterChips: List<FilterChipUiModel> = emptyList(),
    val activeFilters: List<AttributeFilter> = emptyList(),
    val visibleRange: CategoryDetailVisibleRange = CategoryDetailVisibleRange.Top10,
    val visibleRangeOptions: List<CategoryDetailVisibleRange> = emptyList(),
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
        activeFilters: List<AttributeFilter> = emptyList(),
        visibleRange: CategoryDetailVisibleRange = CategoryDetailVisibleRange.All,
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

        val rankedWithIndex = sortedItems.mapIndexed { index, item -> index + 1 to item }

        val effectiveVisibleRange = resolveVisibleRange(visibleRange, sortedItems.size)
        val visibleItems = applyVisibleRange(rankedWithIndex, effectiveVisibleRange)

        val searchedItems = applySearchFilterIndexed(visibleItems, searchQuery)
        val filteredItems = applyAttributeFiltersIndexed(searchedItems, activeFilters)

        val activeProfile = activeProfileId?.let { id -> profiles.firstOrNull { it.id == id } }

        return CategoryDetailUiState(
            categoryName = category.name,
            attributeSummary = category.attributes.size.toAttributeSummary(),
            items =
                filteredItems.map { (rank, rankedItem) ->
                    buildItemUiModel(rankedItem, rank - 1, category, selectedSortOption)
                },
            isLoading = false,
            sortOption = selectedSortOption,
            sortOptions = sortOptions,
            profiles = buildProfileOptions(profiles),
            activeProfileId = activeProfile?.id,
            activeProfileLabel = activeProfile?.let { "Ranking: ${it.name}" },
            searchQuery = searchQuery,
            filterChips = buildFilterChips(category, effectiveRankedItems, activeFilters),
            activeFilters = activeFilters,
            visibleRange = effectiveVisibleRange,
            visibleRangeOptions = buildVisibleRangeOptions(sortedItems.size),
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

    private fun resolveVisibleRange(
        requested: CategoryDetailVisibleRange,
        totalItems: Int,
    ): CategoryDetailVisibleRange =
        if (totalItems <= VISIBLE_RANGE_THRESHOLD) {
            CategoryDetailVisibleRange.All
        } else {
            requested
        }

    private fun applyVisibleRange(
        items: List<Pair<Int, RankedRatedItem>>,
        visibleRange: CategoryDetailVisibleRange,
    ): List<Pair<Int, RankedRatedItem>> = visibleRange.limit?.let { items.take(it) } ?: items

    private fun buildVisibleRangeOptions(totalItems: Int): List<CategoryDetailVisibleRange> =
        if (totalItems <= VISIBLE_RANGE_THRESHOLD) {
            emptyList()
        } else {
            buildList {
                add(CategoryDetailVisibleRange.Top10)
                if (totalItems > VISIBLE_RANGE_THRESHOLD) add(CategoryDetailVisibleRange.Top20)
                if (totalItems > TOP_20_THRESHOLD) add(CategoryDetailVisibleRange.Top50)
                add(CategoryDetailVisibleRange.All)
            }
        }

    private fun applySearchFilterIndexed(
        items: List<Pair<Int, RankedRatedItem>>,
        query: String,
    ): List<Pair<Int, RankedRatedItem>> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return items
        return items.filter { (_, ranked) ->
            ranked.item.id.contains(trimmed, ignoreCase = true) ||
                ranked.item.notes.contains(trimmed, ignoreCase = true) ||
                ranked.item.values.any { value ->
                    value.attribute.type in TEXT_SEARCHABLE_TYPES &&
                        value.matchesSearchQuery(trimmed)
                }
        }
    }

    private fun applyAttributeFiltersIndexed(
        items: List<Pair<Int, RankedRatedItem>>,
        filters: List<AttributeFilter>,
    ): List<Pair<Int, RankedRatedItem>> {
        if (filters.isEmpty()) return items
        return items.filter { (_, ranked) -> filters.all { filter -> matchesFilter(ranked, filter) } }
    }

    private const val VISIBLE_RANGE_THRESHOLD = 10
    private const val TOP_20_THRESHOLD = 20

    private val TEXT_SEARCHABLE_TYPES =
        setOf(
            AttributeType.NATIONALITY,
            AttributeType.NOTES,
            AttributeType.DROPDOWN,
            AttributeType.URL,
            AttributeType.BOOLEAN,
            AttributeType.SOCIAL_NETWORK,
            AttributeType.SKIN_TYPE,
        )

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
            socialBadgeIcons = rankedItem.item.resolveSocialBadgeIcons(category),
            metricLabel = metric.label,
            metricValueText = metric.valueText,
            metricColorHex = metric.colorHex,
        )
    }

    private fun buildProfileOptions(profiles: List<ScoreProfile>): List<ProfileOption> =
        listOf(ProfileOption(id = null, name = "All Attributes")) +
            profiles.map { ProfileOption(id = it.id, name = it.name) }

    @Suppress("CyclomaticComplexMethod")
    private fun matchesFilter(
        ranked: RankedRatedItem,
        filter: AttributeFilter,
    ): Boolean =
        when (filter) {
            is AttributeFilter.Nationality ->
                ranked.item
                    .textValueForAttribute(filter.attributeId)
                    ?.let { NationalityCodes.parse(it).any { code -> code in filter.selectedCodes } }
                    ?: false
            is AttributeFilter.NumberRange ->
                ranked.item
                    .scoreForAttribute(filter.attributeId)
                    ?.let { it in filter.min..filter.max } ?: false
            is AttributeFilter.DateRange ->
                ranked.item
                    .textValueForAttribute(filter.attributeId)
                    ?.let { matchesDateRange(it, filter.startDate, filter.endDate) } ?: false
            is AttributeFilter.Dropdown ->
                ranked.item
                    .textValueForAttribute(filter.attributeId)
                    ?.let { it in filter.selectedValues } ?: false
            is AttributeFilter.BooleanFilter ->
                ranked.item
                    .textValueForAttribute(filter.attributeId)
                    ?.let { it.equals(filter.value.toString(), ignoreCase = true) } ?: false
            is AttributeFilter.SkinType ->
                ranked.item
                    .textValueForAttribute(filter.attributeId)
                    ?.let { value ->
                        SkinTypeValues.fromStoredValue(value)?.storedValue in filter.selectedValues
                    } ?: false
        }

    @Suppress("ReturnCount")
    private fun matchesDateRange(
        value: String,
        startDate: String?,
        endDate: String?,
    ): Boolean {
        if (startDate != null && value < startDate) return false
        if (endDate != null && value > endDate) return false
        return true
    }

    private val FILTERABLE_TYPES =
        setOf(
            AttributeType.NUMBER,
            AttributeType.NATIONALITY,
            AttributeType.DROPDOWN,
            AttributeType.DATE,
            AttributeType.BOOLEAN,
            AttributeType.SKIN_TYPE,
        )

    private fun buildFilterChips(
        category: Category,
        items: List<RankedRatedItem>,
        activeFilters: List<AttributeFilter>,
    ): List<FilterChipUiModel> =
        category.attributes
            .filter { it.type in FILTERABLE_TYPES }
            .map { attribute ->
                val activeFilter = activeFilters.firstOrNull { it.attributeId == attribute.id }
                FilterChipUiModel(
                    attributeId = attribute.id,
                    label = attribute.displayName,
                    type = attribute.type,
                    isActive = activeFilter != null,
                    activeLabel = activeFilter?.toActiveLabel(),
                    availableValues = collectAvailableValues(attribute, items),
                )
            }

    private fun AttributeFilter.toActiveLabel(): String =
        when (this) {
            is AttributeFilter.Nationality -> selectedCodes.joinToString(", ")
            is AttributeFilter.NumberRange -> "$min–$max"
            is AttributeFilter.DateRange -> listOfNotNull(startDate, endDate).joinToString(" – ")
            is AttributeFilter.Dropdown -> selectedValues.joinToString(", ")
            is AttributeFilter.BooleanFilter -> if (value) "Yes" else "No"
            is AttributeFilter.SkinType ->
                selectedValues
                    .map { value -> SkinTypeValues.displayLabelOrUnknown(value) }
                    .joinToString(", ")
        }

    private fun collectAvailableValues(
        attribute: com.juzgon.domain.Attribute,
        items: List<RankedRatedItem>,
    ): List<String> =
        if (attribute.type == AttributeType.NUMBER) {
            emptyList()
        } else if (attribute.type == AttributeType.SKIN_TYPE) {
            items
                .mapNotNull { ranked ->
                    ranked.item.values
                        .firstOrNull { it.attribute.id == attribute.id }
                        ?.value
                        ?.let { SkinTypeValues.fromStoredValue(it)?.storedValue }
                }.distinct()
                .sortedBy { SkinTypeValues.sortOrderOrNull(it) ?: Int.MAX_VALUE }
        } else if (attribute.type == AttributeType.NATIONALITY) {
            items
                .flatMap { ranked ->
                    ranked.item.values
                        .firstOrNull { it.attribute.id == attribute.id }
                        ?.value
                        ?.let { NationalityCodes.parse(it) }
                        ?: emptyList()
                }.distinct()
                .sorted()
        } else {
            items
                .mapNotNull { ranked ->
                    ranked.item.values
                        .firstOrNull { it.attribute.id == attribute.id }
                        ?.value
                        ?.trim()
                        ?.takeIf { it.isNotEmpty() }
                }.distinct()
                .sorted()
        }
}

private const val MISSING_ATTRIBUTE_VALUE_TEXT = "Not rated"
private const val MAX_BADGE_FLAGS = 3
private const val MAX_SOCIAL_BADGE_ICONS = 4

private data class CategoryDetailCardMetric(
    val label: String,
    val valueText: String,
    val colorHex: String? = null,
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

            val textValue = item.textValueForAttribute(attribute.id)
            val valueText =
                when (attribute.type) {
                    AttributeType.NUMBER -> item.scoreForAttribute(attribute.id)?.toString()
                    AttributeType.SKIN_TYPE -> textValue?.let { SkinTypeValues.displayLabelOrUnknown(it) }
                    else -> textValue
                } ?: MISSING_ATTRIBUTE_VALUE_TEXT
            CategoryDetailCardMetric(
                label = attribute.displayName,
                valueText = valueText,
                colorHex = textValue?.let { SkinTypeValues.fromStoredValue(it)?.colorHex },
            )
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
    return when (attribute.type) {
        AttributeType.NUMBER ->
            sortedByNullableKey(
                keySelector = { it.item.scoreForAttribute(attribute.id) },
                descending = true,
            )
        AttributeType.SKIN_TYPE ->
            sortedByNullableKey(
                keySelector = {
                    it.item
                        .textValueForAttribute(attribute.id)
                        ?.let { v -> SkinTypeValues.sortOrderOrNull(v) }
                },
            )
        AttributeType.NATIONALITY ->
            sortedByNullableKey(
                keySelector = {
                    it.item
                        .textValueForAttribute(attribute.id)
                        ?.let { NationalityCodes.primary(it) }
                        ?.let { NationalityDataset.findByCode(it)?.nationality?.lowercase(Locale.US) }
                },
            )
        else ->
            sortedWith(
                compareBy<RankedRatedItem> { it.item.textValueForAttribute(attribute.id) == null }
                    .thenBy { it.item.textValueForAttribute(attribute.id)?.lowercase(Locale.US) }
                    .thenBy { it.item.textValueForAttribute(attribute.id) }
                    .thenBy { it.item.id.lowercase(Locale.US) }
                    .thenBy { it.item.id },
            )
    }
}

private fun <T : Comparable<T>> List<RankedRatedItem>.sortedByNullableKey(
    keySelector: (RankedRatedItem) -> T?,
    descending: Boolean = false,
): List<RankedRatedItem> {
    val comparator =
        compareBy<RankedRatedItem> { keySelector(it) == null }
            .let { if (descending) it.thenByDescending(keySelector) else it.thenBy(keySelector) }
            .thenBy { it.item.id.lowercase(Locale.US) }
            .thenBy { it.item.id }
    return sortedWith(comparator)
}

private fun ItemAttributeValue.matchesSearchQuery(query: String): Boolean =
    if (attribute.type == AttributeType.SKIN_TYPE) {
        SkinTypeValues.matchesQuery(value, query)
    } else {
        value.contains(query, ignoreCase = true)
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
                ?.let { raw ->
                    val codes = NationalityCodes.parse(raw)
                    val flags = codes.mapNotNull { NationalityDataset.findByCode(it)?.flagEmoji }
                    if (flags.isEmpty()) return@let null
                    val visible = flags.take(MAX_BADGE_FLAGS).joinToString("")
                    val overflow = flags.size - MAX_BADGE_FLAGS
                    if (overflow > 0) "$visible+$overflow" else visible
                }
        }

private fun RatedItem.resolveSocialBadgeIcons(category: Category): List<Int> =
    category.attributes
        .firstOrNull { it.type == AttributeType.SOCIAL_NETWORK }
        ?.let { socialAttr ->
            values
                .firstOrNull { it.attribute.id == socialAttr.id }
                ?.value
                ?.let { raw ->
                    SocialNetworkCodec
                        .parse(raw)
                        .take(MAX_SOCIAL_BADGE_ICONS)
                        .map { SocialPlatformIcons.iconRes(it.platform) }
                }
        } ?: emptyList()
