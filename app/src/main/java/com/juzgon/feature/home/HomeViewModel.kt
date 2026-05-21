package com.juzgon.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.juzgon.domain.repository.CategoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val SUBSCRIPTION_STOP_TIMEOUT_MILLIS = 5_000L

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel
    @Inject
    constructor(
        categoryRepository: CategoryRepository,
    ) : ViewModel() {
        private val searchQuery = MutableStateFlow("")
        private val sortOption = MutableStateFlow(HomeSortOption.Recent)
        private val retryTrigger = MutableStateFlow(0)
        private val mutableNavigationEvents = MutableSharedFlow<HomeNavigationEvent>()

        val navigationEvents: SharedFlow<HomeNavigationEvent> =
            mutableNavigationEvents.asSharedFlow()

        val state: StateFlow<HomeUiState> =
            retryTrigger
                .flatMapLatest {
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
                    }.catch { emit(HomeUiState(isLoading = false, errorMessage = "Failed to load categories")) }
                }.stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(SUBSCRIPTION_STOP_TIMEOUT_MILLIS),
                    initialValue = HomeUiState(isLoading = true),
                )

        fun onSearchQueryChanged(query: String) {
            searchQuery.value = query
        }

        fun onSortOptionSelected(option: HomeSortOption) {
            sortOption.value = option
        }

        fun onRetry() {
            retryTrigger.update { it + 1 }
        }

        fun onCreateCategoryClick() {
            viewModelScope.launch {
                mutableNavigationEvents.emit(HomeNavigationEvent.CreateCategory)
            }
        }

        fun onCategoryClick(categoryId: String) {
            viewModelScope.launch {
                mutableNavigationEvents.emit(HomeNavigationEvent.OpenCategory(categoryId))
            }
        }
    }
