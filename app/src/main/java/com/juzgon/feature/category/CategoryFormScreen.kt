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
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

@Composable
fun CategoryFormRoute(
    categoryName: String? = null,
    viewModel: CategoryFormViewModel = hiltViewModel(),
) {
    LaunchedEffect(categoryName) {
        if (categoryName != null) {
            viewModel.loadCategory(categoryName)
        }
    }

    val state by viewModel.state.collectAsState()
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
    )
}

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
            text = if (state.mode == CategoryFormMode.Edit) "Edit category" else "Create category",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
        )

        OutlinedTextField(
            value = state.name,
            onValueChange = onNameChange,
            label = { Text("Category name") },
            isError = state.nameError != null,
            supportingText = {
                state.nameError?.let { nameError ->
                    Text(nameError)
                }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Text(
            text = "Attributes",
            style = MaterialTheme.typography.titleMedium,
        )

        state.formError?.let { formError ->
            Text(
                text = formError,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        state.attributes.forEachIndexed { index, attribute ->
            CategoryAttributeRow(
                attribute = attribute,
                validationError = state.attributeErrors[index],
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
