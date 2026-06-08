@file:Suppress("FunctionName")

package com.juzgon.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.juzgon.ui.theme.JuzgonVisualTheme

private const val IMAGE_SIZE_DP = 56
private const val RING_THICKNESS_DP = 3
private const val MAX_NAME_LINES = 2
private const val MAX_ATTRIBUTE_ROWS = 3

@Immutable
data class GridCardAttribute(
    val emoji: String,
    val label: String,
    val scoreText: String,
)

@Suppress("LongParameterList")
@Composable
internal fun JuzgonCollectionGridCard(
    name: String,
    tierLabel: String,
    scoreText: String,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    modifier: Modifier = Modifier,
    attributes: List<GridCardAttribute> = emptyList(),
    image: @Composable () -> Unit,
) {
    val tokens = JuzgonVisualTheme.tokens
    val cardShape = RoundedCornerShape(tokens.shapes.cardCornerRadius)
    val description = "$name, $tierLabel $scoreText"

    Column(
        modifier =
            modifier
                .clip(cardShape)
                .background(tokens.palette.elevatedBackground)
                .clickable(onClick = onClick)
                .semantics(mergeDescendants = true) { contentDescription = description },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(tokens.spacing.small))
        JuzgonGlowRing(
            contentDescription = "",
            modifier = Modifier.size(IMAGE_SIZE_DP.dp),
            ringThickness = RING_THICKNESS_DP.dp,
        ) {
            image()
        }
        Spacer(modifier = Modifier.height(tokens.spacing.extraSmall))
        GridCardNameAndScore(name = name, tierLabel = tierLabel, scoreText = scoreText)
        GridCardAttributes(attributes = attributes)
        Spacer(modifier = Modifier.weight(1f))
        GridCardFavoriteButton(onFavoriteClick = onFavoriteClick)
        Spacer(modifier = Modifier.height(tokens.spacing.extraSmall))
    }
}

@Composable
private fun GridCardNameAndScore(
    name: String,
    tierLabel: String,
    scoreText: String,
) {
    val tokens = JuzgonVisualTheme.tokens
    Text(
        text = name,
        color = tokens.palette.textStrong,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        maxLines = MAX_NAME_LINES,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(horizontal = tokens.spacing.small),
    )
    Text(
        text = "$tierLabel \u2605 $scoreText",
        color = tokens.palette.ratingAccent,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
private fun GridCardAttributes(attributes: List<GridCardAttribute>) {
    if (attributes.isNotEmpty()) {
        val tokens = JuzgonVisualTheme.tokens
        Spacer(modifier = Modifier.height(tokens.spacing.extraSmall))
        attributes.take(MAX_ATTRIBUTE_ROWS).forEach { attr ->
            Text(
                text = "${attr.emoji} ${attr.label} \u2605 ${attr.scoreText}",
                color = tokens.palette.textSoft,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@Composable
private fun GridCardFavoriteButton(onFavoriteClick: () -> Unit) {
    val tokens = JuzgonVisualTheme.tokens
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.BottomEnd,
    ) {
        IconButton(onClick = onFavoriteClick) {
            Icon(
                imageVector = Icons.Outlined.FavoriteBorder,
                contentDescription = "Toggle favorite",
                tint = tokens.palette.textMuted,
            )
        }
    }
}
