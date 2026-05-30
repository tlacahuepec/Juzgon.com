@file:Suppress("FunctionName")

package com.juzgon.feature.item

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

private const val MAX_AUTOCOMPLETE_RESULTS = 10

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NationalityAutocompleteField(
    attributeId: String,
    valueText: String,
    onValueChange: (String, String) -> Unit,
    isError: Boolean,
    errorText: String?,
) {
    val state = remember(valueText) { NationalityAutocompleteState(valueText) }

    // Keep the state in sync if the external value changes
    LaunchedEffect(valueText) {
        state.updateFromExternalValue(valueText)
    }

    val cd = "$attributeId value"
    ExposedDropdownMenuBox(
        expanded = state.expanded,
        onExpandedChange = { state.onExpandedChange(it) },
    ) {
        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = { text ->
                state.onSearchQueryChange(text) { /* no-op, handled on selection */ }
            },
            label = { Text(attributeId) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(state.expanded) },
            isError = isError,
            supportingText = { errorText?.let { Text(it) } },
            singleLine = true,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryEditable)
                    .semantics { contentDescription = cd },
        )
        ExposedDropdownMenu(
            expanded = state.expanded && state.suggestions.isNotEmpty(),
            onDismissRequest = { state.onExpandedChange(false) },
        ) {
            state.suggestions.forEach { option ->
                DropdownMenuItem(
                    text = { Text("${option.flagEmoji} ${option.nationality} (${option.country})") },
                    onClick = {
                        state.onOptionSelected(option, attributeId, onValueChange)
                    },
                )
            }
        }
    }
}
