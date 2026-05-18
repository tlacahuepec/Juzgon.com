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
        private var activeCategoryName: String? = null
        private var loadJob: Job? = null

        val state: StateFlow<CategoryDetailUiState> = mutableState

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
                    ) { category, rankedItems ->
                        CategoryDetailReducer.reduce(
                            categoryName = categoryName,
                            category = category,
                            rankedItems = rankedItems,
                        )
                    }.collect { detailState ->
                        mutableState.value = detailState
                    }
                }
        }
    }
