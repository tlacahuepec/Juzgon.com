@file:Suppress("FunctionName")

package com.juzgon.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.juzgon.ui.theme.JuzgonVisualTheme

private val DefaultRingThickness = 6.dp
private val MinTouchTarget = 48.dp
private const val GLOW_SPREAD_RATIO = 2f
private const val OUTER_GLOW_ALPHA = 0.25f
private const val MIDDLE_GLOW_ALPHA = 0.5f
private const val OUTER_WIDTH_FACTOR = 3f
private const val MIDDLE_WIDTH_FACTOR = 2f

@Composable
internal fun JuzgonGlowRing(
    contentDescription: String,
    modifier: Modifier = Modifier,
    ringThickness: Dp = DefaultRingThickness,
    glowColors: List<Color>? = null,
    content: @Composable () -> Unit,
) {
    val colors = glowColors ?: JuzgonVisualTheme.tokens.gradients.glowBorder
    val primaryColor = colors.first()
    val accentColor = colors.getOrElse(1) { primaryColor }

    val glowSpread = ringThickness * GLOW_SPREAD_RATIO
    val totalPadding = ringThickness + glowSpread

    Box(
        modifier =
            Modifier
                .sizeIn(minWidth = MinTouchTarget, minHeight = MinTouchTarget)
                .then(modifier)
                .semantics { this.contentDescription = contentDescription }
                .drawBehind {
                    val center = this.center
                    val radius = (size.minDimension / 2f)

                    val outerStrokeWidth = ringThickness.toPx() * OUTER_WIDTH_FACTOR
                    drawCircle(
                        color = primaryColor.copy(alpha = OUTER_GLOW_ALPHA),
                        radius = radius,
                        center = center,
                        style = Stroke(width = outerStrokeWidth),
                    )

                    val middleStrokeWidth = ringThickness.toPx() * MIDDLE_WIDTH_FACTOR
                    drawCircle(
                        color = accentColor.copy(alpha = MIDDLE_GLOW_ALPHA),
                        radius = radius,
                        center = center,
                        style = Stroke(width = middleStrokeWidth),
                    )

                    val innerStrokeWidth = ringThickness.toPx()
                    drawCircle(
                        color = primaryColor,
                        radius = radius,
                        center = center,
                        style = Stroke(width = innerStrokeWidth),
                    )
                }.padding(totalPadding),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}
