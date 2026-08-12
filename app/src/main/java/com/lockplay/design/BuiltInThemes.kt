package com.lockplay.design

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * The catalogue of app-wide looks. Add an entry to [BuiltInThemes] to introduce a new visual identity;
 * everything that reads tokens picks it up automatically.
 */

// Spacing and motion are shared across themes by default — override per-theme only when a look needs it.
private val defaultSpacing = SpacingTokens(
    xs = 4.dp, sm = 8.dp, md = 16.dp, lg = 24.dp, xl = 32.dp, xxl = 48.dp,
    screenPadding = 28.dp, controlSize = 52.dp, playButtonSize = 76.dp,
)

private val defaultMotion = MotionTokens(fastMillis = 150, mediumMillis = 300, slowMillis = 600)

private fun typography(
    primary: Color,
    secondary: Color,
    tertiary: Color,
) = TypographyTokens(
    display = TextStyle(
        fontSize = 31.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 36.sp,
        letterSpacing = (-0.31).sp,
        color = primary,
    ),
    title = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.Bold, color = primary),
    artist = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.Medium, color = secondary),
    label = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = tertiary),
    timestamp = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium, color = tertiary),
    body = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Normal, color = secondary),
    button = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = primary),
)

private fun shapes(albumArtCorner: Int) = ShapeTokens(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(28.dp),
    pill = RoundedCornerShape(percent = 50),
    albumArt = RoundedCornerShape(albumArtCorner.dp),
)

val MinimalistDark: ThemeSpec = run {
    // Ember-dark palette from the design's tokens/colors.css.
    val textPrimary = Color(0xFFF5F4F2)
    val textSecondary = Color(0xA8F5F4F2) // 66% of textPrimary
    val textTertiary = Color(0x6BF5F4F2) // 42% of textPrimary
    ThemeSpec(
        id = "minimalist_dark",
        displayName = "Minimalist Dark",
        colors = ColorTokens(
            background = Color(0xFF060607),
            surface = Color(0xFF0B0B0D),
            surfaceVariant = Color(0xFF1B1B1F),
            primary = Color(0xFFFF6B3D),
            onPrimary = Color(0xFF1A0E08),
            textPrimary = textPrimary,
            textSecondary = textSecondary,
            textTertiary = textTertiary,
            accent = Color(0xFFFF6B3D),
            scrim = Color(0xCC000000),
            sliderTrack = Color(0x24F5F4F2),
            sliderActive = Color(0xFFFF6B3D),
            controlIcon = textPrimary,
            accentStrong = Color(0xFFE5552A),
            accentMuted = Color(0x29FF6B3D), // rgba(255,107,61,.16)
            accentOnSurface = Color(0xFFFF8A63),
            textOnAccent = Color(0xFF1A0E08),
            surfaceElevated = Color(0xFF131316),
            surfaceSheet = Color(0xFF1B1B1F),
            borderSubtle = Color(0x1FF5F4F2), // 12%
            borderStrong = Color(0x38F5F4F2), // 22%
            fillGhost = Color(0x14F5F4F2), // 8%
            fillGhostStrong = Color(0x24F5F4F2), // 14%
            positive = Color(0xFF4ED08A),
            warning = Color(0xFFF5B544),
            critical = Color(0xFFFF5C5C),
        ),
        typography = typography(textPrimary, textSecondary, textTertiary),
        shapes = shapes(albumArtCorner = 20),
        spacing = defaultSpacing,
        motion = defaultMotion,
        backgroundStyle = BackgroundStyle.SOLID,
    )
}

val Glassmorphism: ThemeSpec = run {
    val textPrimary = Color(0xFFFFFFFF)
    val textSecondary = Color(0xCCFFFFFF)
    val textTertiary = Color(0x99FFFFFF)
    ThemeSpec(
        id = "glassmorphism",
        displayName = "Glassmorphism",
        colors = ColorTokens(
            background = Color(0xFF000000),
            surface = Color(0x33FFFFFF),
            surfaceVariant = Color(0x22FFFFFF),
            primary = Color(0xFFFFFFFF),
            onPrimary = Color(0xFF000000),
            textPrimary = textPrimary,
            textSecondary = textSecondary,
            textTertiary = textTertiary,
            accent = Color(0xFFFFFFFF),
            scrim = Color(0x66000000),
            sliderTrack = Color(0x44FFFFFF),
            sliderActive = Color(0xFFFFFFFF),
            controlIcon = textPrimary,
            accentStrong = Color(0xFFFFFFFF),
            accentMuted = Color(0x33FFFFFF),
            accentOnSurface = Color(0xFFFFFFFF),
            textOnAccent = Color(0xFF000000),
            surfaceElevated = Color(0x3DFFFFFF),
            surfaceSheet = Color(0x4DFFFFFF),
            borderSubtle = Color(0x1FFFFFFF),
            borderStrong = Color(0x40FFFFFF),
            fillGhost = Color(0x14FFFFFF),
            fillGhostStrong = Color(0x24FFFFFF),
            positive = Color(0xFF4ED08A),
            warning = Color(0xFFF5B544),
            critical = Color(0xFFFF5C5C),
        ),
        typography = typography(textPrimary, textSecondary, textTertiary),
        shapes = shapes(albumArtCorner = 24),
        spacing = defaultSpacing,
        motion = defaultMotion,
        backgroundStyle = BackgroundStyle.GLASS,
    )
}

/** Source of truth for selectable themes. Order is the order shown in the picker. */
val BuiltInThemes: List<ThemeSpec> = listOf(MinimalistDark, Glassmorphism)

val DefaultTheme: ThemeSpec = MinimalistDark

fun themeById(id: String?): ThemeSpec = BuiltInThemes.firstOrNull { it.id == id } ?: DefaultTheme
