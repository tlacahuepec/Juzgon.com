package com.juzgon.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertTrue
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Calculates the relative luminance of an sRGB color according to WCAG 2.1 specs.
 * Formula: 0.2126 * R + 0.7152 * G + 0.0722 * B where components are linearized.
 */
internal fun calculateRelativeLuminance(color: Color): Double {
    fun componentLuminance(component: Float): Double =
        if (component <= 0.04045f) {
            component.toDouble() / 12.92
        } else {
            ((component.toDouble() + 0.055) / 1.055).pow(2.4)
        }

    val r = componentLuminance(color.red)
    val g = componentLuminance(color.green)
    val b = componentLuminance(color.blue)
    return 0.2126 * r + 0.7152 * g + 0.0722 * b
}

/**
 * Calculates the WCAG 2.1 contrast ratio between two colors: (L1 + 0.05) / (L2 + 0.05).
 * If the foreground has transparency (alpha < 1f), it is composited over the background first.
 */
internal fun calculateContrastRatio(
    foreground: Color,
    background: Color,
): Double {
    val effectiveFg = if (foreground.alpha < 1f) foreground.compositeOver(background) else foreground
    val l1 = calculateRelativeLuminance(effectiveFg)
    val l2 = calculateRelativeLuminance(background)
    val lighter = max(l1, l2)
    val darker = min(l1, l2)
    return (lighter + 0.05) / (darker + 0.05)
}

/**
 * Asserts that a SemanticsNode meets the minimum touch target requirement (default 48x48dp).
 */
internal fun assertMinimumTouchTarget(
    node: SemanticsNode,
    minSizeDp: Dp = 48.dp,
) {
    val density = node.layoutInfo.density
    val minPx = with(density) { minSizeDp.toPx() }
    assertTrue(
        "Touch target width ${node.size.width}px is less than minimum ${minPx}px ($minSizeDp)",
        node.size.width >= minPx - 0.5f,
    )
    assertTrue(
        "Touch target height ${node.size.height}px is less than minimum ${minPx}px ($minSizeDp)",
        node.size.height >= minPx - 0.5f,
    )
}

/**
 * Extension on SemanticsNodeInteraction to assert minimum touch target dimensions.
 */
internal fun SemanticsNodeInteraction.assertMinimumTouchTarget(minSizeDp: Dp = 48.dp): SemanticsNodeInteraction {
    assertWidthIsAtLeast(minSizeDp)
    assertHeightIsAtLeast(minSizeDp)
    return this
}
