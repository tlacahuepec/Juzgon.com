package com.juzgon.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class JuzgonThemeAccessibilityTest {
    private val tokens = JuzgonVisualTokenSelector.refreshTokens()

    @Test
    fun wcagRelativeLuminanceReferenceValues() {
        val blackLuminance = calculateRelativeLuminance(Color.Black)
        val whiteLuminance = calculateRelativeLuminance(Color.White)
        assertEquals(0.0, blackLuminance, 0.001)
        assertEquals(1.0, whiteLuminance, 0.001)

        val blackWhiteContrast = calculateContrastRatio(Color.White, Color.Black)
        assertEquals(21.0, blackWhiteContrast, 0.1)
    }

    @Test
    fun textStrongOverHeroSurfaceGradientColorsExceedsWcagAa() {
        val textStrong = tokens.palette.textStrong
        tokens.gradients.heroSurface.forEachIndexed { index, surfaceColor ->
            val ratio = calculateContrastRatio(textStrong, surfaceColor)
            assertTrue(
                "Color at heroSurface index $index ($surfaceColor) contrast $ratio did not exceed WCAG AA 4.5:1",
                ratio >= 4.5,
            )
        }
    }

    @Test
    fun textSoftOverPanelBackgroundExceedsWcagAa() {
        val textSoft = tokens.palette.textSoft
        val panelBackground = tokens.palette.panelBackground
        val ratio = calculateContrastRatio(textSoft, panelBackground)
        assertTrue(
            "textSoft over panelBackground contrast $ratio did not exceed WCAG AA 4.5:1",
            ratio >= 4.5,
        )
    }

    @Test
    fun textStrongOverPanelBackgroundExceedsWcagAa() {
        val textStrong = tokens.palette.textStrong
        val panelBackground = tokens.palette.panelBackground
        val ratio = calculateContrastRatio(textStrong, panelBackground)
        assertTrue(
            "textStrong over panelBackground contrast $ratio did not exceed WCAG AA 4.5:1",
            ratio >= 4.5,
        )
    }

    @Test
    fun textStrongOverElevatedBackgroundExceedsWcagAa() {
        val textStrong = tokens.palette.textStrong
        val elevatedBackground = tokens.palette.elevatedBackground
        val ratio = calculateContrastRatio(textStrong, elevatedBackground)
        assertTrue(
            "textStrong over elevatedBackground contrast $ratio did not exceed WCAG AA 4.5:1",
            ratio >= 4.5,
        )
    }

    @Test
    fun textStrongOverBaseBackgroundExceedsWcagAa() {
        val textStrong = tokens.palette.textStrong
        val baseBackground = tokens.palette.baseBackground
        val ratio = calculateContrastRatio(textStrong, baseBackground)
        assertTrue(
            "textStrong over baseBackground contrast $ratio did not exceed WCAG AA 4.5:1",
            ratio >= 4.5,
        )
    }

    @Test
    fun scorePillTextOverTranslucentPillBackgroundMeetsWcagAa() {
        val textStrong = tokens.palette.textStrong
        val translucentPillBg = tokens.palette.panelBackground.copy(alpha = 0.7f)
        val compositedOnElevated = translucentPillBg.compositeOver(tokens.palette.elevatedBackground)
        val compositedOnBase = translucentPillBg.compositeOver(tokens.palette.baseBackground)

        val ratioElevated = calculateContrastRatio(textStrong, compositedOnElevated)
        val ratioBase = calculateContrastRatio(textStrong, compositedOnBase)

        assertTrue(
            "Score pill text over translucent pill on elevatedBackground ($ratioElevated) must meet 4.5:1",
            ratioElevated >= 4.5,
        )
        assertTrue(
            "Score pill text over translucent pill on baseBackground ($ratioBase) must meet 4.5:1",
            ratioBase >= 4.5,
        )
    }
}
