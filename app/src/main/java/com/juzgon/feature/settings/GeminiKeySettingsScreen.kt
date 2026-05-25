@file:Suppress("FunctionName", "LongMethod", "LongParameterList")

package com.juzgon.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

@Composable
fun GeminiKeySettingsRoute(
    onBackClick: () -> Unit,
    viewModel: GeminiKeySettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    GeminiKeySettingsScreen(
        state = state,
        onKeyInputChanged = viewModel::onKeyInputChanged,
        onSaveKey = viewModel::onSaveKey,
        onDeleteKey = viewModel::onDeleteKey,
        onReplaceKey = viewModel::onReplaceKey,
        onAddKey = viewModel::onAddKey,
        onBackClick = onBackClick,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeminiKeySettingsScreen(
    state: GeminiKeySettingsUiState,
    onKeyInputChanged: (String) -> Unit,
    onSaveKey: () -> Unit,
    onDeleteKey: () -> Unit,
    onReplaceKey: () -> Unit,
    onAddKey: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Settings") },
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
                    .padding(24.dp),
        ) {
            Text(
                text = "Gemini API Key",
                style = MaterialTheme.typography.titleMedium,
            )

            when (state.keyState) {
                GeminiKeyState.NO_KEY -> NoKeyContent(onAddKey = onAddKey)
                GeminiKeyState.ENTERING ->
                    EnteringKeyContent(
                        inputKey = state.inputKey,
                        errorMessage = state.errorMessage,
                        onKeyInputChanged = onKeyInputChanged,
                        onSaveKey = onSaveKey,
                    )
                GeminiKeyState.CONFIGURED ->
                    ConfiguredKeyContent(
                        maskedKey = state.maskedKey.orEmpty(),
                        onReplaceKey = onReplaceKey,
                        onDeleteKey = onDeleteKey,
                    )
            }
        }
    }
}

@Composable
private fun NoKeyContent(onAddKey: () -> Unit) {
    Text(
        text = "Add your Gemini API key to enable AI suggestions.",
        style = MaterialTheme.typography.bodyMedium,
    )
    Spacer(modifier = Modifier.height(8.dp))
    Button(
        onClick = onAddKey,
        modifier =
            Modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = "Add key"
                    role = Role.Button
                },
    ) {
        Text("Add Key")
    }
}

@Composable
private fun EnteringKeyContent(
    inputKey: String,
    errorMessage: String?,
    onKeyInputChanged: (String) -> Unit,
    onSaveKey: () -> Unit,
) {
    OutlinedTextField(
        value = inputKey,
        onValueChange = onKeyInputChanged,
        label = { Text("Gemini API key") },
        visualTransformation = PasswordVisualTransformation(),
        isError = errorMessage != null,
        supportingText = { errorMessage?.let { Text(it) } },
        singleLine = true,
        modifier =
            Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "API key input" },
    )
    Button(
        onClick = onSaveKey,
        modifier =
            Modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = "Save key"
                    role = Role.Button
                },
    ) {
        Text("Save Key")
    }
}

@Composable
private fun ConfiguredKeyContent(
    maskedKey: String,
    onReplaceKey: () -> Unit,
    onDeleteKey: () -> Unit,
) {
    Text(
        text = "Gemini key configured.",
        style = MaterialTheme.typography.bodyMedium,
    )
    Text(
        text = maskedKey,
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.semantics { contentDescription = "Masked API key" },
    )
    Spacer(modifier = Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
            onClick = onReplaceKey,
            modifier =
                Modifier.semantics {
                    contentDescription = "Replace key"
                    role = Role.Button
                },
        ) {
            Text("Replace")
        }
        OutlinedButton(
            onClick = onDeleteKey,
            modifier =
                Modifier.semantics {
                    contentDescription = "Delete key"
                    role = Role.Button
                },
        ) {
            Text("Delete")
        }
    }
}
