@file:Suppress("FunctionName")

package com.juzgon.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
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

private val HorizontalPadding = 12.dp
private val VerticalPadding = 8.dp
private val MinTouchTarget = 48.dp
private const val BACKGROUND_ALPHA = 0.7f

@Composable
internal fun JuzgonScorePill(
    tierText: String,
    scoreText: String,
    modifier: Modifier = Modifier,
    icon: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    val tokens = JuzgonVisualTheme.tokens
    val description = "$tierText, $scoreText"

    val baseModifier =
        modifier
            .semantics(mergeDescendants = true) { contentDescription = description }
            .background(
                color = tokens.palette.panelBackground.copy(alpha = BACKGROUND_ALPHA),
                shape = RoundedCornerShape(tokens.shapes.pillCornerRadius),
            )

    val interactiveModifier =
        if (onClick != null) {
            Modifier
                .sizeIn(minWidth = MinTouchTarget, minHeight = MinTouchTarget)
                .then(baseModifier)
                .clickable(onClick = onClick)
        } else {
            baseModifier
        }

    Row(
        modifier = interactiveModifier.padding(horizontal = HorizontalPadding, vertical = VerticalPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(tokens.spacing.extraSmall),
    ) {
        icon?.invoke()

        Text(
            text = tierText,
            color = tokens.palette.textStrong,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
        )

        Text(
            text = "\u2022",
            color = tokens.palette.textMuted,
            style = MaterialTheme.typography.labelLarge,
        )

        Text(
            text = scoreText,
            color = tokens.palette.textStrong,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
        )
    }
}
