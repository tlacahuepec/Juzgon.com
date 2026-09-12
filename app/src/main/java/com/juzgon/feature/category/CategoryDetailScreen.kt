@file:Suppress("FunctionName", "LongMethod", "LongParameterList", "TooManyFunctions", "MagicNumber")

package com.juzgon.feature.category

import android.content.ContentResolver
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.juzgon.domain.AttributeType
import com.juzgon.domain.SkinTypeValues
import com.juzgon.ui.components.JuzgonAvatar
import com.juzgon.ui.components.JuzgonCollectionCard
import com.juzgon.ui.components.JuzgonCollectionCardMetadata
import com.juzgon.ui.components.JuzgonCollectionCardMetric
import com.juzgon.ui.components.JuzgonCollectionGridCard
import com.juzgon.ui.components.JuzgonSegmentedFilter
import com.juzgon.ui.theme.JuzgonVisualTheme

private const val GRID_COLUMN_COUNT = 2
private const val DASH_ON_INTERVAL = 12f
private const val DASH_OFF_INTERVAL = 8f
private val GlassBorderColor = Color(0x1FFFFFFF)

@Composable
fun CategoryDetailRoute(
    categoryName: String,
    onBackClick: () -> Unit,
    onAddItemClick: () -> Unit,
    onEditItemClick: (String, String?) -> Unit,
    onEditCategoryClick: () -> Unit,
    onDeleteCategoryComplete: () -> Unit,
    onScoreProfilesClick: () -> Unit,
    viewModel: CategoryDetailViewModel = hiltViewModel(),
) {
    LaunchedEffect(categoryName) {
        viewModel.loadCategory(categoryName)
    }

    LaunchedEffect(viewModel) {
        viewModel.navigationEvents.collect { event ->
            when (event) {
                CategoryDetailNavigationEvent.NavigateToEditCategory -> onEditCategoryClick()
                CategoryDetailNavigationEvent.NavigateBack -> onDeleteCategoryComplete()
            }
        }
    }

    val state by viewModel.state.collectAsState()
    CategoryDetailScreen(
        state = state,
        onBackClick = onBackClick,
        onRetry = viewModel::onRetry,
        onSortOptionSelected = viewModel::onSortOptionSelected,
        onSearchQueryChanged = viewModel::onSearchQueryChanged,
        onAddItemClick = onAddItemClick,
        onEditItemClick = { itemId -> onEditItemClick(itemId, state.activeProfileId) },
        onDeleteClick = viewModel::onDeleteClick,
        onDeleteConfirmed = viewModel::onDeleteConfirmed,
        onDeleteDialogDismissed = viewModel::onDeleteDialogDismissed,
        onEditCategoryClick = viewModel::onEditCategoryClick,
        onScoreProfilesClick = onScoreProfilesClick,
        onProfileSelected = viewModel::onProfileSelected,
        onFilterSelected = viewModel::onFilterSelected,
        onFilterCleared = viewModel::onFilterCleared,
        onVisibleRangeSelected = viewModel::onVisibleRangeSelected,
        onViewModeToggled = viewModel::onViewModeToggled,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryDetailScreen(
    state: CategoryDetailUiState,
    onBackClick: () -> Unit,
    onRetry: () -> Unit,
    onSortOptionSelected: (CategoryDetailSortOption) -> Unit,
    onSearchQueryChanged: (String) -> Unit = {},
    onAddItemClick: () -> Unit,
    onEditItemClick: (String) -> Unit,
    onDeleteClick: () -> Unit = {},
    onDeleteConfirmed: () -> Unit = {},
    onDeleteDialogDismissed: () -> Unit = {},
    onEditCategoryClick: () -> Unit = {},
    onScoreProfilesClick: () -> Unit = {},
    onProfileSelected: (String?) -> Unit = {},
    onFilterSelected: (AttributeFilter) -> Unit = {},
    onFilterCleared: (String) -> Unit = {},
    onVisibleRangeSelected: (CategoryDetailVisibleRange) -> Unit = {},
    onViewModeToggled: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    if (state.showDeleteConfirmDialog || state.showDeleteWithItemsWarning) {
        DeleteCategoryDialog(
            hasItems = state.showDeleteWithItemsWarning,
            itemCount = state.items.size,
            onConfirm = onDeleteConfirmed,
            onDismiss = onDeleteDialogDismissed,
        )
    }

    val tokens = JuzgonVisualTheme.tokens

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = state.categoryName.ifBlank { "Category" },
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge,
                        color = tokens.palette.textStrong,
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier =
                            Modifier
                                .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                                .semantics {
                                    contentDescription = "Back"
                                    role = Role.Button
                                },
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            tint = tokens.palette.textStrong,
                        )
                    }
                },
                actions = {
                    if (!state.isLoading && state.errorMessage == null) {
                        IconButton(
                            onClick = onScoreProfilesClick,
                            modifier =
                                Modifier
                                    .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                                    .semantics {
                                        contentDescription = "Score profiles"
                                        role = Role.Button
                                    },
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = null,
                                tint = tokens.palette.ratingAccent,
                            )
                        }
                        IconButton(
                            onClick = onEditCategoryClick,
                            modifier =
                                Modifier
                                    .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                                    .semantics {
                                        contentDescription = "Edit category"
                                        role = Role.Button
                                    },
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = null,
                                tint = tokens.palette.textMuted,
                            )
                        }
                        IconButton(
                            onClick = onDeleteClick,
                            modifier =
                                Modifier
                                    .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                                    .semantics {
                                        contentDescription = "Delete category"
                                        role = Role.Button
                                    },
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                        IconButton(
                            onClick = onAddItemClick,
                            modifier =
                                Modifier
                                    .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                                    .semantics {
                                        contentDescription = "Add item"
                                        role = Role.Button
                                    },
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = null,
                                tint = tokens.palette.primaryGlow,
                            )
                        }
                    }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = tokens.palette.baseBackground,
                    ),
            )
        },
        floatingActionButton = {
            if (!state.isLoading && state.errorMessage == null) {
                FloatingActionButton(
                    onClick = onAddItemClick,
                    containerColor = tokens.palette.primaryGlow,
                    contentColor = tokens.palette.textStrong,
                    modifier =
                        Modifier.semantics {
                            contentDescription = "Add item FAB"
                        },
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null,
                    )
                }
            }
        },
        modifier = modifier,
    ) { innerPadding ->
        CategoryDetailContent(
            state = state,
            onRetry = onRetry,
            onSortOptionSelected = onSortOptionSelected,
            onSearchQueryChanged = onSearchQueryChanged,
            onAddItemClick = onAddItemClick,
            onEditItemClick = onEditItemClick,
            onProfileSelected = onProfileSelected,
            onFilterSelected = onFilterSelected,
            onFilterCleared = onFilterCleared,
            onVisibleRangeSelected = onVisibleRangeSelected,
            onViewModeToggled = onViewModeToggled,
            modifier = Modifier.padding(innerPadding),
        )
    }
}

