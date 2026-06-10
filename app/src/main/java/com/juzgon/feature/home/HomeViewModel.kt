package com.juzgon.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.juzgon.domain.RankedRatedItem
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
import java.util.Locale
import javax.inject.Inject

private const val SUBSCRIPTION_STOP_TIMEOUT_MILLIS = 5_000L
private const val TRENDING_ITEM_COUNT = 3
private const val SCORE_FORMAT_MAX = 10

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel
    @Inject
    constructor(
        categoryRepository: CategoryRepository,
        ratedItemRepository: RatedItemRepository,
    ) : ViewModel() {
        private val searchQuery = MutableStateFlow("")
        private val sortOption = MutableStateFlow(HomeSortOption.Recent)
        private val retryTrigger = MutableStateFlow(0)
        private val mutableNavigationEvents = MutableSharedFlow<HomeNavigationEvent>()

        val navigationEvents: SharedFlow<HomeNavigationEvent> =
            mutableNavigationEvents.asSharedFlow()

        private val topItemsFlow =
            categoryRepository
                .observeCategories()
                .flatMapLatest { categories ->
                    if (categories.isEmpty()) {
                        flowOf(emptyList())
                    } else {
                        val itemFlows =
                            categories.map { category ->
                                ratedItemRepository
                                    .observeRankedItems(category.name)
                                    .map { items -> items.map { item -> item to category.name } }
                            }
                        combine(itemFlows) { arrays ->
                            arrays
                                .flatMap { it.toList() }
                                .sortedByDescending { it.first.aggregateScore }
                        }
                    }
                }.catch { emit(emptyList()) }

        val state: StateFlow<HomeUiState> =
            retryTrigger
                .flatMapLatest {
                    combine(
                        categoryRepository.observeCategories(),
                        searchQuery,
                        sortOption,
                        topItemsFlow,
                    ) { categories, query, sort, topItems ->
                        val baseState =
                            HomeStateReducer.reduce(
                                categories = categories,
                                searchQuery = query,
                                sortOption = sort,
                            )
                        baseState.copy(
                            heroItem = topItems.firstOrNull()?.toHeroModel(),
                            trendingItems =
                                topItems.take(TRENDING_ITEM_COUNT).map { it.toTrendingModel() },
                        )
                    }.catch { throwable ->
                        Timber.e(throwable, "Failed to load categories")
                        emit(HomeUiState(isLoading = false, errorMessage = "Failed to load categories"))
                    }
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

        fun onCategoryClick(categoryName: String) {
            viewModelScope.launch {
                mutableNavigationEvents.emit(HomeNavigationEvent.OpenCategory(categoryName))
            }
        }
    }

private fun Pair<RankedRatedItem, String>.toHeroModel(): HomeHeroUiModel {
    val (ranked, categoryName) = this
    val score = ranked.aggregateScore
    return HomeHeroUiModel(
        name = ranked.item.id,
        tierLabel = tierFromScore(score),
        scoreText = "${String.format(Locale.ROOT, "%.1f", score)}/$SCORE_FORMAT_MAX",
        categoryName = categoryName,
    )
}

private fun Pair<RankedRatedItem, String>.toTrendingModel(): HomeTrendingItemUiModel {
    val (ranked, categoryName) = this
    val score = ranked.aggregateScore
    val scoreText = "${String.format(Locale.ROOT, "%.1f", score)}/$SCORE_FORMAT_MAX"
    return HomeTrendingItemUiModel(
        name = ranked.item.id,
        scoreText = scoreText,
        contentDescription = "${ranked.item.id}, score $scoreText",
        categoryName = categoryName,
    )
}
