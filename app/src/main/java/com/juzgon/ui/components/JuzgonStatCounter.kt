@file:Suppress("FunctionName")

package com.juzgon.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import com.juzgon.ui.theme.JuzgonVisualTheme

@Composable
internal fun JuzgonStatCounter(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    val tokens = JuzgonVisualTheme.tokens
    val description = "$label: $value"

    Column(
        modifier = modifier.semantics(mergeDescendants = true) { contentDescription = description },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(tokens.spacing.extraSmall),
    ) {
        Text(
            text = value,
            color = tokens.palette.textStrong,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = label,
            color = tokens.palette.textMuted,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}
