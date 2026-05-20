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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

internal val JuzgonLightColorScheme =
    lightColorScheme(
        primary = Color(0xFF2E6F5E),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFB4F1DC),
        onPrimaryContainer = Color(0xFF002118),
        secondary = Color(0xFF4C6359),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFCFE9DC),
        onSecondaryContainer = Color(0xFF092016),
        tertiary = Color(0xFF3F6375),
        onTertiary = Color.White,
        tertiaryContainer = Color(0xFFC3E7FD),
        onTertiaryContainer = Color(0xFF001F2A),
        background = Color(0xFFFBFDF9),
        onBackground = Color(0xFF191C1A),
        surface = Color(0xFFFBFDF9),
        onSurface = Color(0xFF191C1A),
        error = Color(0xFFBA1A1A),
        onError = Color.White,
    )

internal val JuzgonDarkColorScheme =
    darkColorScheme(
        primary = Color(0xFF99D7C1),
        onPrimary = Color(0xFF00382B),
        primaryContainer = Color(0xFF0F5141),
        onPrimaryContainer = Color(0xFFB4F1DC),
        secondary = Color(0xFFB3CCBF),
        onSecondary = Color(0xFF1F352C),
        secondaryContainer = Color(0xFF354B41),
        onSecondaryContainer = Color(0xFFCFE9DC),
        tertiary = Color(0xFFA7CBE0),
        onTertiary = Color(0xFF0A3445),
        tertiaryContainer = Color(0xFF274B5C),
        onTertiaryContainer = Color(0xFFC3E7FD),
        background = Color(0xFF111412),
        onBackground = Color(0xFFE1E3DF),
        surface = Color(0xFF111412),
        onSurface = Color(0xFFE1E3DF),
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
    )

@Composable
fun JuzgonTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
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

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MaterialTheme.typography,
        content = content,
    )
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
