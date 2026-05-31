package com.juzgon.feature.item

import com.juzgon.domain.AttributeType
import com.juzgon.domain.RatingSystem
import com.juzgon.domain.repository.AttributeRankSnapshotRepository
import com.juzgon.domain.repository.CategoryRepository
import com.juzgon.domain.repository.RatedItemRepository
import com.juzgon.domain.repository.ScoreProfileRepository
import com.juzgon.domain.usecase.CalculateProfileRankedItemsUseCase
import kotlinx.coroutines.flow.first
import java.util.Locale

/**
 * Extracted loader responsible for assembling the rich ItemDetailUiState.
 * This removes the LongMethod and ReturnCount complexity from ItemDetailViewModel.
 */
class ItemDetailContentLoader(
    private val ratedItemRepository: RatedItemRepository,
    private val attributeRankSnapshotRepository: AttributeRankSnapshotRepository,
    private val categoryRepository: CategoryRepository,
    private val scoreProfileRepository: ScoreProfileRepository,
    private val calculateProfileRankedItems: CalculateProfileRankedItemsUseCase,
) {
    suspend fun loadContent(
        itemId: String,
        categoryName: String,
        activeProfileId: String?,
    ): ItemDetailUiState {
        val item =
            ratedItemRepository.observeRatedItem(itemId).first()
                ?: return ItemDetailUiState(isLoading = false, errorMessage = "Item not found")

        val attributeScores = buildAttributeScores(item)
        val previousSnapshots =
            attributeRankSnapshotRepository
                .observeSnapshotsForItem(itemId)
                .first()
                .let { latestPreviousAttributeRankSnapshots(it, item.updatedAt) }

        val imageReferencesByAttributeId = buildImageReferences(item)
        val profileBreakdown = resolveProfileBreakdown(categoryName, activeProfileId, itemId)

        return ItemDetailUiState(
            itemId = item.id,
            primaryImage = imageReferencesByAttributeId.values.firstNotNullOfOrNull { it.firstOrNull() },
            overallScoreText =
                computeWeightedAverageText(
                    item.scores.map { it.attribute.weight to it.score },
                ),
            attributeScores = attributeScores,
            rankedAttributes = rankedAttributeCards(attributeScores, previousSnapshots),
            diamondChartPoints = itemAttributeDiamondChartPoints(attributeScores),
            attributeValues =
                item.values.map { valueEntry ->
                    ItemDetailAttributeValue(
                        label = valueEntry.attribute.displayName,
                        value = valueEntry.value,
                        type = valueEntry.attribute.type,
                        displayValue = formatAttributeValue(valueEntry.attribute.type, valueEntry.value),
                        imageReferences = imageReferencesByAttributeId[valueEntry.attribute.id].orEmpty(),
                    )
                },
            notes = item.notes,
            isLoading = false,
            profileBreakdown = profileBreakdown,
        )
    }

    private fun buildAttributeScores(item: com.juzgon.domain.RatedItem): List<ItemDetailAttributeScore> =
        item.scores.map { scoreEntry ->
            ItemDetailAttributeScore(
                label = scoreEntry.attribute.displayName,
                score = scoreEntry.score,
                attributeId = scoreEntry.attribute.id,
                displayInDiamond = scoreEntry.attribute.displayInDiamond,
                diamondOrder = scoreEntry.attribute.diamondOrder,
            )
        }

    private fun buildImageReferences(item: com.juzgon.domain.RatedItem): Map<String, List<ItemImageReference>> =
        item.values.associate { valueEntry ->
            valueEntry.attribute.id to
                if (valueEntry.attribute.type == AttributeType.IMAGE) {
                    decodeItemImageReferences(valueEntry.value)
                } else {
                    emptyList()
                }
        }

    @Suppress("ReturnCount")
    private suspend fun resolveProfileBreakdown(
        categoryName: String,
        activeProfileId: String?,
        itemId: String,
    ): ItemProfileBreakdown? {
        if (activeProfileId == null || categoryName.isBlank()) return null

        val profile = scoreProfileRepository.observeProfile(activeProfileId).first() ?: return null
        val category = categoryRepository.observeCategory(categoryName).first() ?: return null
        val rankableAttributes = category.attributes.filter { it.isRankable }
        if (rankableAttributes.isEmpty()) return null

        val ratingSystem = RatingSystem(rankableAttributes)
        val allRankedItems = ratedItemRepository.observeRankedItems(categoryName).first()
        val ratedItems = allRankedItems.map { it.item }
        val profileRankedItems = calculateProfileRankedItems(profile, ratingSystem, ratedItems)

        val itemEntry = profileRankedItems.firstOrNull { it.item.id == itemId } ?: return null
        val rank = profileRankedItems.indexOf(itemEntry) + 1

        return ItemProfileBreakdown(
            profileName = profile.name,
            profileScoreText = String.format(Locale.US, "%.1f", itemEntry.aggregateScore),
            profileRank = rank,
            totalItems = profileRankedItems.size,
            includedAttributeIds = profile.includedAttributeIds.toSet(),
        )
    }
}
