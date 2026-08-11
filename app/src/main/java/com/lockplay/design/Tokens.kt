package com.lockplay.design

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp

/**
 * The design-token contract for the ENTIRE app.
 *
 * Every composable reads color/type/shape/spacing/motion from these token groups via the [AppTheme]
 * accessor — never from hardcoded literals or MaterialTheme. Swapping one [ThemeSpec] therefore
 * restyles the whole app. New visual directions are added as new [ThemeSpec]s, not new components.
 */

@Immutable
data class ColorTokens(
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val primary: Color,
    val onPrimary: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val accent: Color,
    val scrim: Color,
    val sliderTrack: Color,
    val sliderActive: Color,
    val controlIcon: Color,
    // Semantic roles for onboarding/gallery surfaces (added in Phase 1).
    val accentStrong: Color,
    val accentMuted: Color,
    val accentOnSurface: Color,
    val textOnAccent: Color,
    val surfaceElevated: Color,
    val surfaceSheet: Color,
    val borderSubtle: Color,
    val borderStrong: Color,
    val fillGhost: Color,
    val fillGhostStrong: Color,
    val positive: Color,
    val warning: Color,
    val critical: Color,
)

@Immutable
data class TypographyTokens(
    val display: TextStyle,
    val title: TextStyle,
    val artist: TextStyle,
    val label: TextStyle,
    val timestamp: TextStyle,
    val body: TextStyle,
    val button: TextStyle,
)

@Immutable
data class ShapeTokens(
    val small: Shape,
    val medium: Shape,
    val large: Shape,
    val pill: Shape,
    val albumArt: Shape,
)

@Immutable
data class SpacingTokens(
    val xs: Dp,
    val sm: Dp,
    val md: Dp,
    val lg: Dp,
    val xl: Dp,
    val xxl: Dp,
    val screenPadding: Dp,
    val controlSize: Dp,
    val playButtonSize: Dp,
)

@Immutable
data class MotionTokens(
    val fastMillis: Int,
    val mediumMillis: Int,
    val slowMillis: Int,
)

// staticCompositionLocalOf: tokens change rarely (only on theme switch), so we avoid the
// read-tracking overhead of compositionLocalOf. No defaults — reading outside AppTheme is a bug.
val LocalColorTokens = staticCompositionLocalOf<ColorTokens> { error("No ColorTokens; wrap in AppTheme") }
val LocalTypographyTokens = staticCompositionLocalOf<TypographyTokens> { error("No TypographyTokens; wrap in AppTheme") }
val LocalShapeTokens = staticCompositionLocalOf<ShapeTokens> { error("No ShapeTokens; wrap in AppTheme") }
val LocalSpacingTokens = staticCompositionLocalOf<SpacingTokens> { error("No SpacingTokens; wrap in AppTheme") }
val LocalMotionTokens = staticCompositionLocalOf<MotionTokens> { error("No MotionTokens; wrap in AppTheme") }
val LocalBackgroundStyle = staticCompositionLocalOf<BackgroundStyle> { error("No BackgroundStyle; wrap in AppTheme") }
