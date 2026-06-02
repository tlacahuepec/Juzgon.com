package com.juzgon.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
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
