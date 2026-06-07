package com.juzgon.ui.theme

import android.os.Build
import androidx.compose.animation.core.EaseIn
import androidx.compose.animation.core.EaseOut
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

class JuzgonThemeSelectorTest {
    @Test
    fun dynamicColorIsUsedOnlyWhenEnabledOnSupportedSdk() {
        assertTrue(JuzgonThemeSelector.shouldUseDynamicColor(dynamicColor = true, sdkInt = Build.VERSION_CODES.S))
        assertTrue(
            JuzgonThemeSelector.shouldUseDynamicColor(
                dynamicColor = true,
                sdkInt = Build.VERSION_CODES.UPSIDE_DOWN_CAKE,
            ),
        )
        assertFalse(JuzgonThemeSelector.shouldUseDynamicColor(dynamicColor = true, sdkInt = Build.VERSION_CODES.R))
        assertFalse(JuzgonThemeSelector.shouldUseDynamicColor(dynamicColor = false, sdkInt = Build.VERSION_CODES.S))
    }

    @Test
    fun fallbackColorSchemeUsesDarkAndLightPalettes() {
        assertEquals(JuzgonDarkColorScheme, JuzgonThemeSelector.fallbackColorScheme(darkTheme = true))
        assertEquals(JuzgonLightColorScheme, JuzgonThemeSelector.fallbackColorScheme(darkTheme = false))
    }

    @Test
    fun refreshVisualTokensAreAvailableWithoutDynamicColor() {
        val tokens = JuzgonVisualTokenSelector.refreshTokens()

        assertEquals(Color(0xFF05040A), tokens.palette.baseBackground)
        assertEquals(Color(0xFFC026D3), tokens.palette.primaryGlow)
        assertEquals(Color(0xFF22D3EE), tokens.palette.contrastAccent)
        assertEquals(listOf(Color(0xFF7C3AED), Color(0xFFD946EF), Color(0xFFFDBA74)), tokens.gradients.scoreBar)
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class JuzgonThemeSmokeTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun themeAppliesLightFallbackScheme() {
        var primary = Color.Unspecified

        composeRule.setContent {
            JuzgonTheme(
                darkTheme = false,
                dynamicColor = false,
            ) {
                primary = MaterialTheme.colorScheme.primary
                Text("Light theme")
            }
        }

        composeRule.onNodeWithText("Light theme").assertIsDisplayed()
        assertEquals(JuzgonLightColorScheme.primary, primary)
    }

    @Test
    fun themeAppliesDarkFallbackScheme() {
        var primary = Color.Unspecified

        composeRule.setContent {
            JuzgonTheme(
                darkTheme = true,
                dynamicColor = false,
            ) {
                primary = MaterialTheme.colorScheme.primary
                Text("Dark theme")
            }
        }

        composeRule.onNodeWithText("Dark theme").assertIsDisplayed()
        assertEquals(JuzgonDarkColorScheme.primary, primary)
    }

    @Test
    fun themeProvidesRefreshVisualTokensWithoutChangingFallbackScheme() {
        var primary = Color.Unspecified
        var primaryGlow = Color.Unspecified

        composeRule.setContent {
            JuzgonTheme(
                darkTheme = true,
                dynamicColor = false,
            ) {
                primary = MaterialTheme.colorScheme.primary
                primaryGlow = JuzgonVisualTheme.tokens.palette.primaryGlow
                Text("Visual tokens")
            }
        }

        composeRule.onNodeWithText("Visual tokens").assertIsDisplayed()
        assertEquals(JuzgonDarkColorScheme.primary, primary)
        assertEquals(Color(0xFFC026D3), primaryGlow)
    }
}

class JuzgonVisualTokensAnimationGlowTest {
    @Test
    fun animationShortDurationIs150() {
        val tokens = JuzgonVisualTokenSelector.refreshTokens()
        assertEquals(150, tokens.animations.shortDuration)
    }

    @Test
    fun animationMediumDurationIs300() {
        val tokens = JuzgonVisualTokenSelector.refreshTokens()
        assertEquals(300, tokens.animations.mediumDuration)
    }

    @Test
    fun animationLongDurationIs500() {
        val tokens = JuzgonVisualTokenSelector.refreshTokens()
        assertEquals(500, tokens.animations.longDuration)
    }

