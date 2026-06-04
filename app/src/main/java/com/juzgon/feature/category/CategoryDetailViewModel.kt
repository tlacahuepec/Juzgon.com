package com.juzgon.feature.category

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.juzgon.domain.repository.CategoryRepository
import com.juzgon.domain.repository.RatedItemRepository
import com.juzgon.domain.repository.ScoreProfileRepository
import com.juzgon.domain.usecase.CalculateProfileRankedItemsUseCase
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

@Suppress("TooManyFunctions")
@HiltViewModel
class CategoryDetailViewModel
    @Inject
    constructor(
        private val categoryRepository: CategoryRepository,
        private val ratedItemRepository: RatedItemRepository,
        private val scoreProfileRepository: ScoreProfileRepository,
        private val calculateProfileRankedItems: CalculateProfileRankedItemsUseCase,
    ) : ViewModel() {
        private val mutableState = MutableStateFlow(CategoryDetailUiState())
        private val mutableNavigationEvents = MutableSharedFlow<CategoryDetailNavigationEvent>()
        private var activeCategoryName: String? = null
        private var loadJob: Job? = null
        private val sortOption = MutableStateFlow<CategoryDetailSortOption>(CategoryDetailSortOption.Score)
        private val activeProfileId = MutableStateFlow<String?>(null)
        private val searchQuery = MutableStateFlow<String>("")
        private val activeFilters = MutableStateFlow<List<AttributeFilter>>(emptyList())
        private val visibleRange =
            MutableStateFlow<CategoryDetailVisibleRange>(CategoryDetailVisibleRange.Top10)

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
                        scoreProfileRepository.observeProfilesForCategory(categoryName),
                        sortOption,
                        activeProfileId,
                        searchQuery,
                        activeFilters,
                        visibleRange,
                    ) { flows ->
                        @Suppress("UNCHECKED_CAST")
                        CategoryDetailReducer.reduce(
                            categoryName = categoryName,
                            category = flows[0] as com.juzgon.domain.Category?,
                            rankedItems = flows[1] as List<com.juzgon.domain.RankedRatedItem>,
                            sortOption = flows[3] as CategoryDetailSortOption,
                            profiles = flows[2] as List<com.juzgon.domain.ScoreProfile>,
                            activeProfileId = flows[4] as String?,
                            calculateProfileRankedItems = calculateProfileRankedItems,
                            searchQuery = flows[5] as String,
                            activeFilters = flows[6] as List<AttributeFilter>,
                            visibleRange = flows[7] as CategoryDetailVisibleRange,
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

        fun onVisibleRangeSelected(range: CategoryDetailVisibleRange) {
            visibleRange.value = range
        }

        fun onProfileSelected(profileId: String?) {
            activeProfileId.value = profileId
        }

        fun onSearchQueryChanged(query: String) {
            searchQuery.value = query
        }

        fun onFilterSelected(filter: AttributeFilter) {
            val current = activeFilters.value.toMutableList()
            val index = current.indexOfFirst { it.attributeId == filter.attributeId }
            if (index >= 0) current[index] = filter else current.add(filter)
            activeFilters.value = current
        }

        fun onFilterCleared(attributeId: String) {
            activeFilters.value = activeFilters.value.filter { it.attributeId != attributeId }
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
