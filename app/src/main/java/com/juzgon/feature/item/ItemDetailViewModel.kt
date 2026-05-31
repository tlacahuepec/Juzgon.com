package com.juzgon.feature.item

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.juzgon.domain.repository.AttributeRankSnapshotRepository
import com.juzgon.domain.repository.CategoryRepository
import com.juzgon.domain.repository.RatedItemRepository
import com.juzgon.domain.repository.ScoreProfileRepository
import com.juzgon.domain.usecase.CalculateProfileRankedItemsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
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

        private val contentLoader =
            ItemDetailContentLoader(
                ratedItemRepository,
                attributeRankSnapshotRepository,
                categoryRepository,
                scoreProfileRepository,
                calculateProfileRankedItems,
            )

        val state: StateFlow<ItemDetailUiState> = mutableState

        fun loadItem(
            itemId: String,
            categoryName: String = "",
            activeProfileId: String? = null,
        ) {
            if (!mutableState.value.isLoading && mutableState.value.itemId == itemId) return

            viewModelScope.launch {
                val content = contentLoader.loadContent(itemId, categoryName, activeProfileId)
                mutableState.value = content
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
