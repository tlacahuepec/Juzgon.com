@file:Suppress("FunctionName")

package com.juzgon.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.juzgon.ui.theme.JuzgonVisualTheme

private const val ICON_SIZE_DP = 24
private const val MIN_ROW_HEIGHT_DP = 48

@Composable
internal fun JuzgonScoreRow(
    label: String,
    value: Int,
    maxValue: Int,
    modifier: Modifier = Modifier,
    icon: (@Composable () -> Unit)? = null,
) {
    val tokens = JuzgonVisualTheme.tokens
    val description = "$label, score $value out of $maxValue"
    val progress = value.toFloat() / maxValue.toFloat()

    Row(
        modifier =
            modifier
                .heightIn(min = MIN_ROW_HEIGHT_DP.dp)
                .semantics(mergeDescendants = true) { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Box(modifier = Modifier.size(ICON_SIZE_DP.dp), contentAlignment = Alignment.Center) {
                icon()
            }
            Spacer(modifier = Modifier.width(tokens.spacing.small))
        }
        Text(
            text = label,
            color = tokens.palette.textSoft,
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(modifier = Modifier.width(tokens.spacing.small))
        JuzgonGradientScoreBar(
            progress = progress,
            modifier = Modifier.weight(1f),
        )
        Spacer(modifier = Modifier.width(tokens.spacing.small))
        Text(
            text = "$value/$maxValue",
            color = tokens.palette.textStrong,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
        )
    }
}