@Composable
private fun DeleteCategoryDialog(
    hasItems: Boolean,
    itemCount: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete category") },
        text = {
            if (hasItems) {
                Text(
                    "This category has $itemCount ${if (itemCount == 1) "item" else "items"} " +
                        "that will also be deleted. This action cannot be undone.",
                )
            } else {
                Text("Are you sure you want to delete this category? This action cannot be undone.")
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                modifier = Modifier.semantics { contentDescription = "Confirm delete" },
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.semantics { contentDescription = "Cancel delete" },
            ) {
                Text("Cancel")
            }
        },
    )
}

@Composable
private fun CategoryDetailContent(
    state: CategoryDetailUiState,
    onRetry: () -> Unit,
    onSortOptionSelected: (CategoryDetailSortOption) -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onAddItemClick: () -> Unit,
    onEditItemClick: (String) -> Unit,
    onProfileSelected: (String?) -> Unit,
    onFilterSelected: (AttributeFilter) -> Unit,
    onFilterCleared: (String) -> Unit,
    onVisibleRangeSelected: (CategoryDetailVisibleRange) -> Unit,
    onViewModeToggled: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        state.isLoading -> CenteredContent(modifier = modifier) { CircularProgressIndicator() }
        state.errorMessage != null -> CategoryDetailErrorState(state.errorMessage, onRetry, modifier)
        !state.hasItems -> CategoryDetailEmptyState(state.attributeSummary, onAddItemClick, modifier)
        else ->
            CategoryDetailItemList(
                state = state,
                onSortOptionSelected = onSortOptionSelected,
                onSearchQueryChanged = onSearchQueryChanged,
                onAddItemClick = onAddItemClick,
                onEditItemClick = onEditItemClick,
                onProfileSelected = onProfileSelected,
                onFilterSelected = onFilterSelected,
                onFilterCleared = onFilterCleared,
                onVisibleRangeSelected = onVisibleRangeSelected,
                onViewModeToggled = onViewModeToggled,
                modifier = modifier,
            )
    }
}

