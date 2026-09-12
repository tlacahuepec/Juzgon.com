@file:Suppress("FunctionName", "LongMethod", "LongParameterList", "TooManyFunctions")

package com.juzgon.feature.category

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.juzgon.domain.CatalogType
import com.juzgon.ui.theme.JuzgonVisualTheme

internal const val CATALOGS_GRID_TAG = "Catalogs grid"

@Composable
fun CatalogsRoute(
    onNavigateToCategory: (String) -> Unit,
    onNavigateToCreateCategory: () -> Unit,
    viewModel: CatalogsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(viewModel, onNavigateToCategory, onNavigateToCreateCategory) {
        viewModel.navigationEvents.collect { event ->
            when (event) {
                is CatalogsNavigationEvent.CreateCategory -> onNavigateToCreateCategory()
                is CatalogsNavigationEvent.OpenCategory -> onNavigateToCategory(event.categoryName)
            }
        }
    }

    CatalogsScreen(
        state = state,
        actions =
            CatalogsScreenActions(
                onSearchQueryChange = viewModel::onSearchQueryChanged,
                onFilterSelected = viewModel::onTypeFilterSelected,
                onCreateCategoryClick = viewModel::onCreateCategoryClick,
                onCategoryClick = viewModel::onCategoryClick,
                onRetry = viewModel::onRetry,
            ),
    )
}

@Composable
fun CatalogsScreen(
    state: CatalogsUiState,
    actions: CatalogsScreenActions,
    modifier: Modifier = Modifier,
) {
    val tokens = JuzgonVisualTheme.tokens

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = actions.onCreateCategoryClick,
                containerColor = tokens.palette.primaryGlow,
                contentColor = tokens.palette.textStrong,
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
        CatalogsContent(
            state = state,
            actions = actions,
            modifier = Modifier.padding(innerPadding),
        )
    }
}

@Composable
private fun CatalogsContent(
    state: CatalogsUiState,
    actions: CatalogsScreenActions,
    modifier: Modifier = Modifier,
) {
    when {
        state.isLoading -> {
            Box(
                contentAlignment = Alignment.Center,
                modifier = modifier.fillMaxSize(),
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.semantics { contentDescription = "Loading categories" },
                )
            }
        }
        state.errorMessage != null -> {
            CatalogsErrorState(
                errorMessage = state.errorMessage,
                onRetry = actions.onRetry,
                modifier = modifier,
            )
        }
        else -> {
            CatalogsLoadedState(
                state = state,
                actions = actions,
                modifier = modifier,
            )
        }
    }
}

@Composable
private fun CatalogsErrorState(
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
private fun CatalogsLoadedState(
    state: CatalogsUiState,
    actions: CatalogsScreenActions,
    modifier: Modifier = Modifier,
) {
    val tokens = JuzgonVisualTheme.tokens

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(horizontal = tokens.spacing.large),
        verticalArrangement = Arrangement.spacedBy(tokens.spacing.medium),
    ) {
        Spacer(modifier = Modifier.height(tokens.spacing.small))

        Text(
            text = "My Catalogs",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = tokens.palette.textStrong,
        )

        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = actions.onSearchQueryChange,
            label = { Text("Search categories") },
            placeholder = { Text("Search categories") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = tokens.palette.textMuted,
                )
            },
            singleLine = true,
            colors =
                OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = tokens.palette.primaryGlow,
                    unfocusedBorderColor = tokens.palette.secondaryGlow.copy(alpha = 0.4f),
                    focusedTextColor = tokens.palette.textStrong,
                    unfocusedTextColor = tokens.palette.textStrong,
                    focusedContainerColor = tokens.palette.elevatedBackground,
                    unfocusedContainerColor = tokens.palette.elevatedBackground,
                ),
            shape = RoundedCornerShape(tokens.shapes.pillCornerRadius),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = "Search categories"
                    },
        )

        if (state.filterChips.isNotEmpty()) {
            CatalogTypeFilterChips(
                filterChips = state.filterChips,
                onFilterSelected = actions.onFilterSelected,
            )
        }

        if (state.isEmpty) {
            CatalogsEmptyState(
                hasSearchOrFilter = state.hasSearchQuery || state.selectedType != null,
                onCreateCategoryClick = actions.onCreateCategoryClick,
                modifier = Modifier.weight(1f),
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(tokens.spacing.medium),
                verticalArrangement = Arrangement.spacedBy(tokens.spacing.medium),
                contentPadding = PaddingValues(bottom = tokens.spacing.large),
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .testTag(CATALOGS_GRID_TAG),
            ) {
                items(
                    items = state.categories,
                    key = { it.name },
                ) { category ->
                    CatalogCard(
                        category = category,
                        onClick = { actions.onCategoryClick(category.name) },
                    )
                }
            }
        }
    }
}

