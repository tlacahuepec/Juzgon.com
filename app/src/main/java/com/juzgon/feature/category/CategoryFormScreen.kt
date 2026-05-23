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
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.juzgon.domain.AttributeType
import com.juzgon.domain.ScoringDirection

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
        onAttributeTypeChange = viewModel::onAttributeTypeChanged,
        onAttributeRequiredChange = viewModel::onAttributeRequiredChanged,
        onAttributeDisplayInDiamondChange = viewModel::onAttributeDisplayInDiamondChanged,
        onAttributeDiamondOrderChange = viewModel::onAttributeDiamondOrderChanged,
        onAttributeScoringDirectionChange = viewModel::onAttributeScoringDirectionChanged,
        onAddAttribute = viewModel::addAttribute,
        onRemoveAttribute = viewModel::removeAttribute,
        onMoveAttributeUp = viewModel::moveAttributeUp,
        onMoveAttributeDown = viewModel::moveAttributeDown,
        onTypeChangeConfirmed = viewModel::onTypeChangeConfirmed,
        onTypeChangeDeclined = viewModel::onTypeChangeDeclined,
        onAttributeDeleteConfirmed = viewModel::onAttributeDeleteConfirmed,
        onAttributeDeleteDeclined = viewModel::onAttributeDeleteDeclined,
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
    onAttributeTypeChange: (Long, AttributeType) -> Unit,
    onAttributeRequiredChange: (Long, Boolean) -> Unit,
    onAttributeDisplayInDiamondChange: (Long, Boolean) -> Unit,
    onAttributeDiamondOrderChange: (Long, String) -> Unit,
    onAttributeScoringDirectionChange: (Long, ScoringDirection?) -> Unit,
    onAddAttribute: () -> Unit,
    onRemoveAttribute: (Long) -> Unit,
    onMoveAttributeUp: (Long) -> Unit,
    onMoveAttributeDown: (Long) -> Unit,
    onTypeChangeConfirmed: () -> Unit,
    onTypeChangeDeclined: () -> Unit,
    onAttributeDeleteConfirmed: () -> Unit,
    onAttributeDeleteDeclined: () -> Unit,
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

    if (state.showTypeChangeWarning) {
        AlertDialog(
            onDismissRequest = onTypeChangeDeclined,
            title = { Text("Change attribute type") },
            text = {
                Text("Changing the attribute type may affect existing data. Continue?")
            },
            confirmButton = {
                TextButton(
                    onClick = onTypeChangeConfirmed,
                    modifier = Modifier.semantics { contentDescription = "Confirm type change" },
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = onTypeChangeDeclined,
                    modifier = Modifier.semantics { contentDescription = "Cancel type change" },
                ) {
                    Text("Cancel")
                }
            },
        )
    }

    if (state.showAttributeDeleteWarning) {
        AlertDialog(
            onDismissRequest = onAttributeDeleteDeclined,
            title = { Text("Delete attribute") },
            text = {
                Text("Deleting this attribute will remove its values from all items. Continue?")
            },
            confirmButton = {
                TextButton(
                    onClick = onAttributeDeleteConfirmed,
                    modifier = Modifier.semantics { contentDescription = "Confirm delete attribute" },
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = onAttributeDeleteDeclined,
                    modifier = Modifier.semantics { contentDescription = "Cancel delete attribute" },
                ) {
                    Text("Cancel")
                }
            },
        )
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
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .semantics {
                            contentDescription = "Category name"
                        },
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
                    position = index + 1,
                    validationError = attributeErrors[index],
                    isFirst = index == 0,
                    isLast = index == state.attributes.lastIndex,
                    onNameChange = onAttributeNameChange,
                    onWeightChange = onAttributeWeightChange,
                    onTypeChange = onAttributeTypeChange,
                    onRequiredChange = onAttributeRequiredChange,
                    onDisplayInDiamondChange = onAttributeDisplayInDiamondChange,
                    onDiamondOrderChange = onAttributeDiamondOrderChange,
                    onScoringDirectionChange = onAttributeScoringDirectionChange,
                    onRemove = onRemoveAttribute,
                    onMoveUp = onMoveAttributeUp,
                    onMoveDown = onMoveAttributeDown,
                )
            }

            OutlinedButton(
                onClick = onAddAttribute,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .semantics {
                            contentDescription = "Add attribute"
                            role = Role.Button
                        },
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
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .semantics {
                            contentDescription = "Save category"
                            role = Role.Button
                        },
            ) {
                Text(if (state.isSaving) "Saving" else "Save category")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryAttributeRow(
    attribute: CategoryAttributeInput,
    position: Int,
    validationError: CategoryAttributeValidationError,
    isFirst: Boolean,
    isLast: Boolean,
    onNameChange: (Long, String) -> Unit,
    onWeightChange: (Long, String) -> Unit,
    onTypeChange: (Long, AttributeType) -> Unit,
    onRequiredChange: (Long, Boolean) -> Unit,
    onDisplayInDiamondChange: (Long, Boolean) -> Unit,
    onDiamondOrderChange: (Long, String) -> Unit,
    onScoringDirectionChange: (Long, ScoringDirection?) -> Unit,
    onRemove: (Long) -> Unit,
    onMoveUp: (Long) -> Unit,
    onMoveDown: (Long) -> Unit,
) {
    val actionName = attribute.accessibleActionName(position)
    var typeMenuExpanded by remember { mutableStateOf(false) }

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
            modifier =
                Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = "Attribute $position name"
                    },
        )

        ExposedDropdownMenuBox(
            expanded = typeMenuExpanded,
            onExpandedChange = { typeMenuExpanded = it },
            modifier =
                Modifier.semantics {
                    contentDescription = "Attribute $position type"
                },
        ) {
            OutlinedTextField(
                value =
                    attribute.type.name
                        .lowercase()
                        .replaceFirstChar { it.uppercase() },
                onValueChange = {},
                readOnly = true,
                label = { Text("Type") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeMenuExpanded) },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable),
            )
            ExposedDropdownMenu(
                expanded = typeMenuExpanded,
                onDismissRequest = { typeMenuExpanded = false },
            ) {
                AttributeType.entries.forEach { type ->
                    DropdownMenuItem(
                        text = { Text(type.name.lowercase().replaceFirstChar { it.uppercase() }) },
                        onClick = {
                            onTypeChange(attribute.key, type)
                            typeMenuExpanded = false
                        },
                    )
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Required", style = MaterialTheme.typography.bodyMedium)
            Switch(
                checked = attribute.isRequired,
                onCheckedChange = { onRequiredChange(attribute.key, it) },
                modifier =
                    Modifier.semantics {
                        contentDescription = "Attribute $position required"
                    },
            )
        }

        if (attribute.type == AttributeType.NUMBER) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Diamond chart", style = MaterialTheme.typography.bodyMedium)
                Switch(
                    checked = attribute.displayInDiamond,
                    onCheckedChange = { onDisplayInDiamondChange(attribute.key, it) },
                    modifier =
                        Modifier.semantics {
                            contentDescription = "Attribute $position diamond chart"
                        },
                )
            }

            OutlinedTextField(
                value = attribute.diamondOrderText,
                onValueChange = { onDiamondOrderChange(attribute.key, it) },
                label = { Text("Diamond order") },
                isError = validationError.diamondOrder != null,
                supportingText = {
                    validationError.diamondOrder?.let { Text(it) }
                },
                singleLine = true,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .semantics {
                            contentDescription = "Attribute $position diamond order"
                        },
            )
        }

        if (attribute.type == AttributeType.DATE) {
            ScoringDirectionDropdown(
                position = position,
                scoringDirection = attribute.scoringDirection,
                onDirectionChange = { onScoringDirectionChange(attribute.key, it) },
            )
        }

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
            modifier =
                Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = "Attribute $position weight"
                    },
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { onMoveUp(attribute.key) },
                enabled = !isFirst,
                modifier =
                    Modifier
                        .semantics {
                            contentDescription = "Move $actionName up"
                            role = Role.Button
                        }.sizeIn(minWidth = 48.dp, minHeight = 48.dp),
            ) {
                Text("Move up")
            }
            OutlinedButton(
                onClick = { onMoveDown(attribute.key) },
                enabled = !isLast,
                modifier =
                    Modifier
                        .semantics {
                            contentDescription = "Move $actionName down"
                            role = Role.Button
                        }.sizeIn(minWidth = 48.dp, minHeight = 48.dp),
            ) {
                Text("Move down")
            }
            OutlinedButton(
                onClick = { onRemove(attribute.key) },
                modifier =
                    Modifier
                        .semantics {
                            contentDescription = "Remove $actionName"
                            role = Role.Button
                        }.sizeIn(minWidth = 48.dp, minHeight = 48.dp),
            ) {
                Text("Remove")
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
    }
}

