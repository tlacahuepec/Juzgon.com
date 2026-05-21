@file:Suppress("FunctionName", "LongMethod", "LongParameterList")

package com.juzgon.feature.category

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

@Composable
fun CategoryDetailRoute(
    categoryName: String,
    onBackClick: () -> Unit,
    onAddItemClick: () -> Unit,
    onEditItemClick: (String) -> Unit,
    viewModel: CategoryDetailViewModel = hiltViewModel(),
) {
    LaunchedEffect(categoryName) {
        viewModel.loadCategory(categoryName)
    }

    val state by viewModel.state.collectAsState()
    CategoryDetailScreen(
        state = state,
        onBackClick = onBackClick,
        onRetry = viewModel::onRetry,
        onSortOptionSelected = viewModel::onSortOptionSelected,
        onAddItemClick = onAddItemClick,
        onEditItemClick = onEditItemClick,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryDetailScreen(
    state: CategoryDetailUiState,
    onBackClick: () -> Unit,
    onRetry: () -> Unit,
    onSortOptionSelected: (CategoryDetailSortOption) -> Unit,
    onAddItemClick: () -> Unit,
    onEditItemClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
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
            onAddItemClick = onAddItemClick,
            onEditItemClick = onEditItemClick,
            modifier = Modifier.padding(innerPadding),
        )
    }
}

@Composable
private fun CategoryDetailContent(
    state: CategoryDetailUiState,
    onRetry: () -> Unit,
    onSortOptionSelected: (CategoryDetailSortOption) -> Unit,
    onAddItemClick: () -> Unit,
    onEditItemClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        state.isLoading -> CenteredContent(modifier = modifier) { CircularProgressIndicator() }
        state.errorMessage != null -> CategoryDetailErrorState(state.errorMessage, onRetry, modifier)
        !state.hasItems -> CategoryDetailEmptyState(state.attributeSummary, onAddItemClick, modifier)
        else -> CategoryDetailItemList(state, onSortOptionSelected, onEditItemClick, modifier)
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
    onEditItemClick: (String) -> Unit,
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
        item {
            CategoryDetailSortControls(
                selectedOption = state.sortOption,
                onSortOptionSelected = onSortOptionSelected,
            )
        }
        items(
            items = state.items,
            key = { item -> item.id },
        ) { item ->
            CategoryDetailItemRow(
                item = item,
                onEditItemClick = onEditItemClick,
            )
        }
    }
}

@Composable
private fun CategoryDetailSortControls(
    selectedOption: CategoryDetailSortOption,
    onSortOptionSelected: (CategoryDetailSortOption) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FilterChip(
            selected = selectedOption == CategoryDetailSortOption.Score,
            onClick = { onSortOptionSelected(CategoryDetailSortOption.Score) },
            label = { Text("Score") },
            modifier =
                Modifier
                    .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                    .semantics {
                        contentDescription = "Sort items by score"
                    },
        )
        FilterChip(
            selected = selectedOption == CategoryDetailSortOption.Name,
            onClick = { onSortOptionSelected(CategoryDetailSortOption.Name) },
            label = { Text("Name") },
            modifier =
                Modifier
                    .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                    .semantics {
                        contentDescription = "Sort items by name"
                    },
        )
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
private fun CategoryDetailItemRow(
    item: CategoryDetailItemUiModel,
    onEditItemClick: (String) -> Unit,
) {
    ListItem(
        headlineContent = { Text(item.id) },
        trailingContent = {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = MaterialTheme.shapes.small,
            ) {
                Text(
                    text = item.averageScoreText,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }
        },
        modifier =
            Modifier
                .clickable { onEditItemClick(item.id) }
                .semantics(mergeDescendants = true) {
                    contentDescription = "Rated item ${item.id}, average score ${item.averageScoreText}"
                    role = Role.Button
                },
    )
}
