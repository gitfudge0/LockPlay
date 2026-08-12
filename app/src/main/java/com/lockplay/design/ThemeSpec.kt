package com.lockplay.design

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable

/** How the lockscreen paints its backdrop. Adding a new style here = a new look available to every theme. */
enum class BackgroundStyle { SOLID, GLASS }

/**
 * A complete, named visual identity for the app: one bundle of every token group.
 *
 * To add a new app-wide look, add a new [ThemeSpec] to [BuiltInThemes] — no component changes needed.
 */
@Immutable
data class ThemeSpec(
    val id: String,
    val displayName: String,
    val colors: ColorTokens,
    val typography: TypographyTokens,
    val shapes: ShapeTokens,
    val spacing: SpacingTokens,
    val motion: MotionTokens,
    val backgroundStyle: BackgroundStyle,
)

/**
 * Provides every token group to the composition. Wrap the whole app (and the lockscreen) in this.
 * Read tokens through the [AppTheme] accessor object below.
 */
@Composable
fun AppThemeProvider(spec: ThemeSpec, content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalColorTokens provides spec.colors,
        LocalTypographyTokens provides spec.typography,
        LocalShapeTokens provides spec.shapes,
        LocalSpacingTokens provides spec.spacing,
        LocalMotionTokens provides spec.motion,
        LocalBackgroundStyle provides spec.backgroundStyle,
        content = content,
    )
}

/** Ergonomic accessor, mirroring MaterialTheme.* — `AppTheme.colors.primary`, `AppTheme.spacing.md`. */
object AppTheme {
    val colors: ColorTokens
        @Composable @ReadOnlyComposable
        get() = LocalColorTokens.current
    val typography: TypographyTokens
        @Composable @ReadOnlyComposable
        get() = LocalTypographyTokens.current
    val shapes: ShapeTokens
        @Composable @ReadOnlyComposable
        get() = LocalShapeTokens.current
    val spacing: SpacingTokens
        @Composable @ReadOnlyComposable
        get() = LocalSpacingTokens.current
    val motion: MotionTokens
        @Composable @ReadOnlyComposable
        get() = LocalMotionTokens.current
    val backgroundStyle: BackgroundStyle
        @Composable @ReadOnlyComposable
        get() = LocalBackgroundStyle.current
}
