@file:Suppress("FunctionName", "TooManyFunctions", "LongParameterList")

package com.juzgon.feature.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.juzgon.ui.components.JuzgonHeroCard
import com.juzgon.ui.components.JuzgonItemThumbnail
import com.juzgon.ui.components.JuzgonSegmentedFilter
import com.juzgon.ui.components.RadarChartPoint
import com.juzgon.ui.theme.JuzgonVisualTheme
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun HomeRoute(
    onNavigateToCreateCategory: () -> Unit,
    onNavigateToCategory: (String) -> Unit,
    onNavigateToItem: (String, String) -> Unit = { _, _ -> },
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(viewModel, onNavigateToCreateCategory, onNavigateToCategory, onNavigateToItem) {
        viewModel.navigationEvents.collect { event ->
            when (event) {
                HomeNavigationEvent.CreateCategory -> onNavigateToCreateCategory()
                is HomeNavigationEvent.OpenCategory -> onNavigateToCategory(event.categoryName)
                is HomeNavigationEvent.OpenItem -> onNavigateToItem(event.categoryName, event.itemId)
            }
        }
    }

    HomeScreen(
        state = state,
        actions =
            HomeScreenActions(
                onSearchQueryChange = viewModel::onSearchQueryChanged,
                onSortOptionSelected = viewModel::onSortOptionSelected,
                onCreateCategoryClick = viewModel::onCreateCategoryClick,
                onCategoryClick = viewModel::onCategoryClick,
                onRetry = viewModel::onRetry,
                onNavigateToItem = viewModel::onItemClick,
            ),
    )
}

@Composable
fun HomeScreen(
    state: HomeUiState,
    actions: HomeScreenActions,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = actions.onCreateCategoryClick,
                modifier =
                    Modifier.semantics {
                        contentDescription = "Create category"
                    },
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                )
            }
        },
        modifier = modifier,
    ) { innerPadding ->
        HomeContent(
            state = state,
            actions = actions,
            modifier = Modifier.padding(innerPadding),
        )
    }
}

@Composable
private fun HomeContent(
    state: HomeUiState,
    actions: HomeScreenActions,
    modifier: Modifier = Modifier,
) {
    when {
        state.isLoading ->
            Box(contentAlignment = Alignment.Center, modifier = modifier.fillMaxSize()) {
                CircularProgressIndicator(
                    modifier = Modifier.semantics { contentDescription = "Loading categories" },
                )
            }
        state.errorMessage != null -> HomeErrorState(state.errorMessage, actions.onRetry, modifier)
        else -> HomeCategoriesState(state, actions, modifier)
    }
}

@Composable
private fun HomeErrorState(
    errorMessage: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.fillMaxSize(),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyLarge,
            )
            Button(onClick = onRetry) {
                Text("Retry")
            }
        }
    }
}

