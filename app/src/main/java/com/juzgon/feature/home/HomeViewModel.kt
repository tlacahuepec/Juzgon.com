package com.juzgon.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.juzgon.domain.repository.CategoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

private const val SUBSCRIPTION_STOP_TIMEOUT_MILLIS = 5_000L

@HiltViewModel
class HomeViewModel
    @Inject
    constructor(
        categoryRepository: CategoryRepository,
    ) : ViewModel() {
        private val searchQuery = MutableStateFlow("")
        private val sortOption = MutableStateFlow(HomeSortOption.Recent)

        val state: StateFlow<HomeUiState> =
            combine(
                categoryRepository.observeCategories(),
                searchQuery,
                sortOption,
            ) { categories, query, sort ->
                HomeStateReducer.reduce(
                    categories = categories,
                    searchQuery = query,
                    sortOption = sort,
                )
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(SUBSCRIPTION_STOP_TIMEOUT_MILLIS),
                initialValue = HomeUiState(),
            )

        fun onSearchQueryChanged(query: String) {
            searchQuery.value = query
        }

        fun onSortOptionSelected(option: HomeSortOption) {
            sortOption.value = option
        }
    }
