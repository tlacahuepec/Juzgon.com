package com.juzgon.feature.category

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.juzgon.domain.CatalogType
import com.juzgon.domain.repository.CategoryRepository
import com.juzgon.domain.repository.RatedItemRepository
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
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

private const val SUBSCRIPTION_STOP_TIMEOUT_MILLIS = 5_000L

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class CatalogsViewModel
    @Inject
    constructor(
        private val categoryRepository: CategoryRepository,
        private val ratedItemRepository: RatedItemRepository? = null,
    ) : ViewModel() {
        constructor(categoryRepository: CategoryRepository) : this(categoryRepository, null)

        private val searchQuery = MutableStateFlow("")
        private val selectedType = MutableStateFlow<CatalogType?>(null)
        private val retryTrigger = MutableStateFlow(0)
        private val mutableNavigationEvents = MutableSharedFlow<CatalogsNavigationEvent>()

        val navigationEvents: SharedFlow<CatalogsNavigationEvent> =
            mutableNavigationEvents.asSharedFlow()

        private val categoriesWithRatingsFlow =
            retryTrigger.flatMapLatest {
                categoryRepository
                    .observeCategories()
                    .flatMapLatest { categories ->
                        if (categories.isEmpty()) {
                            flowOf(emptyList())
                        } else if (ratedItemRepository == null) {
                            flowOf(categories.map { it to (null as Double?) })
                        } else {
                            val itemFlows =
                                categories.map { category ->
                                    ratedItemRepository
                                        .observeRankedItems(category.name)
                                        .map { items ->
                                            val avg =
                                                if (items.isEmpty()) {
                                                    null
                                                } else {
                                                    items.map { it.aggregateScore }.average()
                                                }
                                            category to avg
                                        }.catch { throwable ->
                                            Timber.w(throwable, "Failed to load ranked items for ${category.name}")
                                            emit(category to null)
                                        }
                                }
                            combine(itemFlows) { it.toList() }
                        }
                    }
            }

        val state: StateFlow<CatalogsUiState> =
            retryTrigger
                .flatMapLatest {
                    combine(
                        categoriesWithRatingsFlow,
                        searchQuery,
                        selectedType,
                    ) { categoriesWithRatings, query, type ->
                        CatalogsStateReducer.reduce(
                            categoriesWithRatings = categoriesWithRatings,
                            searchQuery = query,
                            selectedType = type,
                        )
                    }.catch { throwable ->
                        Timber.e(throwable, "Failed to load categories")
                        emit(CatalogsUiState(isLoading = false, errorMessage = "Failed to load categories"))
                    }
                }.stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(SUBSCRIPTION_STOP_TIMEOUT_MILLIS),
                    initialValue = CatalogsUiState(isLoading = true),
                )

        fun onSearchQueryChanged(query: String) {
            searchQuery.value = query
        }

        fun onTypeFilterSelected(type: CatalogType?) {
            selectedType.value = if (selectedType.value == type) null else type
        }

        fun onRetry() {
            retryTrigger.update { it + 1 }
        }

        fun onCreateCategoryClick() {
            viewModelScope.launch {
                mutableNavigationEvents.emit(CatalogsNavigationEvent.CreateCategory)
            }
        }

        fun onCategoryClick(categoryName: String) {
            viewModelScope.launch {
                mutableNavigationEvents.emit(CatalogsNavigationEvent.OpenCategory(categoryName))
            }
        }
    }
