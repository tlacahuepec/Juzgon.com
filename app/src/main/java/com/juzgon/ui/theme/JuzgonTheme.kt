@file:Suppress("FunctionName", "MagicNumber")

package com.juzgon.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

internal val JuzgonLightColorScheme =
    lightColorScheme(
        primary = Color(0xFF6200EE),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFE8DEFF),
        onPrimaryContainer = Color(0xFF1C0040),
        secondary = Color(0xFFC49500),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFFFDF80),
        onSecondaryContainer = Color(0xFF3D2E00),
        tertiary = Color(0xFF7C3AED),
        onTertiary = Color.White,
        tertiaryContainer = Color(0xFFE8DEFF),
        onTertiaryContainer = Color(0xFF1C0040),
        background = Color(0xFFFFFBFE),
        onBackground = Color(0xFF1C1B1F),
        surface = Color(0xFFFFFBFE),
        onSurface = Color(0xFF1C1B1F),
        error = Color(0xFFBA1A1A),
        onError = Color.White,
    )

internal val JuzgonDarkColorScheme =
    darkColorScheme(
        primary = Color(0xFF7C3AED),
        onPrimary = Color.White,
        primaryContainer = Color(0xFF24153B),
        onPrimaryContainer = Color(0xFFE8DEFF),
        secondary = Color(0xFFC026D3),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFF1B0B25),
        onSecondaryContainer = Color(0xFFF5F3FF),
        tertiary = Color(0xFF22D3EE),
        onTertiary = Color(0xFF05040A),
        tertiaryContainer = Color(0xFF100717),
        onTertiaryContainer = Color(0xFF22D3EE),
        background = Color(0xFF05040A),
        onBackground = Color.White,
        surface = Color(0xFF100717),
        onSurface = Color.White,
        surfaceVariant = Color(0xFF1B0B25),
        onSurfaceVariant = Color(0xFFA1A1AA),
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
    )

@Composable
fun JuzgonTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme =
        if (JuzgonThemeSelector.shouldUseDynamicColor(dynamicColor, Build.VERSION.SDK_INT)) {
            if (darkTheme) {
                dynamicDarkColorScheme(context)
            } else {
                dynamicLightColorScheme(context)
            }
        } else {
            JuzgonThemeSelector.fallbackColorScheme(darkTheme)
        }

    CompositionLocalProvider(
        LocalJuzgonVisualTokens provides JuzgonVisualTokenSelector.refreshTokens(),
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = MaterialTheme.typography,
            content = content,
        )
    }
}

internal object JuzgonThemeSelector {
    fun shouldUseDynamicColor(
        dynamicColor: Boolean,
        sdkInt: Int,
    ): Boolean = dynamicColor && sdkInt >= Build.VERSION_CODES.S

    fun fallbackColorScheme(darkTheme: Boolean): ColorScheme =
        if (darkTheme) {
            JuzgonDarkColorScheme
        } else {
            JuzgonLightColorScheme
        }
}