@Composable
private fun CatalogTypeFilterChips(
    filterChips: List<CatalogTypeFilterChip>,
    onFilterSelected: (CatalogType?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = JuzgonVisualTheme.tokens

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(tokens.spacing.small),
        contentPadding = PaddingValues(horizontal = 0.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        items(
            items = filterChips,
            key = { it.id },
        ) { chip ->
            FilterChip(
                selected = chip.isSelected,
                onClick = { onFilterSelected(chip.type) },
                label = { Text(chip.label) },
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
                        selected = chip.isSelected,
                        borderColor = if (chip.isSelected) tokens.palette.primaryGlow else Color.Transparent,
                    ),
                shape = RoundedCornerShape(tokens.shapes.pillCornerRadius),
            )
        }
    }
}

@Composable
private fun CatalogsEmptyState(
    hasSearchOrFilter: Boolean,
    onCreateCategoryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.fillMaxSize(),
    ) {
        if (hasSearchOrFilter) {
            Text(
                text = "No categories match your search or filter",
                style = MaterialTheme.typography.bodyLarge,
                color = JuzgonVisualTheme.tokens.palette.textMuted,
            )
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = "No categories yet",
                    style = MaterialTheme.typography.bodyLarge,
                    color = JuzgonVisualTheme.tokens.palette.textMuted,
                )
                Button(
                    onClick = onCreateCategoryClick,
                    modifier =
                        Modifier.semantics {
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
fun CatalogCard(
    category: CatalogCardUiModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = JuzgonVisualTheme.tokens
    val cardShape = RoundedCornerShape(tokens.shapes.cardCornerRadius)
    val cardDescription = "${category.name}, ${category.itemCountText}, ${category.averageRatingText}"

    Surface(
        color = tokens.palette.elevatedBackground,
        contentColor = tokens.palette.textStrong,
        shape = cardShape,
        border =
            BorderStroke(
                width = 1.dp,
                color = tokens.palette.secondaryGlow.copy(alpha = 0.25f),
            ),
        modifier =
            modifier
                .fillMaxWidth()
                .sizeIn(minHeight = 120.dp)
                .clickable(
                    role = Role.Button,
                    onClick = onClick,
                    onClickLabel = "Open category ${category.name}",
                ).semantics(mergeDescendants = true) {
                    contentDescription = cardDescription
                    role = Role.Button
                },
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(tokens.spacing.medium),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier =
                        Modifier
                            .background(
                                color = tokens.palette.baseBackground.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(tokens.shapes.pillCornerRadius),
                            ).padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = category.itemCountText,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = tokens.palette.textMuted,
                    )
                }

                Text(
                    text = "✦",
                    color = tokens.palette.primaryGlow,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Spacer(modifier = Modifier.height(tokens.spacing.small))

            Text(
                text = category.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = tokens.palette.textStrong,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(modifier = Modifier.height(tokens.spacing.small))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Avg Rating",
                    style = MaterialTheme.typography.labelSmall,
                    color = tokens.palette.textMuted,
                )
                Text(
                    text = category.averageRatingText,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = tokens.palette.ratingAccent,
                )
            }
        }
    }
}
