@file:Suppress("FunctionName")

package com.juzgon.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.juzgon.ui.theme.JuzgonVisualTheme
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

private const val GRID_RINGS = 4
private const val MIN_POINTS = 3
private const val GRID_ALPHA = 0.3f
private const val FILL_ALPHA = 0.4f
private const val STROKE_WIDTH_DP = 3
private const val VERTEX_DOT_RADIUS_DP = 4
private const val FULL_RADIUS_FRACTION = 0.32f
private const val COMPACT_RADIUS_FRACTION = 0.42f
private const val COMPACT_SIZE_DP = 160
private const val FULL_HEIGHT_DP = 280
private const val LABEL_OFFSET_DP = 14

@Immutable
internal data class RadarChartPoint(
    val label: String,
    val value: Float,
    val maxValue: Float = 10f,
) {
    val fraction: Float get() = (value / maxValue).coerceIn(0f, 1f)
}

@Composable
internal fun JuzgonRadarChart(
    points: List<RadarChartPoint>,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    if (points.size < MIN_POINTS) {
        RadarChartFallback(modifier)
        return
    }

    val tokens = JuzgonVisualTheme.tokens
    val gridColor = tokens.palette.contrastAccentSoft.copy(alpha = GRID_ALPHA)
    val fillColor = tokens.palette.ratingAccent.copy(alpha = FILL_ALPHA)
    val strokeColor = tokens.palette.contrastAccent
    val labelColor = tokens.palette.textMuted
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = labelColor)
    val textMeasurer = rememberTextMeasurer()

    val description = remember(points) { buildSemanticDescription(points) }

    val chartModifier =
        if (compact) {
            modifier
                .size(COMPACT_SIZE_DP.dp)
                .semantics { contentDescription = description }
        } else {
            modifier
                .fillMaxWidth()
                .height(FULL_HEIGHT_DP.dp)
                .semantics { contentDescription = description }
        }

    val radiusFraction = if (compact) COMPACT_RADIUS_FRACTION else FULL_RADIUS_FRACTION

    Canvas(modifier = chartModifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = min(size.width, size.height) * radiusFraction

        drawGrid(center, radius, points.size, gridColor)
        drawDataShape(center, radius, points, fillColor, strokeColor)

        if (!compact) {
            drawLabels(center, radius, points, textMeasurer, labelStyle)
        }
    }

    if (!compact) {
        LabelOverlay(points, modifier = Modifier.fillMaxWidth().height(FULL_HEIGHT_DP.dp))
    }
}

@Composable
private fun RadarChartFallback(modifier: Modifier) {
    Box(
        modifier =
            modifier.semantics {
                contentDescription = "Radar chart requires at least 3 attributes"
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Not enough data",
            color = JuzgonVisualTheme.tokens.palette.textMuted,
        )
    }
}

@Composable
private fun LabelOverlay(
    points: List<RadarChartPoint>,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        points.forEach { point ->
            Text(
                text = point.label,
                color = JuzgonVisualTheme.tokens.palette.textMuted,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.semantics { contentDescription = point.label },
            )
        }
    }
}

private fun buildSemanticDescription(points: List<RadarChartPoint>): String {
    val highest = points.maxBy { it.fraction }
    return "Radar chart, ${points.size} attributes, " +
        "highest ${highest.label} ${highest.value.toInt()} of ${highest.maxValue.toInt()}"
}

private fun DrawScope.drawGrid(
    center: Offset,
    radius: Float,
    pointCount: Int,
    color: androidx.compose.ui.graphics.Color,
) {
    repeat(GRID_RINGS) { index ->
        val scale = (index + 1) / GRID_RINGS.toFloat()
        drawPath(
            path = regularPolygonPath(center, radius * scale, pointCount),
            color = color,
            style = Stroke(width = 1.dp.toPx()),
        )
    }
    repeat(pointCount) { index ->
        val outer = vertexOffset(center, radius, index, pointCount)
        drawLine(color = color, start = center, end = outer, strokeWidth = 1.dp.toPx())
    }
}

private fun DrawScope.drawDataShape(
    center: Offset,
    radius: Float,
    points: List<RadarChartPoint>,
    fillColor: androidx.compose.ui.graphics.Color,
    strokeColor: androidx.compose.ui.graphics.Color,
) {
    val path = valuePolygonPath(center, radius, points)
    drawPath(path = path, color = fillColor)
    drawPath(path = path, color = strokeColor, style = Stroke(width = STROKE_WIDTH_DP.dp.toPx()))
    val dotRadius = VERTEX_DOT_RADIUS_DP.dp.toPx()
    points.forEachIndexed { index, point ->
        val pos = vertexOffset(center, radius * point.fraction, index, points.size)
        drawCircle(color = strokeColor, radius = dotRadius, center = pos)
    }
}

private fun DrawScope.drawLabels(
    center: Offset,
    radius: Float,
    points: List<RadarChartPoint>,
    textMeasurer: TextMeasurer,
    style: TextStyle,
) {
    val labelOffset = LABEL_OFFSET_DP.dp.toPx()
    points.forEachIndexed { index, point ->
        val pos = vertexOffset(center, radius + labelOffset, index, points.size)
        val layoutResult = textMeasurer.measure(point.label, style)
        val topLeft =
            Offset(
                x = pos.x - layoutResult.size.width / 2f,
                y = pos.y - layoutResult.size.height / 2f,
            )
        drawText(layoutResult, topLeft = topLeft)
    }
}

private fun regularPolygonPath(
    center: Offset,
    radius: Float,
    pointCount: Int,
): Path =
    Path().also { path ->
        repeat(pointCount) { index ->
            val offset = vertexOffset(center, radius, index, pointCount)
            if (index == 0) path.moveTo(offset.x, offset.y) else path.lineTo(offset.x, offset.y)
        }
        path.close()
    }

private fun valuePolygonPath(
    center: Offset,
    radius: Float,
    points: List<RadarChartPoint>,
): Path =
    Path().also { path ->
        points.forEachIndexed { index, point ->
            val offset = vertexOffset(center, radius * point.fraction, index, points.size)
            if (index == 0) path.moveTo(offset.x, offset.y) else path.lineTo(offset.x, offset.y)
        }
        path.close()
    }

private fun vertexOffset(
    center: Offset,
    radius: Float,
    index: Int,
    count: Int,
): Offset {
    val angle = -PI / 2.0 + (2.0 * PI * index / count)
    return Offset(
        x = center.x + (cos(angle) * radius).toFloat(),
        y = center.y + (sin(angle) * radius).toFloat(),
    )
}
