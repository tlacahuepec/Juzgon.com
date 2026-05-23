@file:Suppress("FunctionName", "LongMethod", "LongParameterList")

package com.juzgon.feature.scoreprofile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

@Composable
fun ScoreProfileListRoute(
    categoryName: String,
    onBackClick: () -> Unit,
    onCreateClick: () -> Unit,
    onEditClick: (String) -> Unit,
    viewModel: ScoreProfileListViewModel = hiltViewModel(),
) {
    LaunchedEffect(categoryName) {
        viewModel.load(categoryName)
    }

    val state by viewModel.state.collectAsState()

    ScoreProfileListScreen(
        state = state,
        onBackClick = onBackClick,
        onCreateClick = onCreateClick,
        onEditClick = onEditClick,
        onDeleteRequest = viewModel::onDeleteRequest,
        onDeleteConfirmed = viewModel::onDeleteConfirmed,
        onDeleteDismissed = viewModel::onDeleteDismissed,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScoreProfileListScreen(
    state: ScoreProfileListUiState,
    onBackClick: () -> Unit,
    onCreateClick: () -> Unit,
    onEditClick: (String) -> Unit,
    onDeleteRequest: (String) -> Unit,
    onDeleteConfirmed: () -> Unit,
    onDeleteDismissed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.showDeleteDialog) {
        AlertDialog(
            onDismissRequest = onDeleteDismissed,
            title = { Text("Delete score profile?") },
            text = { Text("This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = onDeleteConfirmed,
                    modifier = Modifier.semantics { contentDescription = "Confirm delete" },
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = onDeleteDismissed,
                    modifier = Modifier.semantics { contentDescription = "Cancel delete" },
                ) {
                    Text("Cancel")
                }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Score profiles") },
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
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateClick,
                modifier =
                    Modifier.semantics {
                        contentDescription = "Create score profile"
                        role = Role.Button
                    },
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
            }
        },
        modifier = modifier,
    ) { innerPadding ->
        if (!state.isLoading && state.profiles.isEmpty()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize().padding(innerPadding),
            ) {
                Text(
                    text = "No score profiles yet",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                items(state.profiles, key = { it.id }) { profile ->
                    ProfileCard(
                        profile = profile,
                        onEditClick = { onEditClick(profile.id) },
                        onDeleteClick = { onDeleteRequest(profile.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileCard(
    profile: ScoreProfileSummary,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = profile.name,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = formatAttributeCount(profile.attributeCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(
                onClick = onEditClick,
                modifier =
                    Modifier
                        .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                        .semantics {
                            contentDescription = "Edit ${profile.name}"
                            role = Role.Button
                        },
            ) {
                Icon(Icons.Filled.Edit, contentDescription = null)
            }
            IconButton(
                onClick = onDeleteClick,
                modifier =
                    Modifier
                        .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                        .semantics {
                            contentDescription = "Delete ${profile.name}"
                            role = Role.Button
                        },
            ) {
                Icon(Icons.Filled.Delete, contentDescription = null)
            }
        }
    }
}

private fun formatAttributeCount(count: Int): String = if (count == 1) "1 attribute" else "$count attributes"
