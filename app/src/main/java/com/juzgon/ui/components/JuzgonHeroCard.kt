@file:Suppress("FunctionName")

package com.juzgon.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.juzgon.ui.theme.JuzgonVisualTheme

private const val MIN_CARD_HEIGHT_DP = 200
private const val GLOW_RING_SIZE_DP = 96

@Suppress("LongParameterList")
@Composable
internal fun JuzgonHeroCard(
    title: String,
    tierLabel: String,
    scoreText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    image: @Composable () -> Unit,
) {
    val tokens = JuzgonVisualTheme.tokens
    val cardShape = RoundedCornerShape(tokens.shapes.cardCornerRadius)
    val description = "$title, $tierLabel $scoreText"

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = MIN_CARD_HEIGHT_DP.dp)
                .clip(cardShape)
                .background(Brush.verticalGradient(tokens.gradients.heroSurface))
                .clickable(onClick = onClick)
                .semantics(mergeDescendants = true) { contentDescription = description },
    ) {
        JuzgonGlowRing(
            contentDescription = "",
            modifier = Modifier.size(GLOW_RING_SIZE_DP.dp).align(Alignment.Center),
        ) {
            image()
        }

        Text(
            text = title,
            color = tokens.palette.textStrong,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier =
                Modifier
                    .align(Alignment.TopStart)
                    .padding(tokens.spacing.large),
        )

        JuzgonScorePill(
            tierText = tierLabel,
            scoreText = scoreText,
            modifier =
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(tokens.spacing.large),
        )
    }
}
