@file:Suppress("FunctionName", "TooManyFunctions", "LongParameterList")

package com.juzgon.feature.home

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.juzgon.feature.about.AboutDialog
import com.juzgon.feature.about.AboutViewModel
import com.juzgon.feature.backup.ExportBackupViewModel
import com.juzgon.ui.components.JuzgonHeroCard
import com.juzgon.ui.components.JuzgonItemThumbnail
import com.juzgon.ui.components.JuzgonSegmentedFilter
import com.juzgon.ui.theme.JuzgonVisualTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun HomeRoute(
    onNavigateToCreateCategory: () -> Unit,
    onNavigateToCategory: (String) -> Unit,
    onNavigateToAiSettings: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
    exportViewModel: ExportBackupViewModel = hiltViewModel(),
    aboutViewModel: AboutViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val exportState by exportViewModel.state.collectAsState()
    val context = LocalContext.current
    var showAboutDialog by remember { mutableStateOf(false) }

    val safLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
            if (uri != null) {
                val json = exportState.exportedJson ?: return@rememberLauncherForActivityResult
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(json.toByteArray(Charsets.UTF_8))
                }
                exportViewModel.onExportConsumed()
            }
        }

    LaunchedEffect(exportState.isExportComplete) {
        if (exportState.isExportComplete && exportState.exportedJson != null) {
            val fileName = "juzgon-backup-${LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)}.juzgon.json"
            safLauncher.launch(fileName)
        }
    }

    LaunchedEffect(viewModel, onNavigateToCreateCategory) {
        viewModel.navigationEvents.collect { event ->
            when (event) {
                HomeNavigationEvent.CreateCategory -> onNavigateToCreateCategory()
                is HomeNavigationEvent.OpenCategory -> onNavigateToCategory(event.categoryName)
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
                onExportClick = exportViewModel::export,
                onAboutClick = { showAboutDialog = true },
                onAiSettingsClick = onNavigateToAiSettings,
            ),
    )

    if (showAboutDialog) {
        AboutDialog(
            metadata = aboutViewModel.metadata,
            onDismiss = { showAboutDialog = false },
        )
    }
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
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(24.dp),
    ) {
        HomeHeader(actions = actions)
        Spacer(modifier = Modifier.height(12.dp))
        if (!state.isEmpty || state.hasSearchQuery) {
            state.heroItem?.let { hero ->
                HomeHeroSection(hero = hero, onCategoryClick = actions.onCategoryClick)
                Spacer(modifier = Modifier.height(12.dp))
            }
            if (state.trendingItems.isNotEmpty()) {
                HomeTrendingRow(items = state.trendingItems, onItemClick = actions.onCategoryClick)
                Spacer(modifier = Modifier.height(12.dp))
            }
            HomeCollectionSummary(stats = state.collectionStats)
            Spacer(modifier = Modifier.height(12.dp))
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
            Spacer(modifier = Modifier.height(12.dp))
            HomeSortControls(
                selectedOption = state.sortOption,
                onSortOptionSelected = actions.onSortOptionSelected,
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
        HomeCategoryContent(
            state = state,
            onCreateCategoryClick = actions.onCreateCategoryClick,
            onCategoryClick = actions.onCategoryClick,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun HomeHeader(actions: HomeScreenActions) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = "Diamond Home",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = actions.onAiSettingsClick,
                modifier =
                    Modifier.semantics {
                        contentDescription = "AI Settings"
                    },
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                )
            }
            IconButton(
                onClick = actions.onAboutClick,
                modifier =
                    Modifier.semantics {
                        contentDescription = "About"
                    },
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                )
            }
            Button(
                onClick = actions.onExportClick,
                modifier =
                    Modifier.semantics {
                        contentDescription = "Export backup"
                    },
            ) {
                Text("Export")
            }
        }
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
private fun HomeCategoryContent(
    state: HomeUiState,
    onCreateCategoryClick: () -> Unit,
    onCategoryClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.isEmpty) {
        HomeEmptyState(
            hasSearchQuery = state.hasSearchQuery,
            onCreateCategoryClick = onCreateCategoryClick,
            modifier = modifier,
        )
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier =
                modifier
                    .fillMaxSize()
                    .testTag(HOME_CATEGORY_LIST_TAG),
        ) {
            items(
                items = state.categories,
                key = { category -> category.name },
            ) { category ->
                CategoryRow(
                    category = category,
                    onCategoryClick = onCategoryClick,
                )
            }
        }
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
                .fillMaxSize()
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
private fun HomeHeroSection(
    hero: HomeHeroUiModel,
    onCategoryClick: (String) -> Unit,
) {
    JuzgonHeroCard(
        title = hero.name,
        tierLabel = hero.tierLabel,
        scoreText = hero.scoreText,
        onClick = { onCategoryClick(hero.categoryName) },
        image = { Text(hero.name.take(1)) },
    )
}

@Composable
private fun HomeTrendingRow(
    items: List<HomeTrendingItemUiModel>,
    onItemClick: (String) -> Unit,
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
            items(items = items, key = { it.name }) { item ->
                JuzgonItemThumbnail(
                    scoreText = item.scoreText,
                    contentDescription = item.contentDescription,
                    onClick = { onItemClick(item.categoryName) },
                    image = { Text(item.name.take(1)) },
                )
            }
        }
    }
}

internal const val HOME_CATEGORY_LIST_TAG = "Home category list"
