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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.juzgon.domain.NationalityDataset

private const val MAX_AUTOCOMPLETE_RESULTS = 10

@Suppress("LongMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NationalityAutocompleteField(
    attributeId: String,
    valueText: String,
    onValueChange: (String, String) -> Unit,
    isError: Boolean,
    errorText: String?,
) {
    var expanded by remember { mutableStateOf(false) }
    var searchQuery by remember(valueText) {
        mutableStateOf(
            NationalityDataset
                .findByCode(valueText)
                ?.let { "${it.flagEmoji} ${it.nationality}" }
                ?: valueText,
        )
    }

    val suggestions =
        remember(searchQuery, valueText) {
            val resolvedDisplay =
                NationalityDataset
                    .findByCode(valueText)
                    ?.let { "${it.flagEmoji} ${it.nationality}" }
            if (searchQuery.isBlank() || resolvedDisplay == searchQuery) {
                NationalityDataset.all.take(MAX_AUTOCOMPLETE_RESULTS)
            } else {
                NationalityDataset
                    .search(searchQuery.trimStart { it.isWhitespace() || !it.isLetter() })
                    .take(MAX_AUTOCOMPLETE_RESULTS)
            }
        }

    val cd = "$attributeId value"
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { text ->
                searchQuery = text
                expanded = true
            },
            label = { Text(attributeId) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
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
            expanded = expanded && suggestions.isNotEmpty(),
            onDismissRequest = { expanded = false },
        ) {
            suggestions.forEach { option ->
                DropdownMenuItem(
                    text = { Text("${option.flagEmoji} ${option.nationality} (${option.country})") },
                    onClick = {
                        searchQuery = "${option.flagEmoji} ${option.nationality}"
                        onValueChange(attributeId, option.code)
                        expanded = false
                    },
                )
            }
        }
    }
}