@Composable
private fun CategoryDetailErrorState(
    errorMessage: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CenteredContent(modifier = modifier) {
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
private fun CategoryDetailEmptyState(
    attributeSummary: String,
    onAddItemClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CenteredContent(modifier = modifier) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (attributeSummary.isNotBlank()) {
                Text(
                    text = attributeSummary,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Text(
                text = "No items yet",
                style = MaterialTheme.typography.bodyLarge,
            )
            Button(onClick = onAddItemClick) {
                Text("Add item")
            }
        }
    }
}

@Composable
private fun CategoryDetailItemList(
    state: CategoryDetailUiState,
    onSortOptionSelected: (CategoryDetailSortOption) -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onAddItemClick: () -> Unit,
    onEditItemClick: (String) -> Unit,
    onProfileSelected: (String?) -> Unit,
    onFilterSelected: (AttributeFilter) -> Unit,
    onFilterCleared: (String) -> Unit,
    onVisibleRangeSelected: (CategoryDetailVisibleRange) -> Unit,
    onViewModeToggled: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.viewMode == CategoryDetailViewMode.GRID) {
        CategoryDetailGridLayout(
            state = state,
            onSortOptionSelected = onSortOptionSelected,
            onSearchQueryChanged = onSearchQueryChanged,
            onAddItemClick = onAddItemClick,
            onEditItemClick = onEditItemClick,
            onProfileSelected = onProfileSelected,
            onFilterSelected = onFilterSelected,
            onFilterCleared = onFilterCleared,
            onVisibleRangeSelected = onVisibleRangeSelected,
            onViewModeToggled = onViewModeToggled,
            modifier = modifier,
        )
    } else {
        CategoryDetailListLayout(
            state = state,
            onSortOptionSelected = onSortOptionSelected,
            onSearchQueryChanged = onSearchQueryChanged,
            onEditItemClick = onEditItemClick,
            onProfileSelected = onProfileSelected,
            onFilterSelected = onFilterSelected,
            onFilterCleared = onFilterCleared,
            onVisibleRangeSelected = onVisibleRangeSelected,
            onViewModeToggled = onViewModeToggled,
            modifier = modifier,
        )
    }
}

@Composable
private fun CategoryDetailListLayout(
    state: CategoryDetailUiState,
    onSortOptionSelected: (CategoryDetailSortOption) -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onEditItemClick: (String) -> Unit,
    onProfileSelected: (String?) -> Unit,
    onFilterSelected: (AttributeFilter) -> Unit,
    onFilterCleared: (String) -> Unit,
    onVisibleRangeSelected: (CategoryDetailVisibleRange) -> Unit,
    onViewModeToggled: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(24.dp),
        modifier = modifier.fillMaxSize(),
    ) {
        item {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = state.attributeSummary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = JuzgonVisualTheme.tokens.palette.primaryGlow,
                )
                ViewModeToggle(state = state, onViewModeToggled = onViewModeToggled)
            }
        }
        if (state.profiles.isNotEmpty()) {
            item {
                ProfileSelector(
                    profiles = state.profiles,
                    activeProfileId = state.activeProfileId,
                    activeProfileLabel = state.activeProfileLabel,
                    onProfileSelected = onProfileSelected,
                )
            }
        }
        item {
            CategoryDetailSortControls(
                selectedOption = state.sortOption,
                sortOptions = state.sortOptions,
                onSortOptionSelected = onSortOptionSelected,
            )
        }
        if (state.visibleRangeOptions.isNotEmpty()) {
            item {
                VisibleRangeChips(
                    selectedRange = state.visibleRange,
                    options = state.visibleRangeOptions,
                    onRangeSelected = onVisibleRangeSelected,
                )
            }
        }
        item {
            CategoryDetailSearchBar(
                query = state.searchQuery,
                onQueryChanged = onSearchQueryChanged,
            )
        }
        if (state.filterChips.isNotEmpty()) {
            item {
                AttributeFilterChipRow(
                    chips = state.filterChips,
                    onFilterSelected = onFilterSelected,
                    onFilterCleared = onFilterCleared,
                )
            }
        }
        items(
            items = state.items,
            key = { item -> item.id },
        ) { item ->
            CategoryDetailItemCard(
                item = item,
                onEditItemClick = onEditItemClick,
            )
        }
    }
}

