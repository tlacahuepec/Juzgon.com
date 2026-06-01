@file:Suppress("FunctionName")

package com.juzgon.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.juzgon.ui.theme.JuzgonVisualTheme

@Composable
internal fun JuzgonCollectionCard(
    metadata: JuzgonCollectionCardMetadata,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    visualContent: @Composable BoxScope.() -> Unit,
) {
    val tokens = JuzgonVisualTheme.tokens
    val shape = RoundedCornerShape(tokens.shapes.cardCornerRadius)

    Surface(
        color = tokens.palette.elevatedBackground,
        contentColor = tokens.palette.textStrong,
        shape = shape,
        modifier =
            modifier
                .fillMaxWidth()
                .height(COLLECTION_CARD_HEIGHT)
                .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                .clickable(
                    role = Role.Button,
                    onClick = onClick,
                ).semantics {
                    contentDescription = metadata.contentDescription
                    role = Role.Button
                },
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(Brush.linearGradient(tokens.gradients.heroSurface)),
                content = visualContent,
            )
            CollectionCardOverlay(
                metadata = metadata,
                modifier =
                    Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth(),
            )
        }
    }
}

internal data class JuzgonCollectionCardMetadata(
    val title: String,
    val rankLabel: String?,
    val metric: JuzgonCollectionCardMetric,
    val contentDescription: String,
    val badge: String? = null,
)

internal data class JuzgonCollectionCardMetric(
    val label: String,
    val value: String,
)

@Composable
private fun CollectionCardOverlay(
    metadata: JuzgonCollectionCardMetadata,
    modifier: Modifier = Modifier,
) {
    val tokens = JuzgonVisualTheme.tokens

    Surface(
        color = tokens.palette.baseBackground.copy(alpha = 0.9f),
        contentColor = tokens.palette.textStrong,
        modifier = modifier,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(tokens.spacing.medium),
            verticalAlignment = Alignment.CenterVertically,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(tokens.spacing.large),
        ) {
            metadata.rankLabel?.let { label ->
                CollectionCardPill(
                    text = label,
                    background = tokens.palette.secondaryGlow,
                    foreground = tokens.palette.textStrong,
                )
            }
            CollectionCardTitle(
                title = metadata.title,
                modifier = Modifier.weight(1f),
            )
            metadata.badge?.let { badgeText ->
                Text(
                    text = badgeText,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                )
            }
            CollectionCardMetric(
                label = metadata.metric.label,
                value = metadata.metric.value,
            )
        }
    }
}

@Composable
private fun CollectionCardPill(
    text: String,
    background: androidx.compose.ui.graphics.Color,
    foreground: androidx.compose.ui.graphics.Color,
) {
    val tokens = JuzgonVisualTheme.tokens

    Box(
        contentAlignment = Alignment.Center,
        modifier =
            Modifier
                .background(
                    color = background,
                    shape = RoundedCornerShape(tokens.shapes.pillCornerRadius),
                ).padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Text(
            text = text,
            color = foreground,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun CollectionCardMetric(
    label: String,
    value: String,
) {
    val tokens = JuzgonVisualTheme.tokens

    Column(
        horizontalAlignment = Alignment.End,
        modifier =
            Modifier
                .background(
                    brush = Brush.linearGradient(tokens.gradients.scoreBar),
                    shape = RoundedCornerShape(tokens.shapes.pillCornerRadius),
                ).padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Text(
            text = label,
            color = tokens.palette.textStrong,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
        )
        Text(
            text = value,
            color = tokens.palette.textStrong,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

@Composable
private fun CollectionCardTitle(
    title: String,
    modifier: Modifier = Modifier,
) {
    val parts = splitCollectionCardTitle(title)
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = parts.primaryWord,
            color = JuzgonVisualTheme.tokens.palette.textStrong,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (parts.remainingTitle != null) {
            Text(
                text = parts.remainingTitle,
                color = JuzgonVisualTheme.tokens.palette.textSoft,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun splitCollectionCardTitle(title: String): CollectionCardTitleParts {
    val words = title.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    return if (words.isEmpty()) {
        CollectionCardTitleParts(primaryWord = "")
    } else {
        CollectionCardTitleParts(
            primaryWord = words.first(),
            remainingTitle = words.drop(1).joinToString(" ").ifBlank { null },
        )
    }
}

private data class CollectionCardTitleParts(
    val primaryWord: String,
    val remainingTitle: String? = null,
)

private val COLLECTION_CARD_HEIGHT = 172.dp
