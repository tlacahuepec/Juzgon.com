@file:Suppress("FunctionName")

package com.juzgon.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.juzgon.ui.theme.JuzgonVisualTheme

private val SegmentHorizontalPadding = 16.dp
private val SegmentVerticalPadding = 12.dp
private val MinTouchTarget = 48.dp
private const val UNSELECTED_BACKGROUND_ALPHA = 0.5f
private val PillShape = RoundedCornerShape(percent = 50)

@Composable
internal fun JuzgonSegmentedFilter(
    items: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = JuzgonVisualTheme.tokens

    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(tokens.spacing.small),
    ) {
        items.forEachIndexed { index, label ->
            val isSelected = index == selectedIndex

            Box(
                contentAlignment = Alignment.Center,
                modifier =
                    Modifier
                        .sizeIn(minHeight = MinTouchTarget)
                        .selectable(
                            selected = isSelected,
                            role = Role.Tab,
                            onClick = { onSelected(index) },
                        ).background(
                            color =
                                if (isSelected) {
                                    tokens.palette.secondaryGlow
                                } else {
                                    tokens.palette.panelBackground.copy(alpha = UNSELECTED_BACKGROUND_ALPHA)
                                },
                            shape = PillShape,
                        ).padding(horizontal = SegmentHorizontalPadding, vertical = SegmentVerticalPadding),
            ) {
                Text(
                    text = label,
                    color = if (isSelected) tokens.palette.textStrong else tokens.palette.textMuted,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                )
            }
        }
    }
}