    @Test
    fun enterEasingIsEaseOut() {
        val tokens = JuzgonVisualTokenSelector.refreshTokens()
        assertEquals(EaseOut, tokens.animations.enterEasing)
    }

    @Test
    fun exitEasingIsEaseIn() {
        val tokens = JuzgonVisualTokenSelector.refreshTokens()
        assertEquals(EaseIn, tokens.animations.exitEasing)
    }

    @Test
    fun glowRadiusSmallIs4dp() {
        val tokens = JuzgonVisualTokenSelector.refreshTokens()
        assertEquals(4.dp, tokens.glow.radiusSmall)
    }

    @Test
    fun glowRadiusMediumIs8dp() {
        val tokens = JuzgonVisualTokenSelector.refreshTokens()
        assertEquals(8.dp, tokens.glow.radiusMedium)
    }

    @Test
    fun glowRadiusLargeIs16dp() {
        val tokens = JuzgonVisualTokenSelector.refreshTokens()
        assertEquals(16.dp, tokens.glow.radiusLarge)
    }

    @Test
    fun glowRingGradientHasThreeColors() {
        val tokens = JuzgonVisualTokenSelector.refreshTokens()
        assertEquals(3, tokens.gradients.glowRing.size)
    }

    @Test
    fun glowRingGradientStartsWithPrimaryGlow() {
        val tokens = JuzgonVisualTokenSelector.refreshTokens()
        assertEquals(Color(0xFFC026D3), tokens.gradients.glowRing[0])
    }

    @Test
    fun existingPaletteTokensUnchanged() {
        val tokens = JuzgonVisualTokenSelector.refreshTokens()
        assertEquals(Color(0xFF05040A), tokens.palette.baseBackground)
        assertEquals(Color(0xFFC026D3), tokens.palette.primaryGlow)
        assertEquals(Color(0xFF22D3EE), tokens.palette.contrastAccent)
    }

    @Test
    fun existingGradientTokensUnchanged() {
        val tokens = JuzgonVisualTokenSelector.refreshTokens()
        assertEquals(
            listOf(Color(0xFF7C3AED), Color(0xFFD946EF), Color(0xFFFDBA74)),
            tokens.gradients.scoreBar,
        )
        assertEquals(
            listOf(Color(0xFF7C3AED), Color(0xFFD946EF), Color(0xFF22D3EE)),
            tokens.gradients.glowBorder,
        )
    }

    @Test
    fun existingShapesUnchanged() {
        val tokens = JuzgonVisualTokenSelector.refreshTokens()
        assertEquals(20.dp, tokens.shapes.cardCornerRadius)
        assertEquals(28.dp, tokens.shapes.pillCornerRadius)
        assertEquals(999.dp, tokens.shapes.avatarCornerRadius)
    }

    @Test
    fun existingSpacingUnchanged() {
        val tokens = JuzgonVisualTokenSelector.refreshTokens()
        assertEquals(4.dp, tokens.spacing.extraSmall)
        assertEquals(8.dp, tokens.spacing.small)
        assertEquals(12.dp, tokens.spacing.medium)
        assertEquals(16.dp, tokens.spacing.large)
        assertEquals(24.dp, tokens.spacing.extraLarge)
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class JuzgonVisualTokensCompositionLocalTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun animationTokensAccessibleViaCompositionLocal() {
        var shortDuration = 0

        composeRule.setContent {
            JuzgonTheme(darkTheme = true, dynamicColor = false) {
                shortDuration = JuzgonVisualTheme.tokens.animations.shortDuration
            }
        }

        assertEquals(150, shortDuration)
    }

    @Test
    fun glowTokensAccessibleViaCompositionLocal() {
        var radiusMedium = 0.dp

        composeRule.setContent {
            JuzgonTheme(darkTheme = true, dynamicColor = false) {
                radiusMedium = JuzgonVisualTheme.tokens.glow.radiusMedium
            }
        }

        assertEquals(8.dp, radiusMedium)
    }

    @Test
    fun glowRingGradientAccessibleViaCompositionLocal() {
        var glowRingSize = 0

        composeRule.setContent {
            JuzgonTheme(darkTheme = true, dynamicColor = false) {
                glowRingSize = JuzgonVisualTheme.tokens.gradients.glowRing.size
            }
        }

        assertEquals(3, glowRingSize)
    }
}
