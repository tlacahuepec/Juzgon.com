package com.juzgon.feature.item

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.juzgon.domain.Category
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

        fun loadCategory(
            categoryName: String,
            itemId: String? = null,
        ) {
            val current = mutableState.value
            if (!current.isLoading && current.categoryName == categoryName && current.originalItemId == itemId) {
                return
            }

            mutableState.value =
                ItemFormUiState(
                    mode = if (itemId == null) ItemFormMode.Create else ItemFormMode.Edit,
                    categoryName = categoryName,
                    originalItemId = itemId,
                )
            viewModelScope.launch {
                loadFormState(categoryName, itemId)
            }
        }

        private suspend fun loadFormState(
            categoryName: String,
            itemId: String?,
        ) {
            val category = categoryRepository.observeCategory(categoryName).first()
            when {
                category == null -> showLoadError("Category not found")
                itemId == null ->
                    mutableState.value =
                        ItemFormUiState(
                            categoryName = category.name,
                            scores = category.attributes.map { attribute -> ItemScoreInput(attribute) },
                            isLoading = false,
                        )
                else -> loadExistingItem(category, itemId)
            }
        }

        private suspend fun loadExistingItem(
            category: Category,
            itemId: String,
        ) {
            val item = ratedItemRepository.observeRatedItem(itemId).first()
            if (item == null) {
                showLoadError("Item not found")
                return
            }

            val scoresByAttributeId = item.scores.associateBy { score -> score.attribute.id }
            mutableState.value =
                ItemFormUiState(
                    mode = ItemFormMode.Edit,
                    categoryName = category.name,
                    originalItemId = item.id,
                    title = item.id,
                    notes = item.notes,
                    scores =
                        category.attributes.map { attribute ->
                            ItemScoreInput(
                                attribute = attribute,
                                scoreText = scoresByAttributeId[attribute.id]?.score?.toString().orEmpty(),
                            )
                        },
                    isLoading = false,
                )
        }

        private fun showLoadError(message: String) {
            mutableState.update {
                it.copy(
                    isLoading = false,
                    errorMessage = message,
                    scores = emptyList(),
                )
            }
        }

        fun onTitleChanged(title: String) {
            if (!mutableState.value.titleEditable) {
                return
            }
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

        fun onDeleteClick() {
            if (mutableState.value.mode != ItemFormMode.Edit) return
            mutableState.update { it.copy(showDeleteDialog = true) }
        }

        fun onDeleteCancel() {
            mutableState.update { it.copy(showDeleteDialog = false) }
        }

        fun onDeleteConfirm() {
            val itemId = mutableState.value.originalItemId ?: return
            viewModelScope.launch {
                ratedItemRepository.deleteRatedItem(itemId)
                mutableState.update { it.copy(showDeleteDialog = false, deleteCompleted = true) }
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
                    if (current.mode == ItemFormMode.Create) {
                        require(ratedItemRepository.observeRatedItem(current.title.trim()).first() == null) {
                            "Item already exists"
                        }
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
