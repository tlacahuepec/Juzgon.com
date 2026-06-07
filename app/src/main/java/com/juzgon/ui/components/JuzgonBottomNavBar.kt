@file:Suppress("FunctionName")

package com.juzgon.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.juzgon.ui.theme.JuzgonVisualTheme

private const val BAR_HEIGHT_DP = 64
private const val ICON_SIZE_DP = 24
private const val MIN_TOUCH_TARGET_DP = 48
private const val DISABLED_ALPHA = 0.5f

@Immutable
internal data class BottomNavItem(
    val icon: ImageVector,
    val label: String,
    val enabled: Boolean = true,
)

@Composable
internal fun JuzgonBottomNavBar(
    items: List<BottomNavItem>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = JuzgonVisualTheme.tokens
    val selectedColor = tokens.palette.primaryGlow
    val unselectedColor = tokens.palette.textMuted
    val backgroundColor = tokens.palette.elevatedBackground

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .height(BAR_HEIGHT_DP.dp)
                .background(backgroundColor),
    ) {
        items.forEachIndexed { index, item ->
            val isSelected = index == selectedIndex
            val tint = if (isSelected) selectedColor else unselectedColor
            val itemAlpha = if (item.enabled) 1f else DISABLED_ALPHA

            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .sizeIn(minHeight = MIN_TOUCH_TARGET_DP.dp)
                        .alpha(itemAlpha)
                        .selectable(
                            selected = isSelected,
                            role = Role.Tab,
                            enabled = item.enabled,
                            onClick = { onItemSelected(index) },
                        ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(ICON_SIZE_DP.dp),
                )
                Text(
                    text = item.label,
                    color = tint,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}
