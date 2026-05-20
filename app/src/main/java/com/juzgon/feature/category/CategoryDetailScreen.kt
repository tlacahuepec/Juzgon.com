@file:Suppress("FunctionName")

package com.juzgon.feature.category

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

@Composable
fun CategoryDetailRoute(
    categoryName: String,
    onBackClick: () -> Unit,
    viewModel: CategoryDetailViewModel = hiltViewModel(),
) {
    LaunchedEffect(categoryName) {
        viewModel.loadCategory(categoryName)
    }

    val state by viewModel.state.collectAsState()
    CategoryDetailScreen(
        state = state,
        onBackClick = onBackClick,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryDetailScreen(
    state: CategoryDetailUiState,
    onBackClick: () -> Unit,
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
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
            )
        },
        modifier = modifier,
    ) { innerPadding ->
        CategoryDetailContent(
            state = state,
            modifier = Modifier.padding(innerPadding),
        )
    }
}

@Composable
private fun CategoryDetailContent(
    state: CategoryDetailUiState,
    modifier: Modifier = Modifier,
) {
    when {
        state.isLoading -> CenteredContent(modifier = modifier) { CircularProgressIndicator() }
        state.errorMessage != null ->
            CenteredContent(modifier = modifier) {
                Text(
                    text = state.errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        !state.hasItems ->
            CenteredContent(modifier = modifier) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (state.attributeSummary.isNotBlank()) {
                        Text(
                            text = state.attributeSummary,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    Text(
                        text = "No items yet",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        else ->
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
                items(
                    items = state.items,
                    key = { item -> item.id },
                ) { item ->
                    CategoryDetailItemRow(item = item)
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
private fun CategoryDetailItemRow(item: CategoryDetailItemUiModel) {
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
            Modifier.semantics(mergeDescendants = true) {
                contentDescription = "Rated item ${item.id}, average score ${item.averageScoreText}"
            },
    )
}
