package com.juzgon.ui.theme

import androidx.compose.animation.core.EaseIn
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.Easing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private const val BASE_BACKGROUND = 0xFF05040A
private const val BACKGROUND_ELEVATED = 0xFF100717
private const val BACKGROUND_PANEL = 0xFF1B0B25
private const val PRIMARY_GLOW = 0xFFC026D3
private const val PRIMARY_GLOW_STRONG = 0xFFD946EF
private const val SECONDARY_GLOW = 0xFF7C3AED
private const val SECONDARY_GLOW_SOFT = 0xFF8B5CF6
private const val SECONDARY_GLOW_STRONG = 0xFFA855F7
private const val CONTRAST_ACCENT = 0xFF22D3EE
private const val CONTRAST_ACCENT_SOFT = 0xFF2DD4BF
private const val RATING_ACCENT = 0xFFFBBF24
private const val RATING_ACCENT_WARM = 0xFFFDBA74
private const val TEXT_STRONG = 0xFFFFFFFF
private const val TEXT_SOFT = 0xFFF5F3FF
private const val TEXT_MUTED = 0xFFA1A1AA
private const val CARD_CORNER_RADIUS_DP = 20
private const val PILL_CORNER_RADIUS_DP = 28
private const val AVATAR_CORNER_RADIUS_DP = 999
private const val SPACING_EXTRA_SMALL_DP = 4
private const val SPACING_SMALL_DP = 8
private const val SPACING_MEDIUM_DP = 12
private const val SPACING_LARGE_DP = 16
private const val SPACING_EXTRA_LARGE_DP = 24
private const val ANIMATION_SHORT_DURATION_MS = 150
private const val ANIMATION_MEDIUM_DURATION_MS = 300
private const val ANIMATION_LONG_DURATION_MS = 500
private const val GLOW_RADIUS_SMALL_DP = 4
private const val GLOW_RADIUS_MEDIUM_DP = 8
private const val GLOW_RADIUS_LARGE_DP = 16

@Immutable
internal data class JuzgonVisualTokens(
    val palette: JuzgonVisualPalette,
    val gradients: JuzgonVisualGradients,
    val shapes: JuzgonVisualShapes,
    val spacing: JuzgonVisualSpacing,
    val animations: JuzgonVisualAnimations,
    val glow: JuzgonVisualGlow,
)

@Immutable
internal data class JuzgonVisualPalette(
    val baseBackground: Color,
    val elevatedBackground: Color,
    val panelBackground: Color,
    val primaryGlow: Color,
    val primaryGlowStrong: Color,
    val secondaryGlow: Color,
    val secondaryGlowSoft: Color,
    val secondaryGlowStrong: Color,
    val contrastAccent: Color,
    val contrastAccentSoft: Color,
    val ratingAccent: Color,
    val ratingAccentWarm: Color,
    val textStrong: Color,
    val textSoft: Color,
    val textMuted: Color,
)

@Immutable
internal data class JuzgonVisualGradients(
    val heroSurface: List<Color>,
    val glowBorder: List<Color>,
    val scoreBar: List<Color>,
    val glowRing: List<Color>,
)

@Immutable
internal data class JuzgonVisualShapes(
    val cardCornerRadius: Dp,
    val pillCornerRadius: Dp,
    val avatarCornerRadius: Dp,
)

@Immutable
internal data class JuzgonVisualSpacing(
    val extraSmall: Dp,
    val small: Dp,
    val medium: Dp,
    val large: Dp,
    val extraLarge: Dp,
)

@Immutable
internal data class JuzgonVisualAnimations(
    val shortDuration: Int,
    val mediumDuration: Int,
    val longDuration: Int,
    val enterEasing: Easing,
    val exitEasing: Easing,
)

@Immutable
internal data class JuzgonVisualGlow(
    val radiusSmall: Dp,
    val radiusMedium: Dp,
    val radiusLarge: Dp,
)

internal object JuzgonVisualTokenSelector {
    fun refreshTokens(): JuzgonVisualTokens = JuzgonRefreshVisualTokens
}

