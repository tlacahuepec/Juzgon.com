@file:Suppress("FunctionName")

package com.juzgon.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.juzgon.ui.theme.JuzgonVisualTheme
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private const val IMAGE_SIZE_DP = 48
private const val RING_THICKNESS_DP = 3
private const val MAX_NAME_LINES = 2
private const val MAX_ATTRIBUTE_ROWS = 3
private const val MIN_RADAR_POINTS = 3
private const val MAX_RADAR_SCORE = 10f
private const val RADAR_RADIUS_RATIO = 0.85f
private const val MIN_SCORE_FRACTION = 0.1f

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
    rank: Int? = null,
    radarValues: List<Float> = emptyList(),
    attributes: List<GridCardAttribute> = emptyList(),
    image: @Composable () -> Unit,
) {
    val tokens = JuzgonVisualTheme.tokens
    val cardShape = RoundedCornerShape(tokens.shapes.cardCornerRadius)
    val description = "$name, $tierLabel $scoreText"

    Column(
        modifier =
            modifier
                .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                .clip(cardShape)
                .border(1.dp, Color.White.copy(alpha = 0.08f), cardShape)
                .background(tokens.palette.elevatedBackground)
                .clickable(onClick = onClick)
                .semantics(mergeDescendants = true) { contentDescription = description },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(tokens.spacing.small))
        GridCardAvatarWithRank(rank = rank, image = image)
        Spacer(modifier = Modifier.height(tokens.spacing.extraSmall))
        GridCardNameAndScore(name = name, tierLabel = tierLabel, scoreText = scoreText)
        val effectiveRadarValues = resolveRadarValues(radarValues, attributes)
        if (effectiveRadarValues.size >= MIN_RADAR_POINTS) {
            Spacer(modifier = Modifier.height(tokens.spacing.extraSmall))
            MiniRadarCanvas(values = effectiveRadarValues)
        }
        GridCardAttributes(attributes = attributes)
        Spacer(modifier = Modifier.height(tokens.spacing.extraSmall))
        GridCardFavoriteButton(onFavoriteClick = onFavoriteClick)
        Spacer(modifier = Modifier.height(tokens.spacing.extraSmall))
    }
}

private fun resolveRadarValues(
    radarValues: List<Float>,
    attributes: List<GridCardAttribute>,
): List<Float> =
    if (radarValues.size >= MIN_RADAR_POINTS) {
        radarValues
    } else if (attributes.size >= MIN_RADAR_POINTS) {
        attributes.mapNotNull { it.scoreText.substringBefore('/').toFloatOrNull() }
    } else {
        emptyList()
    }

@Composable
private fun GridCardAvatarWithRank(
    rank: Int?,
    image: @Composable () -> Unit,
) {
    val tokens = JuzgonVisualTheme.tokens
    Box(contentAlignment = Alignment.TopEnd) {
        JuzgonGlowRing(
            contentDescription = "",
            modifier = Modifier.size(IMAGE_SIZE_DP.dp),
            ringThickness = RING_THICKNESS_DP.dp,
        ) {
            image()
        }
        if (rank != null && rank > 0) {
            Box(
                contentAlignment = Alignment.Center,
                modifier =
                    Modifier
                        .offset(x = 2.dp, y = (-2).dp)
                        .sizeIn(minWidth = 20.dp, minHeight = 20.dp)
                        .background(
                            tokens.palette.ratingAccent,
                            RoundedCornerShape(tokens.shapes.pillCornerRadius),
                        ).border(
                            1.5.dp,
                            tokens.palette.baseBackground,
                            RoundedCornerShape(tokens.shapes.pillCornerRadius),
                        ).padding(horizontal = 4.dp, vertical = 1.dp),
            ) {
                Text(
                    text = "#$rank",
                    color = Color.Black,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    fontWeight = FontWeight.ExtraBold,
                )
            }
        }
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
    Spacer(modifier = Modifier.height(2.dp))
    Box(
        contentAlignment = Alignment.Center,
        modifier =
            Modifier
                .padding(horizontal = tokens.spacing.small)
                .background(
                    color = tokens.palette.primaryGlow.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(tokens.shapes.pillCornerRadius),
                ).border(
                    width = 1.dp,
                    color = tokens.palette.primaryGlow.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(tokens.shapes.pillCornerRadius),
                ).padding(horizontal = 8.dp, vertical = 2.dp),
    ) {
        Text(
            text = if (tierLabel.isNotBlank()) "$tierLabel \u2605 $scoreText" else scoreText,
            color = tokens.palette.primaryGlowStrong,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun MiniRadarCanvas(
    values: List<Float>,
    modifier: Modifier = Modifier,
) {
    val tokens = JuzgonVisualTheme.tokens
    val fillColor = tokens.palette.secondaryGlow.copy(alpha = 0.4f)
    val strokeColor = tokens.palette.primaryGlow
    val count = values.size

    Canvas(
        modifier =
            modifier
                .size(56.dp)
                .semantics { contentDescription = "Mini radar chart" },
    ) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val maxRadius = (size.minDimension / 2f) * RADAR_RADIUS_RATIO

        val path = Path()
        for (i in 0 until count) {
            val angle = (-PI / 2.0 + (2.0 * PI * i / count)).toFloat()
            val scoreFraction = (values[i] / MAX_RADAR_SCORE).coerceIn(MIN_SCORE_FRACTION, 1f)
            val r = maxRadius * scoreFraction
            val x = center.x + r * cos(angle)
            val y = center.y + r * sin(angle)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()

        drawPath(path = path, color = fillColor)
        drawPath(path = path, color = strokeColor, style = Stroke(width = 1.5.dp.toPx()))
    }
}

@Composable
private fun GridCardAttributes(attributes: List<GridCardAttribute>) {
    if (attributes.isNotEmpty()) {
        val tokens = JuzgonVisualTheme.tokens
        Spacer(modifier = Modifier.height(tokens.spacing.extraSmall))
        attributes.take(MAX_ATTRIBUTE_ROWS).forEach { attr ->
            val labelText = if (attr.emoji.isNotBlank()) "${attr.emoji} ${attr.label}" else attr.label
            Text(
                text = "$labelText \u2605 ${attr.scoreText}",
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
        IconButton(
            onClick = onFavoriteClick,
            modifier = Modifier.size(48.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.FavoriteBorder,
                contentDescription = "Toggle favorite",
                tint = tokens.palette.textMuted,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}
