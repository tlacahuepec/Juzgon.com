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
}
