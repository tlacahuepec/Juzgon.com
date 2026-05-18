@file:Suppress("FunctionName")

package com.juzgon.feature.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

@Composable
fun HomeRoute(
    onNavigateToCreateCategory: () -> Unit,
    onNavigateToCategory: (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

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
            FloatingActionButton(onClick = actions.onCreateCategoryClick) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Create category",
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
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(24.dp),
    ) {
        Text(
            text = "Categories",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = actions.onSearchQueryChange,
            label = { Text("Search categories") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(12.dp))
        HomeSortControls(
            selectedOption = state.sortOption,
            onSortOptionSelected = actions.onSortOptionSelected,
        )
        Spacer(modifier = Modifier.height(16.dp))
        HomeCategoryContent(
            state = state,
            onCreateCategoryClick = actions.onCreateCategoryClick,
            onCategoryClick = actions.onCategoryClick,
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
        FilterChip(
            selected = selectedOption == HomeSortOption.Recent,
            onClick = { onSortOptionSelected(HomeSortOption.Recent) },
            label = { Text("Recent") },
        )
        FilterChip(
            selected = selectedOption == HomeSortOption.Name,
            onClick = { onSortOptionSelected(HomeSortOption.Name) },
            label = { Text("Name") },
        )
    }
}

@Composable
private fun HomeCategoryContent(
    state: HomeUiState,
    onCreateCategoryClick: () -> Unit,
    onCategoryClick: (String) -> Unit,
) {
    if (state.isEmpty) {
        HomeEmptyState(
            hasSearchQuery = state.hasSearchQuery,
            onCreateCategoryClick = onCreateCategoryClick,
        )
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize(),
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
        contentAlignment = Alignment.Center,
        modifier = modifier.fillMaxSize(),
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
                Button(onClick = onCreateCategoryClick) {
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
    ListItem(
        headlineContent = { Text(category.name) },
        supportingContent = {
            Text(
                text =
                    if (category.attributeCount == 1) {
                        "1 attribute"
                    } else {
                        "${category.attributeCount} attributes"
                    },
            )
        },
        modifier = Modifier.clickable { onCategoryClick(category.name) },
    )
}
