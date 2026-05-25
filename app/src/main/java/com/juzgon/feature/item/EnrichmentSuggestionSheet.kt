@file:Suppress("LongMethod", "FunctionNaming", "FunctionName")

package com.juzgon.feature.item

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.juzgon.domain.enrichment.EnrichmentConfidence
import com.juzgon.domain.enrichment.EnrichmentFailureCode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnrichmentSuggestionSheet(
    state: EnrichmentSheetState,
    onAccept: () -> Unit,
    onDismiss: () -> Unit,
    onNavigateToSettings: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when (state) {
                is EnrichmentSheetState.Loading -> LoadingContent()
                is EnrichmentSheetState.NoKey -> NoKeyContent(onNavigateToSettings, onDismiss)
                is EnrichmentSheetState.Found -> FoundContent(state, onAccept, onDismiss)
                is EnrichmentSheetState.NotFound -> NotFoundContent(onDismiss)
                is EnrichmentSheetState.Conflict -> ConflictContent(onDismiss)
                is EnrichmentSheetState.Error -> ErrorContent(state, onDismiss)
                is EnrichmentSheetState.Hidden -> {}
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun LoadingContent() {
    Text(
        text = "Searching reliable sources...",
        style = MaterialTheme.typography.titleMedium,
    )
    Spacer(modifier = Modifier.height(16.dp))
    CircularProgressIndicator(
        modifier = Modifier.semantics { contentDescription = "Loading enrichment" },
    )
}

@Composable
private fun NoKeyContent(
    onNavigateToSettings: () -> Unit,
    onDismiss: () -> Unit,
) {
    Text(
        text = "Gemini API key required.",
        style = MaterialTheme.typography.titleMedium,
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = "Configure your Gemini API key in settings to use suggestions.",
        style = MaterialTheme.typography.bodyMedium,
    )
    Spacer(modifier = Modifier.height(16.dp))
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
            Text("Cancel")
        }
        Button(onClick = onNavigateToSettings, modifier = Modifier.weight(1f)) {
            Text("Go to Settings")
        }
    }
}

@Composable
private fun FoundContent(
    state: EnrichmentSheetState.Found,
    onAccept: () -> Unit,
    onDismiss: () -> Unit,
) {
    Text(
        text = "Suggested Birth Date",
        style = MaterialTheme.typography.titleMedium,
    )
    Spacer(modifier = Modifier.height(12.dp))
    Text(
        text = state.displayValue ?: state.suggestedValue,
        style = MaterialTheme.typography.headlineSmall,
        modifier = Modifier.semantics { contentDescription = "Suggested value" },
    )
    Spacer(modifier = Modifier.height(12.dp))
    state.confidence?.let { confidence ->
        Text(
            text = "Confidence: ${confidence.name}",
            style = MaterialTheme.typography.bodyMedium,
            color = confidenceColor(confidence),
        )
    }
    if (state.sources.isNotEmpty()) {
        Text(
            text = "Sources: ${state.sources.size}",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
    Spacer(modifier = Modifier.height(16.dp))
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
            Text("Dismiss")
        }
        Button(onClick = onAccept, modifier = Modifier.weight(1f)) {
            Text("Accept")
        }
    }
}

@Composable
private fun NotFoundContent(onDismiss: () -> Unit) {
    Text(
        text = "Couldn't find this reliably.",
        style = MaterialTheme.typography.titleMedium,
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = "No changes were made.",
        style = MaterialTheme.typography.bodyMedium,
    )
    Spacer(modifier = Modifier.height(16.dp))
    Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
        Text("OK")
    }
}

@Composable
private fun ConflictContent(onDismiss: () -> Unit) {
    Text(
        text = "Conflicting information found.",
        style = MaterialTheme.typography.titleMedium,
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = "No value was suggested.",
        style = MaterialTheme.typography.bodyMedium,
    )
    Spacer(modifier = Modifier.height(16.dp))
    Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
        Text("OK")
    }
}

@Composable
private fun ErrorContent(
    state: EnrichmentSheetState.Error,
    onDismiss: () -> Unit,
) {
    Text(
        text = "Something went wrong",
        style = MaterialTheme.typography.titleMedium,
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = errorMessage(state.failureCode),
        style = MaterialTheme.typography.bodyMedium,
    )
    Spacer(modifier = Modifier.height(16.dp))
    Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
        Text("OK")
    }
}

@Composable
private fun confidenceColor(confidence: EnrichmentConfidence) =
    when (confidence) {
        EnrichmentConfidence.HIGH -> MaterialTheme.colorScheme.primary
        EnrichmentConfidence.MEDIUM -> MaterialTheme.colorScheme.tertiary
        EnrichmentConfidence.LOW -> MaterialTheme.colorScheme.error
    }

private fun errorMessage(failureCode: EnrichmentFailureCode?): String =
    when (failureCode) {
        EnrichmentFailureCode.INVALID_API_KEY -> "Invalid API key. Check your Gemini key in settings."
        EnrichmentFailureCode.NETWORK_ERROR -> "Network error. Check your connection and try again."
        EnrichmentFailureCode.RATE_LIMITED -> "Too many requests. Please wait and try again."
        EnrichmentFailureCode.QUOTA_EXCEEDED -> "API quota exceeded. Try again later."
        else -> "Couldn't connect to Gemini. Check your API key, connection, or quota."
    }
