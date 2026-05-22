package com.juzgon.feature.item

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
                        )
                    }
                mutableState.value =
                    ItemDetailUiState(
                        itemId = item.id,
                        overallScoreText =
                            computeWeightedAverageText(
                                item.scores.map { it.attribute.weight to it.score },
                            ),
                        attributeScores = attributeScores,
                        rankedAttributes = rankedAttributeCards(attributeScores),
                        attributeValues =
                            item.values.map { valueEntry ->
                                ItemDetailAttributeValue(
                                    label = valueEntry.attribute.id,
                                    value = valueEntry.value,
                                )
                            },
                        notes = item.notes,
                        isLoading = false,
                    )
            }
        }
    }
