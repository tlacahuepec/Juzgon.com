@file:Suppress("FunctionName")

package com.juzgon.feature.home

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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

@Composable
fun HomeRoute(viewModel: HomeViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()

    HomeScreen(
        state = state,
        onSearchQueryChange = viewModel::onSearchQueryChanged,
        onSortOptionSelected = viewModel::onSortOptionSelected,
    )
}

@Composable
fun HomeScreen(
    state: HomeUiState,
    onSearchQueryChange: (String) -> Unit,
    onSortOptionSelected: (HomeSortOption) -> Unit,
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
            onValueChange = onSearchQueryChange,
            label = { Text("Search categories") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Sort",
                style = MaterialTheme.typography.labelLarge,
            )
            FilterChip(
                selected = state.sortOption == HomeSortOption.Recent,
                onClick = { onSortOptionSelected(HomeSortOption.Recent) },
                label = { Text("Recent") },
            )
            FilterChip(
                selected = state.sortOption == HomeSortOption.Name,
                onClick = { onSortOptionSelected(HomeSortOption.Name) },
                label = { Text("Name") },
            )
        }
        Spacer(modifier = Modifier.height(16.dp))

        if (state.isEmpty) {
            HomeEmptyState(hasSearchQuery = state.hasSearchQuery)
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(
                    items = state.categories,
                    key = { category -> category.name },
                ) { category ->
                    CategoryRow(category = category)
                }
            }
        }
    }
}

@Composable
private fun HomeEmptyState(
    hasSearchQuery: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.fillMaxSize(),
    ) {
        Text(
            text =
                if (hasSearchQuery) {
                    "No categories match your search"
                } else {
                    "No categories yet"
                },
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun CategoryRow(category: HomeCategoryUiModel) {
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
    )
}
