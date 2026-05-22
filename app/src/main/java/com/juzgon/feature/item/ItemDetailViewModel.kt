package com.juzgon.feature.item

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.juzgon.domain.AttributeType
import com.juzgon.domain.repository.AttributeRankSnapshotRepository
import com.juzgon.domain.repository.RatedItemRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ItemDetailViewModel
    @Inject
    constructor(
        private val ratedItemRepository: RatedItemRepository,
        private val attributeRankSnapshotRepository: AttributeRankSnapshotRepository,
    ) : ViewModel() {
        private val mutableState = MutableStateFlow(ItemDetailUiState())

        val state: StateFlow<ItemDetailUiState> = mutableState

        fun loadItem(itemId: String) {
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
                            label = scoreEntry.attribute.id,
                            score = scoreEntry.score,
                            attributeId = scoreEntry.attribute.id,
                        )
                    }
                val previousSnapshots =
                    latestPreviousAttributeRankSnapshots(
                        snapshots = attributeRankSnapshotRepository.observeSnapshotsForItem(itemId).first(),
                        currentUpdatedAt = item.updatedAt,
                    )
                mutableState.value =
                    ItemDetailUiState(
                        itemId = item.id,
                        primaryImageValue =
                            item.values
                                .firstOrNull { valueEntry ->
                                    valueEntry.attribute.type == AttributeType.IMAGE &&
                                        valueEntry.value.isNotBlank()
                                }?.value,
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
                        attributeValues =
                            item.values.map { valueEntry ->
                                ItemDetailAttributeValue(
                                    label = valueEntry.attribute.id,
                                    value = valueEntry.value,
                                    type = valueEntry.attribute.type,
                                    displayValue =
                                        formatAttributeValue(
                                            type = valueEntry.attribute.type,
                                            value = valueEntry.value,
                                        ),
                                )
                            },
                        notes = item.notes,
                        isLoading = false,
                    )
            }
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
