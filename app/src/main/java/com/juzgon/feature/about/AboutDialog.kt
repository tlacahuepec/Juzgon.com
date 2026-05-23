@file:Suppress("FunctionName")

package com.juzgon.feature.about

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.juzgon.domain.BuildMetadata

@Composable
fun AboutDialog(
    metadata: BuildMetadata,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("About Juzgon") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Version ${metadata.versionName} (${metadata.versionCode})",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = "Channel: ${metadata.channel}",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = "Build: ${metadata.gitSha}",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
    )
}
