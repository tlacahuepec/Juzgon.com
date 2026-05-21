package com.juzgon.feature.category

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.juzgon.domain.repository.CategoryRepository
import com.juzgon.domain.repository.RatedItemRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
        private var activeCategoryId: String? = null
        private var loadJob: Job? = null
        private val sortOption = MutableStateFlow(CategoryDetailSortOption.Score)

        val state: StateFlow<CategoryDetailUiState> = mutableState

        fun loadCategory(categoryId: String) {
            if (activeCategoryId == categoryId) {
                return
            }

            activeCategoryId = categoryId
            loadJob?.cancel()
            mutableState.value = CategoryDetailReducer.loading(categoryId)
            loadJob =
                viewModelScope.launch {
                    combine(
                        categoryRepository.observeCategory(categoryId),
                        ratedItemRepository.observeRankedItems(categoryId),
                        sortOption,
                    ) { category, rankedItems, sort ->
                        CategoryDetailReducer.reduce(
                            categoryId = categoryId,
                            category = category,
                            rankedItems = rankedItems,
                            sortOption = sort,
                        )
                    }.collect { detailState ->
                        mutableState.value = detailState
                    }
                }
        }

        fun onSortOptionSelected(option: CategoryDetailSortOption) {
            sortOption.value = option
        }

        fun onRetry() {
            val categoryId = activeCategoryId ?: return
            activeCategoryId = null
            loadCategory(categoryId)
        }
    }