@Composable
private fun HomeCategoriesState(
    state: HomeUiState,
    actions: HomeScreenActions,
    modifier: Modifier = Modifier,
) {
    val tokens = JuzgonVisualTheme.tokens

    LazyColumn(
        modifier =
            modifier
                .fillMaxSize()
                .testTag(HOME_CATEGORY_LIST_TAG),
        contentPadding = PaddingValues(tokens.spacing.large),
        verticalArrangement = Arrangement.spacedBy(tokens.spacing.medium),
    ) {
        item(key = "home_header") {
            HomeHeader()
        }

        state.heroItem?.let { hero ->
            item(key = "home_hero") {
                HomeHeroSection(hero = hero, onNavigateToItem = actions.onNavigateToItem)
            }
        }

        if (state.trendingItems.isNotEmpty()) {
            item(key = "home_trending") {
                HomeTrendingRow(items = state.trendingItems, onItemClick = actions.onNavigateToItem)
            }
        }

        item(key = "home_collection_summary") {
            HomeCollectionSummary(stats = state.collectionStats)
        }

        homeSearchAndSort(state, actions)
        homeCategoryItems(state, actions)
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.homeSearchAndSort(
    state: HomeUiState,
    actions: HomeScreenActions,
) {
    if (!state.isEmpty || state.hasSearchQuery) {
        item(key = "home_search") {
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = actions.onSearchQueryChange,
                label = { Text("Search categories") },
                singleLine = true,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .semantics {
                            contentDescription = "Search categories"
                        },
            )
        }

        item(key = "home_sort_controls") {
            HomeSortControls(
                selectedOption = state.sortOption,
                onSortOptionSelected = actions.onSortOptionSelected,
            )
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.homeCategoryItems(
    state: HomeUiState,
    actions: HomeScreenActions,
) {
    if (state.isEmpty) {
        item(key = "home_empty_state") {
            HomeEmptyState(
                hasSearchQuery = state.hasSearchQuery,
                onCreateCategoryClick = actions.onCreateCategoryClick,
            )
        }
    } else {
        items(
            items = state.categories,
            key = { category -> category.name },
        ) { category ->
            CategoryRow(
                category = category,
                onCategoryClick = actions.onCategoryClick,
            )
        }
    }
}

@Composable
private fun HomeHeader(modifier: Modifier = Modifier) {
    val tokens = JuzgonVisualTheme.tokens
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = "Juzgón",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = tokens.palette.textStrong,
        )
    }
}

@Composable
private fun HomeCollectionSummary(
    stats: HomeCollectionStatsUiModel,
    modifier: Modifier = Modifier,
) {
    val tokens = JuzgonVisualTheme.tokens
    val shape = RoundedCornerShape(tokens.shapes.cardCornerRadius)
    val summaryDescription =
        "Home collection summary, ${formatCategoryCount(stats.categoryCount)}, " +
            "${formatItemCount(stats.itemCount)}, ${formatAttributeCount(stats.attributeCount)}"

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(tokens.gradients.heroSurface),
                    shape = shape,
                ).semantics(mergeDescendants = true) {
                    contentDescription = summaryDescription
                }.padding(tokens.spacing.medium),
        verticalArrangement = Arrangement.spacedBy(tokens.spacing.small),
    ) {
        Text(
            text = "Collection overview",
            color = tokens.palette.textStrong,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(tokens.spacing.small),
            modifier = Modifier.fillMaxWidth(),
        ) {
            HomeStatChip(
                label = formatCategoryCount(stats.categoryCount),
                modifier = Modifier.weight(1f),
            )
            HomeStatChip(
                label = formatItemCount(stats.itemCount),
                modifier = Modifier.weight(1f),
            )
            HomeStatChip(
                label = formatAttributeCount(stats.attributeCount),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun HomeStatChip(
    label: String,
    modifier: Modifier = Modifier,
) {
    val tokens = JuzgonVisualTheme.tokens

    Box(
        contentAlignment = Alignment.Center,
        modifier =
            modifier
                .background(
                    color = tokens.palette.baseBackground.copy(alpha = 0.58f),
                    shape = RoundedCornerShape(tokens.shapes.pillCornerRadius),
                ).padding(
                    horizontal = tokens.spacing.small,
                    vertical = tokens.spacing.small,
                ),
    ) {
        Text(
            text = label,
            color = tokens.palette.textStrong,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun HomeSortControls(
    selectedOption: HomeSortOption,
    onSortOptionSelected: (HomeSortOption) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Sort",
            style = MaterialTheme.typography.labelLarge,
        )
        JuzgonSegmentedFilter(
            items = listOf("Recent", "Name"),
            selectedIndex = if (selectedOption == HomeSortOption.Recent) 0 else 1,
            onSelected = { index ->
                onSortOptionSelected(
                    if (index == 0) HomeSortOption.Recent else HomeSortOption.Name,
                )
            },
            contentDescriptions = listOf("Sort categories by recent", "Sort categories by name"),
        )
    }
}

@Composable
private fun HomeEmptyState(
    hasSearchQuery: Boolean,
    onCreateCategoryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.TopCenter,
        modifier =
            modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
    ) {
        if (hasSearchQuery) {
            Text(
                text = "No categories match your search",
                style = MaterialTheme.typography.bodyLarge,
            )
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = "No categories yet",
                    style = MaterialTheme.typography.bodyLarge,
                )
                Button(
                    onClick = onCreateCategoryClick,
                    modifier =
                        Modifier.semantics {
                            contentDescription = "Create category"
                            role = Role.Button
                        },
                ) {
                    Text("Create category")
                }
            }
        }
    }
}

@Composable
private fun CategoryRow(
    category: HomeCategoryUiModel,
    onCategoryClick: (String) -> Unit,
) {
    val tokens = JuzgonVisualTheme.tokens
    val summary = buildCategorySummary(category.itemCount, category.attributeCount)
    val shape = RoundedCornerShape(tokens.shapes.cardCornerRadius)

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .sizeIn(minHeight = 48.dp)
                .background(
                    color = tokens.palette.elevatedBackground,
                    shape = shape,
                ).clickable(
                    onClickLabel = "Open category ${category.name}",
                    role = Role.Button,
                ) {
                    onCategoryClick(category.name)
                }.semantics(mergeDescendants = true) {
                    contentDescription = "Open category ${category.name}, $summary"
                    role = Role.Button
                }.padding(tokens.spacing.large),
    ) {
        Text(
            text = category.name,
            color = tokens.palette.textStrong,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(tokens.spacing.extraSmall))
        Text(
            text = summary,
            color = tokens.palette.textSoft,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

private fun buildCategorySummary(
    itemCount: Int,
    attributeCount: Int,
): String {
    val items = formatItemCount(itemCount)
    val attrs = formatAttributeCount(attributeCount)
    return "$items · $attrs"
}

private fun formatCategoryCount(count: Int): String = if (count == 1) "1 category" else "$count categories"

private fun formatItemCount(count: Int): String = if (count == 1) "1 item" else "$count items"

private fun formatAttributeCount(count: Int): String = if (count == 1) "1 attribute" else "$count attributes"

@Composable
internal fun MiniRadarPreview(
    points: List<RadarChartPoint> = emptyList(),
    modifier: Modifier = Modifier,
) {
    val tokens = JuzgonVisualTheme.tokens
    val fillColor = tokens.palette.secondaryGlow.copy(alpha = MINI_RADAR_FILL_ALPHA)
    val strokeColor = tokens.palette.primaryGlow
    val effectivePoints =
        if (points.size >= MINI_RADAR_MIN_POINTS) {
            points
        } else {
            listOf(
                RadarChartPoint("P1", RADAR_FALLBACK_VAL_1),
                RadarChartPoint("P2", RADAR_FALLBACK_VAL_2),
                RadarChartPoint("P3", RADAR_FALLBACK_VAL_3),
                RadarChartPoint("P4", RADAR_FALLBACK_VAL_4),
            )
        }

    Canvas(
        modifier =
            modifier
                .size(MINI_RADAR_SIZE_DP.dp)
                .semantics { contentDescription = "Mini radar canvas preview" },
    ) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension * MINI_RADAR_RADIUS_FRACTION
        val path = Path()
        val count = effectivePoints.size
        val angleStep = (2 * PI / count).toFloat()

        effectivePoints.forEachIndexed { i, pt ->
            val angle = angleStep * i - (PI / 2).toFloat()
            val r = radius * pt.fraction
            val x = center.x + r * cos(angle)
            val y = center.y + r * sin(angle)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        drawPath(path, color = fillColor)
        drawPath(path, color = strokeColor, style = Stroke(width = MINI_RADAR_STROKE_WIDTH_DP.dp.toPx()))
    }
}

@Composable
private fun HomeHeroSection(
    hero: HomeHeroUiModel,
    onNavigateToItem: (String, String) -> Unit,
) {
    JuzgonHeroCard(
        title = hero.name,
        tierLabel = hero.tierLabel,
        scoreText = hero.scoreText,
        categoryTag = hero.categoryName,
        onClick = { onNavigateToItem(hero.categoryName, hero.itemId) },
        radarPreview = { MiniRadarPreview(points = hero.radarPoints) },
        image = { Text(hero.name.take(1)) },
    )
}

@Composable
private fun HomeTrendingRow(
    items: List<HomeTrendingItemUiModel>,
    onItemClick: (String, String) -> Unit,
) {
    val tokens = JuzgonVisualTheme.tokens

    Column {
        Text(
            text = "Trending",
            color = tokens.palette.textStrong,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(tokens.spacing.small))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(tokens.spacing.small),
            contentPadding = PaddingValues(horizontal = tokens.spacing.extraSmall),
        ) {
            items(items = items, key = { "${it.categoryName}:${it.itemId}" }) { item ->
                JuzgonItemThumbnail(
                    scoreText = item.scoreText,
                    contentDescription = item.contentDescription,
                    onClick = { onItemClick(item.categoryName, item.itemId) },
                    image = { Text(item.name.take(1)) },
                )
            }
        }
    }
}

internal const val HOME_CATEGORY_LIST_TAG = "Home category list"

private const val MINI_RADAR_SIZE_DP = 48
private const val MINI_RADAR_MIN_POINTS = 3
private const val MINI_RADAR_RADIUS_FRACTION = 0.45f
private const val RADAR_FALLBACK_VAL_1 = 8f
private const val RADAR_FALLBACK_VAL_2 = 9f
private const val RADAR_FALLBACK_VAL_3 = 7f
private const val RADAR_FALLBACK_VAL_4 = 8.5f
private const val MINI_RADAR_STROKE_WIDTH_DP = 2
private const val MINI_RADAR_FILL_ALPHA = 0.4f
