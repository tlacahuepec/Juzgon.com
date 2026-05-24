package com.juzgon.feature.item

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.juzgon.domain.AttributeType
import com.juzgon.domain.RatingSystem
import com.juzgon.domain.repository.AttributeRankSnapshotRepository
import com.juzgon.domain.repository.CategoryRepository
import com.juzgon.domain.repository.RatedItemRepository
import com.juzgon.domain.repository.ScoreProfileRepository
import com.juzgon.domain.usecase.CalculateProfileRankedItemsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class ItemDetailViewModel
    @Inject
    constructor(
        private val ratedItemRepository: RatedItemRepository,
        private val attributeRankSnapshotRepository: AttributeRankSnapshotRepository,
        private val categoryRepository: CategoryRepository,
        private val scoreProfileRepository: ScoreProfileRepository,
        private val calculateProfileRankedItems: CalculateProfileRankedItemsUseCase,
    ) : ViewModel() {
        private val mutableState = MutableStateFlow(ItemDetailUiState())

        val state: StateFlow<ItemDetailUiState> = mutableState

        @Suppress("LongMethod")
        fun loadItem(
            itemId: String,
            categoryName: String = "",
            activeProfileId: String? = null,
        ) {
            if (!mutableState.value.isLoading && mutableState.value.itemId == itemId) return
            viewModelScope.launch {
                val item = ratedItemRepository.observeRatedItem(itemId).first()
                if (item == null) {
                    mutableState.value = ItemDetailUiState(isLoading = false, errorMessage = "Item not found")
                    return@launch
                }
                val attributeScores =
                    item.scores.map { scoreEntry ->
                        ItemDetailAttributeScore(
                            label = scoreEntry.attribute.displayName,
                            score = scoreEntry.score,
                            attributeId = scoreEntry.attribute.id,
                            displayInDiamond = scoreEntry.attribute.displayInDiamond,
                            diamondOrder = scoreEntry.attribute.diamondOrder,
                        )
                    }
                val previousSnapshots =
                    latestPreviousAttributeRankSnapshots(
                        snapshots = attributeRankSnapshotRepository.observeSnapshotsForItem(itemId).first(),
                        currentUpdatedAt = item.updatedAt,
                    )
                val imageReferencesByAttributeId =
                    item.values
                        .associate { valueEntry ->
                            valueEntry.attribute.id to
                                if (valueEntry.attribute.type == AttributeType.IMAGE) {
                                    decodeItemImageReferences(valueEntry.value)
                                } else {
                                    emptyList()
                                }
                        }
                val profileBreakdown = resolveProfileBreakdown(categoryName, activeProfileId, itemId)
                mutableState.value =
                    ItemDetailUiState(
                        itemId = item.id,
                        primaryImage = imageReferencesByAttributeId.values.firstNotNullOfOrNull { it.firstOrNull() },
                        overallScoreText =
                            computeWeightedAverageText(
                                item.scores.map { it.attribute.weight to it.score },
                            ),
                        attributeScores = attributeScores,
                        rankedAttributes =
                            rankedAttributeCards(
                                attributeScores = attributeScores,
                                previousSnapshots = previousSnapshots,
                            ),
                        diamondChartPoints = itemAttributeDiamondChartPoints(attributeScores),
                        attributeValues =
                            item.values.map { valueEntry ->
                                ItemDetailAttributeValue(
                                    label = valueEntry.attribute.displayName,
                                    value = valueEntry.value,
                                    type = valueEntry.attribute.type,
                                    displayValue =
                                        formatAttributeValue(
                                            type = valueEntry.attribute.type,
                                            value = valueEntry.value,
                                        ),
                                    imageReferences = imageReferencesByAttributeId[valueEntry.attribute.id].orEmpty(),
                                )
                            },
                        notes = item.notes,
                        isLoading = false,
                        profileBreakdown = profileBreakdown,
                    )
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

        fun onDeleteClick() {
            mutableState.value = mutableState.value.copy(showDeleteConfirmDialog = true)
        }

        fun onDeleteDialogDismissed() {
            mutableState.value =
                mutableState.value.copy(
                    showDeleteConfirmDialog = false,
                    isDeleting = false,
                )
        }

        fun onDeleteConfirmed() {
            val itemId = mutableState.value.itemId
            if (itemId.isBlank()) return
            mutableState.value = mutableState.value.copy(isDeleting = true)
            viewModelScope.launch {
                ratedItemRepository.deleteRatedItem(itemId)
                mutableState.value =
                    mutableState.value.copy(
                        showDeleteConfirmDialog = false,
                        isDeleting = false,
                        deleteCompleted = true,
                    )
            }
        }
    }
