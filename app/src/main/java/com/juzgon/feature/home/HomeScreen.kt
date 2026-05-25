@file:Suppress("FunctionName", "TooManyFunctions", "LongParameterList")

package com.juzgon.feature.home

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
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
import androidx.compose.ui.platform.LocalContext
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
        Spacer(modifier = Modifier.height(16.dp))
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
        Spacer(modifier = Modifier.height(16.dp))
        HomeCategoryContent(
            state = state,
            onCreateCategoryClick = actions.onCreateCategoryClick,
            onCategoryClick = actions.onCategoryClick,
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
            text = "Categories",
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
            modifier =
                Modifier
                    .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                    .semantics {
                        contentDescription = "Sort categories by recent"
                    },
        )
        FilterChip(
            selected = selectedOption == HomeSortOption.Name,
            onClick = { onSortOptionSelected(HomeSortOption.Name) },
            label = { Text("Name") },
            modifier =
                Modifier
                    .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                    .semantics {
                        contentDescription = "Sort categories by name"
                    },
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
    val summary = buildCategorySummary(category.itemCount, category.attributeCount)

    ListItem(
        headlineContent = { Text(category.name) },
        supportingContent = {
            Text(
                text = summary,
            )
        },
        modifier =
            Modifier
                .clickable(
                    onClickLabel = "Open category ${category.name}",
                    role = Role.Button,
                ) {
                    onCategoryClick(category.name)
                }.semantics(mergeDescendants = true) {
                    contentDescription = "Open category ${category.name}, $summary"
                    role = Role.Button
                },
    )
}

private fun buildCategorySummary(
    itemCount: Int,
    attributeCount: Int,
): String {
    val items = if (itemCount == 1) "1 item" else "$itemCount items"
    val attrs = if (attributeCount == 1) "1 attribute" else "$attributeCount attributes"
    return "$items · $attrs"
}