@Composable
private fun CategoryDetailGridLayout(
    state: CategoryDetailUiState,
    onSortOptionSelected: (CategoryDetailSortOption) -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onAddItemClick: () -> Unit,
    onEditItemClick: (String) -> Unit,
    onProfileSelected: (String?) -> Unit,
    onFilterSelected: (AttributeFilter) -> Unit,
    onFilterCleared: (String) -> Unit,
    onVisibleRangeSelected: (CategoryDetailVisibleRange) -> Unit,
    onViewModeToggled: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(GRID_COLUMN_COUNT),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
        modifier = modifier.fillMaxSize(),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = state.attributeSummary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = JuzgonVisualTheme.tokens.palette.primaryGlow,
                )
                ViewModeToggle(state = state, onViewModeToggled = onViewModeToggled)
            }
        }
        if (state.profiles.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                ProfileSelector(
                    profiles = state.profiles,
                    activeProfileId = state.activeProfileId,
                    activeProfileLabel = state.activeProfileLabel,
                    onProfileSelected = onProfileSelected,
                )
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            CategoryDetailSortControls(
                selectedOption = state.sortOption,
                sortOptions = state.sortOptions,
                onSortOptionSelected = onSortOptionSelected,
            )
        }
        if (state.visibleRangeOptions.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                VisibleRangeChips(
                    selectedRange = state.visibleRange,
                    options = state.visibleRangeOptions,
                    onRangeSelected = onVisibleRangeSelected,
                )
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            CategoryDetailSearchBar(
                query = state.searchQuery,
                onQueryChanged = onSearchQueryChanged,
            )
        }
        if (state.filterChips.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                AttributeFilterChipRow(
                    chips = state.filterChips,
                    onFilterSelected = onFilterSelected,
                    onFilterCleared = onFilterCleared,
                )
            }
        }
        item(key = "add_new_persona_card") {
            AddNewPersonaGridCard(onAddItemClick = onAddItemClick)
        }
        items(
            items = state.items,
            key = { item -> item.id },
        ) { item ->
            CategoryDetailGridCard(
                item = item,
                onEditItemClick = onEditItemClick,
            )
        }
    }
}

