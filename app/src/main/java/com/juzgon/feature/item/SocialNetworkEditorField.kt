@file:Suppress("FunctionName")

package com.juzgon.feature.item

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.juzgon.domain.social.SocialPlatform
import com.juzgon.domain.social.SocialPlatformIcons

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SocialNetworkEditorField(
    attributeId: String,
    valueText: String,
    onValueChange: (String, String) -> Unit,
) {
    val state = remember(valueText) { SocialNetworkEditorState(valueText) }

    LaunchedEffect(valueText) {
        state.updateFromExternalValue(valueText)
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        state.entries.forEachIndexed { index, entry ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    painter = painterResource(SocialPlatformIcons.iconRes(entry.platform)),
                    contentDescription = entry.platform.displayName,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "${entry.platform.displayName}: ${entry.handle}",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick = { state.removeEntry(index, attributeId, onValueChange) },
                    modifier =
                        Modifier.semantics {
                            contentDescription = "Remove ${entry.platform.displayName}"
                        },
                ) {
                    Icon(Icons.Default.Close, contentDescription = null)
                }
            }
        }

        AddSocialNetworkRow(
            attributeId = attributeId,
            state = state,
            onValueChange = onValueChange,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddSocialNetworkRow(
    attributeId: String,
    state: SocialNetworkEditorState,
    onValueChange: (String, String) -> Unit,
) {
    var showAddForm by remember { mutableStateOf(false) }
    var selectedPlatform by remember { mutableStateOf<SocialPlatform?>(null) }
    var handleText by remember { mutableStateOf("") }

    if (!showAddForm) {
        OutlinedButton(
            onClick = { showAddForm = true },
            modifier = Modifier.semantics { contentDescription = "Add social network" },
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Text("Add social network")
        }
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        PlatformDropdown(
            selectedPlatform = selectedPlatform,
            onPlatformSelected = { selectedPlatform = it },
        )

        OutlinedTextField(
            value = handleText,
            onValueChange = { handleText = it },
            label = { Text("Handle or URL") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = {
                    showAddForm = false
                    selectedPlatform = null
                    handleText = ""
                },
            ) {
                Text("Cancel")
            }
            OutlinedButton(
                onClick = {
                    val platform = selectedPlatform ?: return@OutlinedButton
                    val handle = handleText.trim()
                    if (handle.isNotEmpty()) {
                        state.addEntry(platform, handle, attributeId, onValueChange)
                        showAddForm = false
                        selectedPlatform = null
                        handleText = ""
                    }
                },
                enabled = selectedPlatform != null && handleText.isNotBlank(),
            ) {
                Text("Add")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlatformDropdown(
    selectedPlatform: SocialPlatform?,
    onPlatformSelected: (SocialPlatform) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        OutlinedTextField(
            value = selectedPlatform?.displayName ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text("Platform") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            SocialPlatform.entries.forEach { platform ->
                DropdownMenuItem(
                    text = { Text(platform.displayName) },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(SocialPlatformIcons.iconRes(platform)),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    },
                    onClick = {
                        onPlatformSelected(platform)
                        expanded = false
                    },
                )
            }
        }
    }
}
