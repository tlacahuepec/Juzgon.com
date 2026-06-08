@file:Suppress("FunctionName", "MatchingDeclarationName")

package com.juzgon.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.juzgon.ui.theme.JuzgonVisualTheme

@Immutable
data class ScoreBarItem(
    val label: String,
    val icon: String?,
    val value: Int,
    val maxValue: Int,
)

@Composable
internal fun JuzgonScoreBarList(
    items: List<ScoreBarItem>,
    modifier: Modifier = Modifier,
    header: (@Composable () -> Unit)? = null,
) {
    if (items.isEmpty()) return

    val tokens = JuzgonVisualTheme.tokens
    val description = "Score list, ${items.size} attributes"

    Column(
        modifier =
            modifier
                .padding(tokens.spacing.large)
                .semantics { contentDescription = description },
    ) {
        header?.invoke()
        items.forEach { item ->
            JuzgonScoreRow(
                label = item.label,
                value = item.value,
                maxValue = item.maxValue,
                icon = item.icon?.let { emoji -> { Text(text = emoji) } },
            )
            Spacer(modifier = Modifier.height(tokens.spacing.small))
        }
    }
}
