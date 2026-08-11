package com.lockplay.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lockplay.design.AppTheme

/**
 * Token-only building blocks. Every surface in the app is composed from these, so restyling a
 * [com.lockplay.design.ThemeSpec] restyles every screen. No literal colors/sizes live in feature code.
 */

@Composable
fun AppText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
    color: Color = Color.Unspecified,
    textAlign: TextAlign? = null,
) {
    Text(
        text = text,
        style = style,
        color = color,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        textAlign = textAlign,
        modifier = modifier,
    )
}

/** A themed surface card (rounded, filled with the surface token). */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .background(AppTheme.colors.surface, AppTheme.shapes.medium)
            .padding(AppTheme.spacing.md),
    ) { content() }
}

/** Round, borderless control button driven entirely by tokens (used for media controls). */
@Composable
fun AppIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = AppTheme.spacing.controlSize,
    tint: Color = AppTheme.colors.controlIcon,
    background: Color = Color.Transparent,
) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .background(background, AppTheme.shapes.pill)
            .clickable(
                interactionSource = interaction,
                indication = androidx.compose.material3.ripple(bounded = false),
                onClick = onClick,
            ),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(size * 0.5f),
        )
    }
}

/** Visual styles for [AppButton]: filled accent vs. transparent text-only. */
enum class ButtonVariant { Primary, Text }

/** Button heights. [Lg] is the 52dp tall onboarding CTA. */
enum class ButtonSize { Md, Lg }

/**
 * Pill-shaped action button (used in onboarding). Defaults preserve the original filled-accent,
 * wrap-content behavior so existing call sites keep working.
 */
@Composable
fun AppButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    variant: ButtonVariant = ButtonVariant.Primary,
    block: Boolean = false,
    size: ButtonSize = ButtonSize.Md,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
) {
    val isText = variant == ButtonVariant.Text
    val bg = when {
        isText -> Color.Transparent
        enabled -> AppTheme.colors.primary
        else -> AppTheme.colors.surfaceVariant
    }
    val fg = when {
        isText -> if (enabled) AppTheme.colors.accentOnSurface else AppTheme.colors.textTertiary
        enabled -> AppTheme.colors.textOnAccent
        else -> AppTheme.colors.textTertiary
    }
    val heightMod = if (size == ButtonSize.Lg) Modifier.height(AppTheme.spacing.controlSize) else Modifier
    val widthMod = if (block) Modifier.fillMaxWidth() else Modifier
    Row(
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .then(widthMod)
            .then(heightMod)
            .background(bg, AppTheme.shapes.pill)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = AppTheme.spacing.xl, vertical = AppTheme.spacing.md),
    ) {
        if (leadingIcon != null) {
            Icon(leadingIcon, contentDescription = null, tint = fg, modifier = Modifier.size(AppTheme.spacing.md))
        }
        AppText(label, AppTheme.typography.button, color = fg)
        if (trailingIcon != null) {
            Icon(trailingIcon, contentDescription = null, tint = fg, modifier = Modifier.size(AppTheme.spacing.md))
        }
    }
}

/** Tones for [AppChip]: accent-muted, positive-tinted, or bordered/neutral. */
enum class ChipTone { Accent, Positive, Outline }

/** Small status pill used for "Required" tags and summary status. */
@Composable
fun AppChip(
    text: String,
    tone: ChipTone,
    modifier: Modifier = Modifier,
) {
    val bg = when (tone) {
        ChipTone.Accent -> AppTheme.colors.accentMuted
        ChipTone.Positive -> AppTheme.colors.positive.copy(alpha = 0.16f)
        ChipTone.Outline -> Color.Transparent
    }
    val fg = when (tone) {
        ChipTone.Accent -> AppTheme.colors.accentOnSurface
        ChipTone.Positive -> AppTheme.colors.positive
        ChipTone.Outline -> AppTheme.colors.textSecondary
    }
    val border = if (tone == ChipTone.Outline) {
        Modifier.border(1.dp, AppTheme.colors.borderStrong, AppTheme.shapes.pill)
    } else {
        Modifier
    }
    Box(
        modifier = modifier
            .background(bg, AppTheme.shapes.pill)
            .then(border)
            .padding(horizontal = AppTheme.spacing.sm, vertical = AppTheme.spacing.xs),
    ) {
        AppText(text, AppTheme.typography.label, color = fg)
    }
}

/** Row of thin segment bars at the top of the onboarding wizard. */
@Composable
fun ProgressSegments(
    total: Int,
    currentIndex: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs),
        modifier = modifier.fillMaxWidth(),
    ) {
        repeat(total) { i ->
            val color = when {
                i < currentIndex -> AppTheme.colors.accentOnSurface
                i == currentIndex -> AppTheme.colors.accent
                else -> AppTheme.colors.fillGhostStrong
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(AppTheme.spacing.xs)
                    .background(color, AppTheme.shapes.pill),
            )
        }
    }
}

/** Bordered secondary/outline button. */
@Composable
fun AppOutlineButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .border(1.dp, AppTheme.colors.textTertiary, AppTheme.shapes.pill)
            .clickable(onClick = onClick)
            .padding(horizontal = AppTheme.spacing.xl, vertical = AppTheme.spacing.md),
    ) {
        AppText(label, AppTheme.typography.button, color = AppTheme.colors.textPrimary)
    }
}

/** Full-screen themed background fill (used by SOLID themes and as the base under GLASS). */
@Composable
fun SolidBackground(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppTheme.colors.background),
    ) { content() }
}
