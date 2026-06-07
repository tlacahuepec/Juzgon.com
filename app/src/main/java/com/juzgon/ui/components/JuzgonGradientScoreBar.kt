@file:Suppress("FunctionName")

package com.juzgon.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.juzgon.ui.theme.JuzgonVisualTheme

private val DefaultHeight = 12.dp
private const val PERCENT_MULTIPLIER = 100

@Composable
internal fun JuzgonGradientScoreBar(
    progress: Float,
    modifier: Modifier = Modifier,
    height: Dp = DefaultHeight,
) {
    val clampedProgress = progress.coerceIn(0f, 1f)
    val percentage = (clampedProgress * PERCENT_MULTIPLIER).toInt()
    val tokens = JuzgonVisualTheme.tokens
    val gradientColors = tokens.gradients.scoreBar
    val trackColor = tokens.palette.elevatedBackground

    Spacer(
        modifier =
            modifier
                .fillMaxWidth()
                .height(height)
                .semantics { contentDescription = "Score: $percentage%" }
                .drawWithCache {
                    val cornerRadius = CornerRadius(size.height / 2f)
                    val gradientBrush = Brush.horizontalGradient(gradientColors)
                    val fillWidth = size.width * clampedProgress

                    onDrawBehind {
                        drawRoundRect(
                            color = trackColor,
                            cornerRadius = cornerRadius,
                        )
                        if (clampedProgress > 0f) {
                            drawRoundRect(
                                brush = gradientBrush,
                                size = Size(fillWidth, size.height),
                                cornerRadius = cornerRadius,
                            )
                        }
                    }
                },
    )
}
