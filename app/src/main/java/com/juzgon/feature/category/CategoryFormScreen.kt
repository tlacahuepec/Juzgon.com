@file:Suppress("FunctionName", "LongMethod", "LongParameterList")

package com.juzgon.feature.category

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

@Composable
fun CategoryFormRoute(
    categoryName: String? = null,
    onBackClick: () -> Unit,
    onSaveCompleted: () -> Unit,
    viewModel: CategoryFormViewModel = hiltViewModel(),
) {
    LaunchedEffect(categoryName) {
        if (categoryName != null) {
            viewModel.loadCategory(categoryName)
        }
    }

    val state by viewModel.state.collectAsState()
    LaunchedEffect(state.saveCompleted) {
        if (state.saveCompleted) {
            onSaveCompleted()
        }
    }

    CategoryFormScreen(
        state = state,
        onNameChange = viewModel::onNameChanged,
        onAttributeNameChange = viewModel::onAttributeNameChanged,
        onAttributeWeightChange = viewModel::onAttributeWeightChanged,
        onAddAttribute = viewModel::addAttribute,
        onRemoveAttribute = viewModel::removeAttribute,
        onMoveAttributeUp = viewModel::moveAttributeUp,
        onMoveAttributeDown = viewModel::moveAttributeDown,
        onSaveClick = viewModel::onSaveClick,
        onBackClick = onBackClick,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryFormScreen(
    state: CategoryFormUiState,
    onNameChange: (String) -> Unit,
    onAttributeNameChange: (Long, String) -> Unit,
    onAttributeWeightChange: (Long, String) -> Unit,
    onAddAttribute: () -> Unit,
    onRemoveAttribute: (Long) -> Unit,
    onMoveAttributeUp: (Long) -> Unit,
    onMoveAttributeDown: (Long) -> Unit,
    onSaveClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val nameError = if (state.showValidationErrors) state.nameError else null
    val formError = if (state.showValidationErrors) state.formError else null
    val attributeErrors =
        if (state.showValidationErrors) {
            state.attributeErrors
        } else {
            List(state.attributes.size) { CategoryAttributeValidationError() }
        }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (state.mode == CategoryFormMode.Edit) {
                            "Edit category"
                        } else {
                            "Create category"
                        },
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
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
        ) {
            OutlinedTextField(
                value = state.name,
                onValueChange = onNameChange,
                label = { Text("Category name") },
                isError = nameError != null,
                supportingText = {
                    nameError?.let { error ->
                        Text(error)
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Text(
                text = "Attributes",
                style = MaterialTheme.typography.titleMedium,
            )

            formError?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            state.attributes.forEachIndexed { index, attribute ->
                CategoryAttributeRow(
                    attribute = attribute,
                    validationError = attributeErrors[index],
                    isFirst = index == 0,
                    isLast = index == state.attributes.lastIndex,
                    onNameChange = onAttributeNameChange,
                    onWeightChange = onAttributeWeightChange,
                    onRemove = onRemoveAttribute,
                    onMoveUp = onMoveAttributeUp,
                    onMoveDown = onMoveAttributeDown,
                )
            }

            OutlinedButton(
                onClick = onAddAttribute,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Add attribute")
            }

            if (state.errorMessage != null) {
                Text(
                    text = state.errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            Button(
                onClick = onSaveClick,
                enabled = state.saveEnabled,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (state.isSaving) "Saving" else "Save category")
            }
        }
    }
}

@Composable
private fun CategoryAttributeRow(
    attribute: CategoryAttributeInput,
    validationError: CategoryAttributeValidationError,
    isFirst: Boolean,
    isLast: Boolean,
    onNameChange: (Long, String) -> Unit,
    onWeightChange: (Long, String) -> Unit,
    onRemove: (Long) -> Unit,
    onMoveUp: (Long) -> Unit,
    onMoveDown: (Long) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = attribute.name,
            onValueChange = { onNameChange(attribute.key, it) },
            label = { Text("Attribute name") },
            isError = validationError.name != null,
            supportingText = {
                if (validationError.name != null) {
                    Text(validationError.name)
                }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = attribute.weightText,
            onValueChange = { onWeightChange(attribute.key, it) },
            label = { Text("Weight") },
            isError = validationError.weight != null,
            supportingText = {
                if (validationError.weight != null) {
                    Text(validationError.weight)
                }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { onMoveUp(attribute.key) },
                enabled = !isFirst,
            ) {
                Text("Move up")
            }
            OutlinedButton(
                onClick = { onMoveDown(attribute.key) },
                enabled = !isLast,
            ) {
                Text("Move down")
            }
            OutlinedButton(onClick = { onRemove(attribute.key) }) {
                Text("Remove")
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
    }
}
