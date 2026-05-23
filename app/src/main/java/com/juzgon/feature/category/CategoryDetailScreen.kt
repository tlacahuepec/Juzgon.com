@file:Suppress("FunctionName", "LongMethod", "LongParameterList", "TooManyFunctions")

package com.juzgon.feature.category

import android.content.ContentResolver
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

@Composable
fun CategoryDetailRoute(
    categoryName: String,
    onBackClick: () -> Unit,
    onAddItemClick: () -> Unit,
    onEditItemClick: (String) -> Unit,
    onEditCategoryClick: () -> Unit,
    onDeleteCategoryComplete: () -> Unit,
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
        onAddItemClick = onAddItemClick,
        onEditItemClick = onEditItemClick,
        onDeleteClick = viewModel::onDeleteClick,
        onDeleteConfirmed = viewModel::onDeleteConfirmed,
        onDeleteDialogDismissed = viewModel::onDeleteDialogDismissed,
        onEditCategoryClick = viewModel::onEditCategoryClick,
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
    onDeleteClick: () -> Unit = {},
    onDeleteConfirmed: () -> Unit = {},
    onDeleteDialogDismissed: () -> Unit = {},
    onEditCategoryClick: () -> Unit = {},
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
            onAddItemClick = onAddItemClick,
            onEditItemClick = onEditItemClick,
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
                sortOptions = state.sortOptions,
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
    sortOptions: List<CategoryDetailSortOptionUiModel>,
    onSortOptionSelected: (CategoryDetailSortOption) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
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
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        shape = MaterialTheme.shapes.small,
        modifier =
            Modifier
                .fillMaxWidth()
                .height(172.dp)
                .clickable(
                    role = Role.Button,
                    onClick = { onEditItemClick(item.id) },
                ).semantics {
                    contentDescription =
                        "Rated item ${item.id}, rank ${item.rank}, ${item.metricLabel} ${item.metricValueText}"
                    role = Role.Button
                },
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            CategoryDetailItemVisual(item = item)
            CategoryDetailItemOverlay(
                item = item,
                modifier =
                    Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth(),
            )
        }
    }
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

@Composable
private fun CategoryDetailItemOverlay(
    item: CategoryDetailItemUiModel,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.92f),
        contentColor = MaterialTheme.colorScheme.inverseOnSurface,
        modifier = modifier,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
        ) {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                shape = MaterialTheme.shapes.small,
            ) {
                Text(
                    text = "#${item.rank}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }
            Text(
                text = item.id,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Surface(
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = MaterialTheme.shapes.small,
            ) {
                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                ) {
                    Text(
                        text = item.metricLabel,
                        style = MaterialTheme.typography.labelSmall,
                    )
                    Text(
                        text = item.metricValueText,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
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
