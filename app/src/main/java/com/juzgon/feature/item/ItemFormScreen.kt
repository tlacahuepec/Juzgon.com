@file:Suppress("FunctionName", "LongMethod", "LongParameterList")

package com.juzgon.feature.item

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

@Composable
fun ItemFormRoute(
    categoryName: String,
    itemId: String? = null,
    onBackClick: () -> Unit,
    onSaveCompleted: () -> Unit,
    onDeleteCompleted: () -> Unit = {},
    viewModel: ItemFormViewModel = hiltViewModel(),
) {
    LaunchedEffect(categoryName, itemId) {
        viewModel.loadCategory(categoryName, itemId)
    }

    val state by viewModel.state.collectAsState()
    LaunchedEffect(state.saveCompleted) {
        if (state.saveCompleted) {
            onSaveCompleted()
        }
    }
    LaunchedEffect(state.deleteCompleted) {
        if (state.deleteCompleted) {
            onDeleteCompleted()
        }
    }

    ItemFormScreen(
        state = state,
        onTitleChange = viewModel::onTitleChanged,
        onNotesChange = viewModel::onNotesChanged,
        onScoreChange = viewModel::onScoreChanged,
        onSaveClick = viewModel::onSaveClick,
        onBackClick = onBackClick,
        onDeleteClick = viewModel::onDeleteClick,
        onDeleteCancel = viewModel::onDeleteCancel,
        onDeleteConfirm = viewModel::onDeleteConfirm,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemFormScreen(
    state: ItemFormUiState,
    onTitleChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onScoreChange: (String, String) -> Unit,
    onSaveClick: () -> Unit,
    onBackClick: () -> Unit,
    onDeleteClick: () -> Unit = {},
    onDeleteCancel: () -> Unit = {},
    onDeleteConfirm: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val titleError = if (state.showValidationErrors) state.titleError else null
    val scoreErrors =
        if (state.showValidationErrors) {
            state.scoreErrors
        } else {
            List(state.scores.size) { ItemScoreValidationError() }
        }

    if (state.showDeleteDialog) {
        AlertDialog(
            onDismissRequest = onDeleteCancel,
            title = { Text("Delete item?") },
            text = { Text("This will permanently delete the item and all its ratings.") },
            confirmButton = {
                TextButton(onClick = onDeleteConfirm) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = onDeleteCancel) { Text("Cancel") }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.mode == ItemFormMode.Edit) "Edit item" else "Add item") },
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
                    if (state.mode == ItemFormMode.Edit) {
                        IconButton(
                            onClick = onDeleteClick,
                            modifier =
                                Modifier
                                    .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                                    .semantics {
                                        contentDescription = "Delete item"
                                        role = Role.Button
                                    },
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = null,
                            )
                        }
                    }
                },
            )
        },
        modifier = modifier,
    ) { innerPadding ->
        when {
            state.isLoading ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                ) {
                    CircularProgressIndicator()
                }
            else ->
                ItemFormContent(
                    state = state,
                    titleError = titleError,
                    scoreErrors = scoreErrors,
                    onTitleChange = onTitleChange,
                    onNotesChange = onNotesChange,
                    onScoreChange = onScoreChange,
                    onSaveClick = onSaveClick,
                    modifier = Modifier.padding(innerPadding),
                )
        }
    }
}

@Composable
private fun ItemFormContent(
    state: ItemFormUiState,
    titleError: String?,
    scoreErrors: List<ItemScoreValidationError>,
    onTitleChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onScoreChange: (String, String) -> Unit,
    onSaveClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
    ) {
        Text(
            text = state.categoryName,
            style = MaterialTheme.typography.titleMedium,
        )

        OutlinedTextField(
            value = state.title,
            onValueChange = onTitleChange,
            enabled = state.titleEditable,
            label = { Text("Item title") },
            isError = titleError != null,
            supportingText = {
                titleError?.let { Text(it) }
            },
            singleLine = true,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = "Item title"
                    },
        )

        OutlinedTextField(
            value = state.notes,
            onValueChange = onNotesChange,
            label = { Text("Notes") },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = "Item notes"
                    },
        )

        state.scores.forEachIndexed { index, scoreInput ->
            ItemScoreField(
                scoreInput = scoreInput,
                validationError = scoreErrors[index],
                onScoreChange = onScoreChange,
            )
        }

        state.errorMessage?.let { error ->
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        Button(
            onClick = onSaveClick,
            enabled = state.saveEnabled,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = "Save item"
                        role = Role.Button
                    },
        ) {
            Text(if (state.isSaving) "Saving" else "Save item")
        }
    }
}

@Composable
private fun ItemScoreField(
    scoreInput: ItemScoreInput,
    validationError: ItemScoreValidationError,
    onScoreChange: (String, String) -> Unit,
) {
    val label = "${scoreInput.attribute.id} score"
    OutlinedTextField(
        value = scoreInput.scoreText,
        onValueChange = { onScoreChange(scoreInput.attribute.id, it) },
        label = { Text(label) },
        isError = validationError.score != null,
        supportingText = {
            validationError.score?.let { Text(it) }
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier =
            Modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = label
                },
    )
}
