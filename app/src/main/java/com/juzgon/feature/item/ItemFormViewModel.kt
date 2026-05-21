package com.juzgon.feature.item

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.juzgon.domain.repository.CategoryRepository
import com.juzgon.domain.repository.RatedItemRepository
import com.juzgon.domain.usecase.ValidateRatingsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ItemFormViewModel
    @Inject
    constructor(
        private val categoryRepository: CategoryRepository,
        private val ratedItemRepository: RatedItemRepository,
        private val validateRatingsUseCase: ValidateRatingsUseCase,
    ) : ViewModel() {
        private val mutableState = MutableStateFlow(ItemFormUiState())

        val state: StateFlow<ItemFormUiState> = mutableState

        fun loadCategory(categoryName: String) {
            val current = mutableState.value
            if (!current.isLoading && current.categoryName == categoryName) {
                return
            }

            mutableState.value = ItemFormUiState(categoryName = categoryName)
            viewModelScope.launch {
                val category = categoryRepository.observeCategory(categoryName).first()
                if (category == null) {
                    mutableState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Category not found",
                            scores = emptyList(),
                        )
                    }
                } else {
                    mutableState.value =
                        ItemFormUiState(
                            categoryName = category.name,
                            scores = category.attributes.map { attribute -> ItemScoreInput(attribute) },
                            isLoading = false,
                        )
                }
            }
        }

        fun onTitleChanged(title: String) {
            mutableState.update { it.copy(title = title, saveCompleted = false, errorMessage = null) }
        }

        fun onNotesChanged(notes: String) {
            mutableState.update { it.copy(notes = notes, saveCompleted = false, errorMessage = null) }
        }

        fun onScoreChanged(
            attributeId: String,
            scoreText: String,
        ) {
            mutableState.update {
                it.copy(
                    scores =
                        it.scores.map { score ->
                            if (score.attribute.id == attributeId) {
                                score.copy(scoreText = scoreText)
                            } else {
                                score
                            }
                        },
                    saveCompleted = false,
                    errorMessage = null,
                )
            }
        }

        fun onSaveClick() {
            val current = mutableState.value.copy(showValidationErrors = true)
            mutableState.value = current

            if (!current.saveEnabled) {
                return
            }

            viewModelScope.launch {
                mutableState.update { it.copy(isSaving = true, errorMessage = null) }
                runCatching {
                    validateRatingsUseCase(
                        scoresByAttributeId =
                            current.scores.associate { scoreInput ->
                                scoreInput.attribute.id to checkNotNull(scoreInput.scoreText.toIntOrNull())
                            },
                        requiredAttributeBounds =
                            current.scores.map { scoreInput ->
                                ValidateRatingsUseCase.AttributeBounds(scoreInput.attribute.id)
                            },
                    )
                    require(ratedItemRepository.observeRatedItem(current.title.trim()).first() == null) {
                        "Item already exists"
                    }
                    ratedItemRepository.saveRatedItem(current.toRatedItem())
                }.onSuccess {
                    mutableState.update {
                        it.copy(
                            isSaving = false,
                            saveCompleted = true,
                        )
                    }
                }.onFailure { error ->
                    mutableState.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = error.message ?: "Unable to save item",
                            saveCompleted = false,
                        )
                    }
                }
            }
        }
    }
