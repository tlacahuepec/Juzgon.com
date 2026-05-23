@file:Suppress("FunctionName", "LongMethod", "LongParameterList")

package com.juzgon.feature.scoreprofile

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
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.juzgon.domain.AttributeType
import com.juzgon.domain.ScoringDirection

@Composable
fun ScoreProfileFormRoute(
    categoryName: String,
    profileId: String? = null,
    onBackClick: () -> Unit,
    onSaveCompleted: () -> Unit,
    viewModel: ScoreProfileFormViewModel = hiltViewModel(),
) {
    LaunchedEffect(categoryName, profileId) {
        viewModel.load(categoryName, profileId)
    }

    val state by viewModel.state.collectAsState()
    LaunchedEffect(state.saveCompleted) {
        if (state.saveCompleted) {
            onSaveCompleted()
        }
    }

    ScoreProfileFormScreen(
        state = state,
        onNameChange = viewModel::onNameChanged,
        onAttributeToggled = viewModel::onAttributeToggled,
        onSaveClick = viewModel::onSaveClick,
        onBackClick = onBackClick,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScoreProfileFormScreen(
    state: ScoreProfileFormUiState,
    onNameChange: (String) -> Unit,
    onAttributeToggled: (String, Boolean) -> Unit,
    onSaveClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val nameError = if (state.showValidationErrors) state.nameError else null
    val selectionError = if (state.showValidationErrors) state.selectionError else null

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.screenTitle) },
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
                value = state.profileName,
                onValueChange = onNameChange,
                label = { Text("Profile name") },
                isError = nameError != null,
                supportingText = nameError?.let { { Text(it) } },
                singleLine = true,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = "Profile name" },
            )

            if (state.attributes.isNotEmpty()) {
                Text(
                    text = "${state.selectedCount} of ${state.attributes.size} attributes selected",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            selectionError?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            state.attributes.forEach { attribute ->
                AttributeCheckboxRow(
                    attribute = attribute,
                    onToggle = { onAttributeToggled(attribute.id, !attribute.isSelected) },
                )
            }

            state.errorMessage?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onSaveClick,
                enabled = state.saveEnabled,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .sizeIn(minHeight = 48.dp)
                        .semantics { contentDescription = "Save" },
            ) {
                Text(if (state.isSaving) "Saving\u2026" else "Save")
            }
        }
    }
}

@Composable
private fun AttributeCheckboxRow(
    attribute: RankableAttributeCheckbox,
    onToggle: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier
                .fillMaxWidth()
                .sizeIn(minHeight = 48.dp),
    ) {
        Checkbox(
            checked = attribute.isSelected,
            onCheckedChange = { onToggle() },
            modifier =
                Modifier
                    .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                    .semantics {
                        contentDescription = "Toggle ${attribute.name}"
                        role = Role.Checkbox
                        toggleableState =
                            if (attribute.isSelected) {
                                ToggleableState.On
                            } else {
                                ToggleableState.Off
                            }
                    },
        )
        Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
            Text(
                text = attribute.name,
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = formatAttributeType(attribute.type, attribute.scoringDirection),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun formatAttributeType(
    type: AttributeType,
    scoringDirection: ScoringDirection?,
): String {
    val typeName =
        when (type) {
            AttributeType.NUMBER -> "Number"
            AttributeType.DATE -> "Date"
            AttributeType.IMAGE -> "Image"
            AttributeType.NATIONALITY -> "Nationality"
            AttributeType.BOOLEAN -> "Boolean"
            AttributeType.DROPDOWN -> "Dropdown"
            AttributeType.URL -> "URL"
            AttributeType.NOTES -> "Notes"
        }
    val directionSuffix =
        when (scoringDirection) {
            ScoringDirection.NEWER_IS_BETTER -> " \u00b7 Newer is better"
            ScoringDirection.OLDER_IS_BETTER -> " \u00b7 Older is better"
            null -> ""
        }
    return "$typeName$directionSuffix"
}