@Composable
private fun AddNewPersonaGridCard(
    onAddItemClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = JuzgonVisualTheme.tokens
    val cardShape = RoundedCornerShape(tokens.shapes.cardCornerRadius)
    val borderColor = tokens.palette.primaryGlow.copy(alpha = 0.5f)

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .sizeIn(minWidth = 48.dp, minHeight = 140.dp)
                .clip(cardShape)
                .background(tokens.palette.primaryGlow.copy(alpha = 0.08f))
                .drawBehind {
                    val strokeWidth = 1.5.dp.toPx()
                    val halfStroke = strokeWidth / 2f
                    val radiusPx =
                        (tokens.shapes.cardCornerRadius.toPx() - halfStroke).coerceAtLeast(0f)
                    drawRoundRect(
                        color = borderColor,
                        topLeft = Offset(halfStroke, halfStroke),
                        size = Size(size.width - strokeWidth, size.height - strokeWidth),
                        style =
                            Stroke(
                                width = strokeWidth,
                                pathEffect =
                                    PathEffect.dashPathEffect(
                                        floatArrayOf(DASH_ON_INTERVAL, DASH_OFF_INTERVAL),
                                        0f,
                                    ),
                                cap = StrokeCap.Round,
                            ),
                        cornerRadius = CornerRadius(radiusPx),
                    )
                }.clickable(
                    role = Role.Button,
                    onClick = onAddItemClick,
                ).semantics(mergeDescendants = true) {
                    contentDescription = "Add new persona"
                    role = Role.Button
                }.padding(tokens.spacing.small),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .size(36.dp)
                    .background(
                        brush =
                            Brush.linearGradient(
                                listOf(tokens.palette.primaryGlow, tokens.palette.contrastAccent),
                            ),
                        shape = CircleShape,
                    ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                tint = tokens.palette.textStrong,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(modifier = Modifier.height(tokens.spacing.extraSmall))
        Text(
            text = "Add New Persona",
            color = tokens.palette.textStrong,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "Tap to evaluate or generate with AI",
            color = tokens.palette.textMuted,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ViewModeToggle(
    state: CategoryDetailUiState,
    onViewModeToggled: () -> Unit,
) {
    JuzgonSegmentedFilter(
        items = listOf("List", "Grid"),
        selectedIndex = if (state.viewMode == CategoryDetailViewMode.GRID) 1 else 0,
        onSelected = { onViewModeToggled() },
        contentDescriptions = listOf("View as list", "View as grid"),
    )
}

@Composable
private fun CategoryDetailGridCard(
    item: CategoryDetailItemUiModel,
    onEditItemClick: (String) -> Unit,
) {
    JuzgonCollectionGridCard(
        name = item.id,
        rank = item.rank,
        tierLabel = item.tierLabel,
        scoreText = item.averageScoreText,
        onClick = { onEditItemClick(item.id) },
        onFavoriteClick = {},
        radarValues = item.radarValues,
        attributes = item.gridAttributes,
        image = { CategoryDetailItemVisual(item = item) },
    )
}

@Suppress("UnusedParameter")
@Composable
private fun categoryChipColors(isSelected: Boolean) =
    FilterChipDefaults.filterChipColors(
        selectedContainerColor =
            JuzgonVisualTheme.tokens.palette.primaryGlow
                .copy(alpha = 0.2f),
        selectedLabelColor = JuzgonVisualTheme.tokens.palette.textStrong,
        containerColor =
            JuzgonVisualTheme.tokens.palette.panelBackground
                .copy(alpha = 0.6f),
        labelColor = JuzgonVisualTheme.tokens.palette.textMuted,
    )

@Composable
private fun categoryChipBorder(isSelected: Boolean) =
    FilterChipDefaults.filterChipBorder(
        enabled = true,
        selected = isSelected,
        borderColor =
            if (isSelected) {
                JuzgonVisualTheme.tokens.palette.primaryGlow
            } else {
                GlassBorderColor
            },
    )

@Composable
private fun CategoryDetailSearchBar(
    query: String,
    onQueryChanged: (String) -> Unit,
) {
    val tokens = JuzgonVisualTheme.tokens
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChanged,
        placeholder = {
            Text(
                text = "Search items…",
                style = MaterialTheme.typography.bodyMedium,
                color = tokens.palette.textMuted,
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = "Search",
                tint = tokens.palette.primaryGlow,
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChanged("") }) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Clear search",
                        tint = tokens.palette.textMuted,
                    )
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(tokens.shapes.pillCornerRadius),
        colors =
            OutlinedTextFieldDefaults.colors(
                focusedContainerColor = tokens.palette.panelBackground,
                unfocusedContainerColor = tokens.palette.panelBackground.copy(alpha = 0.6f),
                focusedBorderColor = tokens.palette.primaryGlow,
                unfocusedBorderColor = GlassBorderColor,
                focusedTextColor = tokens.palette.textStrong,
                unfocusedTextColor = tokens.palette.textStrong,
                cursorColor = tokens.palette.primaryGlow,
            ),
        modifier = Modifier.fillMaxWidth(),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AttributeFilterChipRow(
    chips: List<FilterChipUiModel>,
    onFilterSelected: (AttributeFilter) -> Unit,
    onFilterCleared: (String) -> Unit,
) {
    var activeSheet by remember { mutableStateOf<FilterChipUiModel?>(null) }
    val tokens = JuzgonVisualTheme.tokens

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.horizontalScroll(rememberScrollState()),
    ) {
        chips.forEach { chip ->
            FilterChip(
                selected = chip.isActive,
                onClick = {
                    if (chip.isActive) {
                        onFilterCleared(chip.attributeId)
                    } else {
                        activeSheet = chip
                    }
                },
                label = { Text(chip.activeLabel ?: chip.label) },
                colors = categoryChipColors(chip.isActive),
                border = categoryChipBorder(chip.isActive),
                shape = RoundedCornerShape(tokens.shapes.pillCornerRadius),
                trailingIcon = {
                    if (chip.isActive) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "Clear filter: ${chip.label}",
                            modifier = Modifier.sizeIn(maxWidth = 18.dp, maxHeight = 18.dp),
                        )
                    }
                },
                modifier =
                    Modifier
                        .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                        .semantics {
                            contentDescription = "Filter: ${chip.label}"
                        },
            )
        }
    }

    activeSheet?.let { chip ->
        AttributeFilterSheet(
            chip = chip,
            onApply = { filter ->
                onFilterSelected(filter)
                activeSheet = null
            },
            onDismiss = { activeSheet = null },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AttributeFilterSheet(
    chip: FilterChipUiModel,
    onApply: (AttributeFilter) -> Unit,
    onDismiss: () -> Unit,
) {
    when (chip.type) {
        AttributeType.NATIONALITY -> NationalityFilterSheet(chip, onApply, onDismiss)
        AttributeType.DROPDOWN -> DropdownFilterSheet(chip, onApply, onDismiss)
        AttributeType.SKIN_TYPE -> SkinTypeFilterSheet(chip, onApply, onDismiss)
        AttributeType.BOOLEAN -> BooleanFilterSheet(chip, onApply, onDismiss)
        AttributeType.NUMBER -> NumberRangeFilterSheet(chip, onApply, onDismiss)
        AttributeType.DATE -> DateRangeFilterSheet(chip, onApply, onDismiss)
        else -> onDismiss()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NationalityFilterSheet(
    chip: FilterChipUiModel,
    onApply: (AttributeFilter) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selected by remember { mutableStateOf(emptySet<String>()) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(
                text = "Filter by ${chip.label}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                items(items = chip.availableValues, key = { it }) { value ->
                    FilterOptionRow(
                        label = value,
                        isSelected = value in selected,
                        onClick = {
                            selected = if (value in selected) selected - value else selected + value
                        },
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    if (selected.isNotEmpty()) {
                        onApply(AttributeFilter.Nationality(chip.attributeId, selected))
                    }
                },
                enabled = selected.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Apply") }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropdownFilterSheet(
    chip: FilterChipUiModel,
    onApply: (AttributeFilter) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selected by remember { mutableStateOf(emptySet<String>()) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(
                text = "Filter by ${chip.label}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                items(items = chip.availableValues, key = { it }) { value ->
                    FilterOptionRow(
                        label = value,
                        isSelected = value in selected,
                        onClick = {
                            selected = if (value in selected) selected - value else selected + value
                        },
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    if (selected.isNotEmpty()) {
                        onApply(AttributeFilter.Dropdown(chip.attributeId, selected))
                    }
                },
                enabled = selected.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Apply") }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SkinTypeFilterSheet(
    chip: FilterChipUiModel,
    onApply: (AttributeFilter) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selected by remember { mutableStateOf(emptySet<String>()) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(
                text = "Filter by ${chip.label}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                items(items = chip.availableValues, key = { it }) { value ->
                    FilterOptionRow(
                        label = SkinTypeValues.displayLabelOrUnknown(value),
                        isSelected = value in selected,
                        onClick = {
                            selected = if (value in selected) selected - value else selected + value
                        },
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    if (selected.isNotEmpty()) {
                        onApply(AttributeFilter.SkinType(chip.attributeId, selected))
                    }
                },
                enabled = selected.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Apply") }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BooleanFilterSheet(
    chip: FilterChipUiModel,
    onApply: (AttributeFilter) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(
                text = "Filter by ${chip.label}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            FilterOptionRow(
                label = "Yes",
                isSelected = false,
                onClick = { onApply(AttributeFilter.BooleanFilter(chip.attributeId, true)) },
            )
            FilterOptionRow(
                label = "No",
                isSelected = false,
                onClick = { onApply(AttributeFilter.BooleanFilter(chip.attributeId, false)) },
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NumberRangeFilterSheet(
    chip: FilterChipUiModel,
    onApply: (AttributeFilter) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var range by remember { mutableStateOf(SCORE_RANGE_MIN..SCORE_RANGE_MAX) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(
                text = "Filter by ${chip.label}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            Text(
                text = "${range.start.toInt()} – ${range.endInclusive.toInt()}",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            RangeSlider(
                value = range,
                onValueChange = { range = it },
                valueRange = SCORE_RANGE_MIN..SCORE_RANGE_MAX,
                steps = SCORE_RANGE_STEPS,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    onApply(
                        AttributeFilter.NumberRange(
                            chip.attributeId,
                            min = range.start.toInt(),
                            max = range.endInclusive.toInt(),
                        ),
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Apply") }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateRangeFilterSheet(
    chip: FilterChipUiModel,
    onApply: (AttributeFilter) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var startDate by remember { mutableStateOf("") }
    var endDate by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(
                text = "Filter by ${chip.label}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            OutlinedTextField(
                value = startDate,
                onValueChange = { startDate = it },
                label = { Text("From (yyyy-MM-dd)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            )
            OutlinedTextField(
                value = endDate,
                onValueChange = { endDate = it },
                label = { Text("To (yyyy-MM-dd)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            )
            Button(
                onClick = {
                    onApply(
                        AttributeFilter.DateRange(
                            chip.attributeId,
                            startDate = startDate.takeIf { it.isNotBlank() },
                            endDate = endDate.takeIf { it.isNotBlank() },
                        ),
                    )
                },
                enabled = startDate.isNotBlank() || endDate.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Apply") }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun FilterOptionRow(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val tokens = JuzgonVisualTheme.tokens
    Surface(
        onClick = onClick,
        color =
            if (isSelected) {
                tokens.palette.primaryGlow.copy(alpha = 0.2f)
            } else {
                tokens.palette.panelBackground
            },
        shape = RoundedCornerShape(tokens.shapes.pillCornerRadius),
        border =
            if (isSelected) {
                BorderStroke(1.dp, tokens.palette.primaryGlow)
            } else {
                BorderStroke(1.dp, GlassBorderColor)
            },
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .sizeIn(minHeight = 48.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Text(
                text = label,
                color = if (isSelected) tokens.palette.textStrong else tokens.palette.textMuted,
                modifier = Modifier.weight(1f),
            )
            if (isSelected) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = "Selected",
                    tint = tokens.palette.primaryGlow,
                )
            }
        }
    }
}

private const val INLINE_SORT_OPTIONS_THRESHOLD = 6
private const val SORT_SHEET_SEARCH_THRESHOLD = 10
private const val SCORE_RANGE_MIN = 1f
private const val SCORE_RANGE_MAX = 10f
private const val SCORE_RANGE_STEPS = 8

@Composable
private fun ProfileSelector(
    profiles: List<ProfileOption>,
    activeProfileId: String?,
    activeProfileLabel: String?,
    onProfileSelected: (String?) -> Unit,
) {
    val tokens = JuzgonVisualTheme.tokens
    Column {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.horizontalScroll(rememberScrollState()),
        ) {
            profiles.forEach { profile ->
                val isSelected = profile.id == activeProfileId
                FilterChip(
                    selected = isSelected,
                    onClick = { onProfileSelected(profile.id) },
                    label = { Text(profile.name) },
                    colors =
                        FilterChipDefaults.filterChipColors(
                            selectedContainerColor = tokens.palette.secondaryGlow,
                            selectedLabelColor = tokens.palette.textStrong,
                            containerColor = tokens.palette.panelBackground.copy(alpha = 0.6f),
                            labelColor = tokens.palette.textMuted,
                        ),
                    border =
                        FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = if (isSelected) tokens.palette.primaryGlowStrong else Color.Transparent,
                        ),
                    shape = RoundedCornerShape(tokens.shapes.pillCornerRadius),
                    modifier =
                        Modifier
                            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                            .semantics {
                                contentDescription = "Profile: ${profile.name}"
                            },
                )
            }
        }
        if (activeProfileLabel != null) {
            Text(
                text = activeProfileLabel,
                color = tokens.palette.ratingAccent,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun VisibleRangeChips(
    selectedRange: CategoryDetailVisibleRange,
    options: List<CategoryDetailVisibleRange>,
    onRangeSelected: (CategoryDetailVisibleRange) -> Unit,
) {
    val tokens = JuzgonVisualTheme.tokens
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.horizontalScroll(rememberScrollState()),
    ) {
        options.forEach { range ->
            val isSelected = selectedRange == range
            FilterChip(
                selected = isSelected,
                onClick = { onRangeSelected(range) },
                label = { Text(range.label()) },
                colors = categoryChipColors(isSelected),
                border = categoryChipBorder(isSelected),
                shape = RoundedCornerShape(tokens.shapes.pillCornerRadius),
                modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp),
            )
        }
    }
}

private fun CategoryDetailVisibleRange.label(): String =
    when (this) {
        CategoryDetailVisibleRange.Top10 -> "Top 10"
        CategoryDetailVisibleRange.Top20 -> "Top 20"
        CategoryDetailVisibleRange.Top50 -> "Top 50"
        CategoryDetailVisibleRange.All -> "All"
    }

@Composable
private fun CategoryDetailSortControls(
    selectedOption: CategoryDetailSortOption,
    sortOptions: List<CategoryDetailSortOptionUiModel>,
    onSortOptionSelected: (CategoryDetailSortOption) -> Unit,
) {
    if (sortOptions.size <= INLINE_SORT_OPTIONS_THRESHOLD) {
        InlineSortChips(selectedOption, sortOptions, onSortOptionSelected)
    } else {
        CompactSortTrigger(selectedOption, sortOptions, onSortOptionSelected)
    }
}

@Composable
private fun InlineSortChips(
    selectedOption: CategoryDetailSortOption,
    sortOptions: List<CategoryDetailSortOptionUiModel>,
    onSortOptionSelected: (CategoryDetailSortOption) -> Unit,
) {
    val tokens = JuzgonVisualTheme.tokens
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.horizontalScroll(rememberScrollState()),
    ) {
        sortOptions.forEach { sortOption ->
            val isSelected = selectedOption == sortOption.option
            FilterChip(
                selected = isSelected,
                onClick = { onSortOptionSelected(sortOption.option) },
                label = { Text(sortOption.label) },
                colors = categoryChipColors(isSelected),
                border = categoryChipBorder(isSelected),
                shape = RoundedCornerShape(tokens.shapes.pillCornerRadius),
                modifier =
                    Modifier
                        .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                        .semantics {
                            contentDescription = sortOption.contentDescription
                        },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CompactSortTrigger(
    selectedOption: CategoryDetailSortOption,
    sortOptions: List<CategoryDetailSortOptionUiModel>,
    onSortOptionSelected: (CategoryDetailSortOption) -> Unit,
) {
    var showSheet by remember { mutableStateOf(false) }
    val selectedLabel =
        sortOptions.firstOrNull { it.option == selectedOption }?.label ?: "Score"
    val tokens = JuzgonVisualTheme.tokens

    FilterChip(
        selected = true,
        onClick = { showSheet = true },
        label = { Text("Sorted by: $selectedLabel") },
        colors = categoryChipColors(true),
        border = categoryChipBorder(true),
        shape = RoundedCornerShape(tokens.shapes.pillCornerRadius),
        trailingIcon = {
            Icon(
                imageVector = Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
            )
        },
        modifier =
            Modifier
                .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                .semantics {
                    contentDescription = "Sort options, currently sorted by $selectedLabel"
                },
    )

    if (showSheet) {
        SortOptionsBottomSheet(
            selectedOption = selectedOption,
            sortOptions = sortOptions,
            onSortOptionSelected = { option ->
                onSortOptionSelected(option)
                showSheet = false
            },
            onDismiss = { showSheet = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SortOptionsBottomSheet(
    selectedOption: CategoryDetailSortOption,
    sortOptions: List<CategoryDetailSortOptionUiModel>,
    onSortOptionSelected: (CategoryDetailSortOption) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var searchQuery by remember { mutableStateOf("") }
    val showSearch = sortOptions.size >= SORT_SHEET_SEARCH_THRESHOLD

    val filteredOptions =
        remember(searchQuery, sortOptions) {
            if (searchQuery.isBlank()) {
                sortOptions
            } else {
                sortOptions.filter { option ->
                    option.label.contains(searchQuery, ignoreCase = true)
                }
            }
        }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(
                text = "Sort by",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier =
                    Modifier
                        .padding(bottom = 12.dp)
                        .semantics { contentDescription = "Sort options sheet" },
            )
            if (showSearch) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search attributes") },
                    singleLine = true,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .semantics { contentDescription = "Search sort options" },
                )
            }
            LazyColumn(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
            ) {
                items(
                    items = filteredOptions,
                    key = { option -> option.contentDescription },
                ) { sortOption ->
                    SortOptionRow(
                        sortOption = sortOption,
                        isSelected = selectedOption == sortOption.option,
                        onClick = { onSortOptionSelected(sortOption.option) },
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SortOptionRow(
    sortOption: CategoryDetailSortOptionUiModel,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val tokens = JuzgonVisualTheme.tokens
    Surface(
        onClick = onClick,
        color =
            if (isSelected) {
                tokens.palette.primaryGlow.copy(alpha = 0.2f)
            } else {
                tokens.palette.panelBackground
            },
        border =
            if (isSelected) {
                BorderStroke(1.dp, tokens.palette.primaryGlow)
            } else {
                BorderStroke(1.dp, GlassBorderColor)
            },
        shape = RoundedCornerShape(tokens.shapes.cardCornerRadius),
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .sizeIn(minHeight = 48.dp)
                .semantics {
                    contentDescription = sortOption.contentDescription
                    role = Role.Button
                },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Text(
                text = sortOption.label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isSelected) tokens.palette.textStrong else tokens.palette.textMuted,
            )
            if (isSelected) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = tokens.palette.primaryGlow,
                )
            }
        }
    }
}

@Composable
private fun CenteredContent(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.fillMaxSize(),
    ) {
        content()
    }
}

@Composable
private fun CategoryDetailItemCard(
    item: CategoryDetailItemUiModel,
    onEditItemClick: (String) -> Unit,
) {
    JuzgonCollectionCard(
        metadata =
            JuzgonCollectionCardMetadata(
                title = item.id,
                rankLabel = "#${item.rank}",
                metric =
                    JuzgonCollectionCardMetric(
                        label = item.metricLabel,
                        value = item.metricValueText,
                        swatchColorHex = item.metricColorHex,
                    ),
                badge = item.nationalityBadge,
                badgeIcons = item.socialBadgeIcons,
                contentDescription = buildItemCardContentDescription(item),
            ),
        onClick = { onEditItemClick(item.id) },
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            CategoryDetailItemVisual(item = item)
        }
    }
}

private fun buildItemCardContentDescription(item: CategoryDetailItemUiModel): String =
    buildString {
        append("Rated item ${item.id}, rank ${item.rank}")
        item.nationalityBadge?.let { append(", $it") }
        append(", ${item.metricLabel} ${item.metricValueText}")
    }

@Composable
private fun CategoryDetailItemVisual(item: CategoryDetailItemUiModel) {
    val imageValue = item.imageValue
    val cd =
        if (imageValue.isNullOrBlank()) {
            "${item.id} image placeholder"
        } else {
            "${item.id} image preview"
        }

    val context = LocalContext.current
    val bitmap =
        remember(imageValue) {
            if (!imageValue.isNullOrBlank()) {
                imageBitmapFromValue(context.contentResolver, imageValue)
            } else {
                null
            }
        }

    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = cd,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
    } else {
        JuzgonAvatar(
            name = item.id,
            contentDescription = cd,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxSize(),
        )
    }
}

private fun imageBitmapFromValue(
    contentResolver: ContentResolver,
    value: String,
) = runCatching {
    val uri = Uri.parse(value)
    contentResolver.openInputStream(uri)?.use { inputStream ->
        BitmapFactory.decodeStream(inputStream)
    }
}.getOrNull()
