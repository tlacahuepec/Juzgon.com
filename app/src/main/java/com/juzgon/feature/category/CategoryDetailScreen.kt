@file:Suppress("FunctionName", "LongMethod", "LongParameterList", "TooManyFunctions")

package com.juzgon.feature.category

import android.content.ContentResolver
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.juzgon.domain.AttributeType
import com.juzgon.domain.SkinTypeValues
import com.juzgon.ui.components.JuzgonCollectionCard
import com.juzgon.ui.components.JuzgonCollectionCardMetadata
import com.juzgon.ui.components.JuzgonCollectionCardMetric

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = state.categoryName.ifBlank { "Category" },
                        fontWeight = FontWeight.SemiBold,
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
                            )
                        }
                    }
                },
            )
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
                onEditItemClick = onEditItemClick,
                onProfileSelected = onProfileSelected,
                onFilterSelected = onFilterSelected,
                onFilterCleared = onFilterCleared,
                onVisibleRangeSelected = onVisibleRangeSelected,
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
    onEditItemClick: (String) -> Unit,
    onProfileSelected: (String?) -> Unit,
    onFilterSelected: (AttributeFilter) -> Unit,
    onFilterCleared: (String) -> Unit,
    onVisibleRangeSelected: (CategoryDetailVisibleRange) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(24.dp),
        modifier = modifier.fillMaxSize(),
    ) {
        item {
            Text(
                text = state.attributeSummary,
                style = MaterialTheme.typography.bodyMedium,
            )
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
private fun CategoryDetailSearchBar(
    query: String,
    onQueryChanged: (String) -> Unit,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChanged,
        placeholder = { Text("Search items…") },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search") },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChanged("") }) {
                    Icon(Icons.Filled.Close, contentDescription = "Clear search")
                }
            }
        },
        singleLine = true,
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
    Surface(
        onClick = onClick,
        modifier =
            Modifier
                .fillMaxWidth()
                .sizeIn(minHeight = 48.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
        ) {
            Text(text = label, modifier = Modifier.weight(1f))
            if (isSelected) {
                Icon(Icons.Filled.Check, contentDescription = "Selected")
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
    Column {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.horizontalScroll(rememberScrollState()),
        ) {
            profiles.forEach { profile ->
                FilterChip(
                    selected = profile.id == activeProfileId,
                    onClick = { onProfileSelected(profile.id) },
                    label = { Text(profile.name) },
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
                style = MaterialTheme.typography.labelMedium,
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
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.horizontalScroll(rememberScrollState()),
    ) {
        options.forEach { range ->
            FilterChip(
                selected = selectedRange == range,
                onClick = { onRangeSelected(range) },
                label = { Text(range.label()) },
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
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.horizontalScroll(rememberScrollState()),
    ) {
        sortOptions.forEach { sortOption ->
            FilterChip(
                selected = selectedOption == sortOption.option,
                onClick = { onSortOptionSelected(sortOption.option) },
                label = { Text(sortOption.label) },
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

    FilterChip(
        selected = true,
        onClick = { showSheet = true },
        label = { Text("Sorted by: $selectedLabel") },
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
    Surface(
        onClick = onClick,
        color =
            if (isSelected) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            },
        shape = MaterialTheme.shapes.small,
        modifier =
            Modifier
                .fillMaxWidth()
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
                    .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Text(
                text = sortOption.label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            )
            if (isSelected) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
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
    if (imageValue.isNullOrBlank()) {
        CategoryDetailItemImagePlaceholder(
            text = "No image",
            contentDescription = "${item.id} image placeholder",
        )
        return
    }

    val context = LocalContext.current
    val bitmap =
        remember(imageValue) {
            imageBitmapFromValue(context.contentResolver, imageValue)
        }
    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "${item.id} image preview",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
    } else {
        CategoryDetailItemImagePlaceholder(
            text = "Image selected",
            contentDescription = "${item.id} image preview",
        )
    }
}

@Composable
private fun CategoryDetailItemImagePlaceholder(
    text: String,
    contentDescription: String,
) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        modifier =
            Modifier
                .fillMaxSize()
                .semantics { this.contentDescription = contentDescription },
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize(),
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }
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