private fun CategoryAttributeInput.accessibleActionName(position: Int): String =
    name
        .takeIf { it.isNotBlank() }
        ?: "attribute $position"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScoringDirectionDropdown(
    position: Int,
    scoringDirection: ScoringDirection?,
    onDirectionChange: (ScoringDirection?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val displayText =
        when (scoringDirection) {
            ScoringDirection.NEWER_IS_BETTER -> "Newer is better"
            ScoringDirection.OLDER_IS_BETTER -> "Older is better"
            null -> "None (display only)"
        }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier =
            Modifier.semantics {
                contentDescription = "Attribute $position scoring direction"
            },
    ) {
        OutlinedTextField(
            value = displayText,
            onValueChange = {},
            readOnly = true,
            label = { Text("Scoring direction") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            DropdownMenuItem(
                text = { Text("None (display only)") },
                onClick = {
                    onDirectionChange(null)
                    expanded = false
                },
            )
            DropdownMenuItem(
                text = { Text("Newer is better") },
                onClick = {
                    onDirectionChange(ScoringDirection.NEWER_IS_BETTER)
                    expanded = false
                },
            )
            DropdownMenuItem(
                text = { Text("Older is better") },
                onClick = {
                    onDirectionChange(ScoringDirection.OLDER_IS_BETTER)
                    expanded = false
                },
            )
        }
    }
}
