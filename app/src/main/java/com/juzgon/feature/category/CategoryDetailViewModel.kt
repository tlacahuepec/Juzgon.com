package com.juzgon.feature.category

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.juzgon.domain.repository.CategoryRepository
import com.juzgon.domain.repository.RatedItemRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CategoryDetailViewModel
    @Inject
    constructor(
        private val categoryRepository: CategoryRepository,
        private val ratedItemRepository: RatedItemRepository,
    ) : ViewModel() {
        private val mutableState = MutableStateFlow(CategoryDetailUiState())
        private val mutableNavigationEvents = MutableSharedFlow<CategoryDetailNavigationEvent>()
        private var activeCategoryName: String? = null
        private var loadJob: Job? = null
        private val sortOption = MutableStateFlow<CategoryDetailSortOption>(CategoryDetailSortOption.Score)

        val state: StateFlow<CategoryDetailUiState> = mutableState
        val navigationEvents: SharedFlow<CategoryDetailNavigationEvent> = mutableNavigationEvents.asSharedFlow()

        fun loadCategory(categoryName: String) {
            if (activeCategoryName == categoryName) {
                return
            }

            activeCategoryName = categoryName
            loadJob?.cancel()
            mutableState.value = CategoryDetailReducer.loading(categoryName)
            loadJob =
                viewModelScope.launch {
                    combine(
                        categoryRepository.observeCategory(categoryName),
                        ratedItemRepository.observeRankedItems(categoryName),
                        sortOption,
                    ) { category, rankedItems, sort ->
                        CategoryDetailReducer.reduce(
                            categoryName = categoryName,
                            category = category,
                            rankedItems = rankedItems,
                            sortOption = sort,
                        )
                    }.collect { detailState ->
                        mutableState.value =
                            detailState.copy(
                                showDeleteConfirmDialog = mutableState.value.showDeleteConfirmDialog,
                                showDeleteWithItemsWarning = mutableState.value.showDeleteWithItemsWarning,
                                isDeleting = mutableState.value.isDeleting,
                            )
                    }
                }
        }

        fun onSortOptionSelected(option: CategoryDetailSortOption) {
            sortOption.value = option
        }

        fun onRetry() {
            val categoryName = activeCategoryName ?: return
            activeCategoryName = null
            loadCategory(categoryName)
        }

        fun onDeleteClick() {
            val current = mutableState.value
            if (current.isLoading || current.errorMessage != null) return
            if (current.hasItems) {
                mutableState.value = current.copy(showDeleteWithItemsWarning = true)
            } else {
                mutableState.value = current.copy(showDeleteConfirmDialog = true)
            }
        }

        fun onDeleteConfirmed() {
            val categoryName = activeCategoryName ?: return
            mutableState.value =
                mutableState.value.copy(
                    showDeleteConfirmDialog = false,
                    showDeleteWithItemsWarning = false,
                    isDeleting = true,
                )
            viewModelScope.launch {
                categoryRepository.deleteCategory(categoryName)
                mutableNavigationEvents.emit(CategoryDetailNavigationEvent.NavigateBack)
            }
        }

        fun onDeleteDialogDismissed() {
            mutableState.value =
                mutableState.value.copy(
                    showDeleteConfirmDialog = false,
                    showDeleteWithItemsWarning = false,
                )
        }

        fun onEditCategoryClick() {
            viewModelScope.launch {
                mutableNavigationEvents.emit(CategoryDetailNavigationEvent.NavigateToEditCategory)
            }
        }
    }
