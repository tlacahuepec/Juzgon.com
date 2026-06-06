@file:Suppress("FunctionName")

package com.juzgon.feature.item

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.juzgon.domain.NationalityDataset

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun NationalityMultiSelectField(
    attributeId: String,
    valueText: String,
    onValueChange: (String, String) -> Unit,
    isError: Boolean,
    errorText: String?,
) {
    val state = remember(valueText) { NationalityMultiSelectState(valueText) }

    LaunchedEffect(valueText) {
        state.updateFromExternalValue(valueText)
    }

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        state.selectedCodes.forEach { code ->
            val option = NationalityDataset.findByCode(code)
            val label = option?.let { "${it.flagEmoji} ${it.nationality}" } ?: code
            InputChip(
                selected = true,
                onClick = { state.removeNationality(code, attributeId, onValueChange) },
                label = { Text(label) },
                trailingIcon = {
                    Icon(Icons.Default.Close, contentDescription = "Remove $code")
                },
                modifier = Modifier.semantics { contentDescription = "Selected nationality $code" },
            )
        }
    }

    val cd = "$attributeId value"
    ExposedDropdownMenuBox(
        expanded = state.expanded,
        onExpandedChange = { state.onExpandedChange(it) },
    ) {
        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = { text -> state.onSearchQueryChange(text) },
            label = { Text(if (state.selectedCodes.isEmpty()) attributeId else "Add nationality") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(state.expanded) },
            isError = isError,
            supportingText = { errorText?.let { Text(it) } },
            singleLine = true,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable)
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
                        state.addNationality(option.code, attributeId, onValueChange)
                    },
                )
            }
        }
    }
}
