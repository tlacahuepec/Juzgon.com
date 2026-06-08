@file:Suppress("FunctionName")

package com.juzgon.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.juzgon.ui.theme.JuzgonVisualTheme

private const val CARD_WIDTH_DP = 110
private const val IMAGE_SIZE_DP = 56
private const val RING_THICKNESS_DP = 3
private const val MIN_TOUCH_TARGET_DP = 48

@Composable
internal fun JuzgonItemThumbnail(
    scoreText: String,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    image: @Composable () -> Unit,
) {
    val tokens = JuzgonVisualTheme.tokens
    val cardShape = RoundedCornerShape(tokens.shapes.cardCornerRadius)

    Column(
        modifier =
            modifier
                .width(CARD_WIDTH_DP.dp)
                .sizeIn(minHeight = MIN_TOUCH_TARGET_DP.dp)
                .clip(cardShape)
                .background(tokens.palette.elevatedBackground)
                .clickable(onClick = onClick)
                .semantics(mergeDescendants = true) {
                    this.contentDescription = contentDescription
                },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
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
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "\u2605",
                color = tokens.palette.ratingAccent,
                style = MaterialTheme.typography.labelSmall,
            )
            Spacer(modifier = Modifier.width(tokens.spacing.extraSmall))
            Text(
                text = scoreText,
                color = tokens.palette.ratingAccent,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(modifier = Modifier.height(tokens.spacing.small))
    }
}