internal object JuzgonVisualTheme {
    val tokens: JuzgonVisualTokens
        @Composable
        @ReadOnlyComposable
        get() = LocalJuzgonVisualTokens.current
}

internal val LocalJuzgonVisualTokens =
    staticCompositionLocalOf { JuzgonVisualTokenSelector.refreshTokens() }

private val JuzgonRefreshVisualPalette =
    JuzgonVisualPalette(
        baseBackground = Color(BASE_BACKGROUND),
        elevatedBackground = Color(BACKGROUND_ELEVATED),
        panelBackground = Color(BACKGROUND_PANEL),
        primaryGlow = Color(PRIMARY_GLOW),
        primaryGlowStrong = Color(PRIMARY_GLOW_STRONG),
        secondaryGlow = Color(SECONDARY_GLOW),
        secondaryGlowSoft = Color(SECONDARY_GLOW_SOFT),
        secondaryGlowStrong = Color(SECONDARY_GLOW_STRONG),
        contrastAccent = Color(CONTRAST_ACCENT),
        contrastAccentSoft = Color(CONTRAST_ACCENT_SOFT),
        ratingAccent = Color(RATING_ACCENT),
        ratingAccentWarm = Color(RATING_ACCENT_WARM),
        textStrong = Color(TEXT_STRONG),
        textSoft = Color(TEXT_SOFT),
        textMuted = Color(TEXT_MUTED),
    )

private val JuzgonRefreshVisualTokens =
    JuzgonVisualTokens(
        palette = JuzgonRefreshVisualPalette,
        gradients =
            JuzgonVisualGradients(
                heroSurface =
                    listOf(
                        JuzgonRefreshVisualPalette.baseBackground,
                        JuzgonRefreshVisualPalette.panelBackground,
                        JuzgonRefreshVisualPalette.primaryGlow,
                    ),
                glowBorder =
                    listOf(
                        JuzgonRefreshVisualPalette.secondaryGlow,
                        JuzgonRefreshVisualPalette.primaryGlowStrong,
                        JuzgonRefreshVisualPalette.contrastAccent,
                    ),
                scoreBar =
                    listOf(
                        JuzgonRefreshVisualPalette.secondaryGlow,
                        JuzgonRefreshVisualPalette.primaryGlowStrong,
                        JuzgonRefreshVisualPalette.ratingAccentWarm,
                    ),
                glowRing =
                    listOf(
                        JuzgonRefreshVisualPalette.primaryGlow,
                        JuzgonRefreshVisualPalette.secondaryGlowStrong,
                        JuzgonRefreshVisualPalette.contrastAccent,
                    ),
            ),
        shapes =
            JuzgonVisualShapes(
                cardCornerRadius = CARD_CORNER_RADIUS_DP.dp,
                pillCornerRadius = PILL_CORNER_RADIUS_DP.dp,
                avatarCornerRadius = AVATAR_CORNER_RADIUS_DP.dp,
            ),
        spacing =
            JuzgonVisualSpacing(
                extraSmall = SPACING_EXTRA_SMALL_DP.dp,
                small = SPACING_SMALL_DP.dp,
                medium = SPACING_MEDIUM_DP.dp,
                large = SPACING_LARGE_DP.dp,
                extraLarge = SPACING_EXTRA_LARGE_DP.dp,
            ),
        animations =
            JuzgonVisualAnimations(
                shortDuration = ANIMATION_SHORT_DURATION_MS,
                mediumDuration = ANIMATION_MEDIUM_DURATION_MS,
                longDuration = ANIMATION_LONG_DURATION_MS,
                enterEasing = EaseOut,
                exitEasing = EaseIn,
            ),
        glow =
            JuzgonVisualGlow(
                radiusSmall = GLOW_RADIUS_SMALL_DP.dp,
                radiusMedium = GLOW_RADIUS_MEDIUM_DP.dp,
                radiusLarge = GLOW_RADIUS_LARGE_DP.dp,
            ),
    )
