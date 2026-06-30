package com.musiclock.ui.gallery

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.musiclock.R
import com.musiclock.design.AppTheme
import com.musiclock.design.BuiltInThemes
import com.musiclock.design.ThemeController
import com.musiclock.design.ThemeSpec
import com.musiclock.design.components.AppIconButton
import com.musiclock.design.components.AppText
import com.musiclock.model.NowPlaying
import com.musiclock.ui.lockscreen.skin.BuiltInSkins
import com.musiclock.ui.lockscreen.skin.DefaultSkin
import com.musiclock.ui.lockscreen.skin.PlayerSkin
import com.musiclock.ui.lockscreen.skin.SkinController
import com.musiclock.ui.lockscreen.skin.SkinScope
import kotlinx.coroutines.launch

/**
 * The app home after onboarding: a horizontally-snapping carousel of every [BuiltInSkins] entry,
 * each rendered as a live, scaled-down [PlayerSkin] preview driven by sample [SampleTracks] data
 * (the same technique the onboarding peek uses, re-implemented here so it owns no onboarding code).
 * Tapping a card persists the choice through [SkinController.select]; the overflow menu switches the
 * app-wide [ThemeSpec] through [ThemeController]. Both selections are collected via
 * `collectAsStateWithLifecycle`, and previews share one static position so 19 cards don't each tick.
 *
 * @param skinController source of truth for the selected lockscreen player.
 * @param themeController source of truth for the app-wide color theme.
 */
@Composable
fun PlayerGalleryScreen(
    skinController: SkinController,
    themeController: ThemeController,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val selectedSkin by skinController.skin.collectAsStateWithLifecycle(initialValue = DefaultSkin)
    val theme by themeController.theme.collectAsStateWithLifecycle(initialValue = BuiltInThemes.first())

    // One gradient brush per sample track, remembered so album-art placeholders survive recomposition.
    val accent = AppTheme.colors.accent
    val accentStrong = AppTheme.colors.accentStrong
    val sheet = AppTheme.colors.surfaceSheet
    val artBrushes = remember(accent, accentStrong, sheet) {
        listOf(
            Brush.linearGradient(listOf(accent, accentStrong, sheet)),
            Brush.linearGradient(listOf(accentStrong, sheet, accent)),
            Brush.linearGradient(listOf(sheet, accent, accentStrong)),
        )
    }

    val listState = rememberLazyListState()
    val flingBehavior = rememberSnapFlingBehavior(listState)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppTheme.colors.background),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.lg),
    ) {
        GalleryHeader(
            currentTheme = theme,
            onThemeSelected = { spec -> scope.launch { themeController.select(spec) } },
            modifier = Modifier.padding(
                start = AppTheme.spacing.screenPadding,
                end = AppTheme.spacing.screenPadding,
                top = AppTheme.spacing.lg,
            ),
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
            modifier = Modifier.padding(horizontal = AppTheme.spacing.screenPadding),
        ) {
            AppText("YOUR LOCKSCREEN PLAYER", AppTheme.typography.label, color = AppTheme.colors.accentOnSurface)
            AppText("Choose your player", AppTheme.typography.display, color = AppTheme.colors.textPrimary)
            AppText(
                "Swipe through the looks and tap one to make it your lock screen player.",
                AppTheme.typography.body,
                color = AppTheme.colors.textSecondary,
            )
        }

        LazyRow(
            state = listState,
            flingBehavior = flingBehavior,
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
            contentPadding = PaddingValues(horizontal = AppTheme.spacing.screenPadding),
        ) {
            items(BuiltInSkins, key = { it.id }) { skin ->
                val index = BuiltInSkins.indexOf(skin)
                SkinCard(
                    skin = skin,
                    track = SampleTracks[index % SampleTracks.size],
                    artBrush = artBrushes[index % artBrushes.size],
                    selected = skin.id == selectedSkin.id,
                    onClick = { scope.launch { skinController.select(skin) } },
                    modifier = Modifier.fillParentMaxWidth(GalleryDefaults.CardWidthFraction),
                )
            }
        }

        Spacer(Modifier.weight(1f))

        GalleryFooter(
            selectedName = selectedSkin.displayName,
            modifier = Modifier.padding(
                start = AppTheme.spacing.screenPadding,
                end = AppTheme.spacing.screenPadding,
                bottom = AppTheme.spacing.xl,
            ),
        )
    }
}

// --- Header -----------------------------------------------------------------------------------

@Composable
private fun GalleryHeader(
    currentTheme: ThemeSpec,
    onThemeSelected: (ThemeSpec) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_logo_mark),
            contentDescription = null,
            tint = AppTheme.colors.accent,
            modifier = Modifier.size(AppTheme.spacing.xl),
        )
        AppText("MusicLock", AppTheme.typography.title, color = AppTheme.colors.textPrimary)
        Spacer(Modifier.weight(1f))

        Box {
            var open by remember { mutableStateOf(false) }
            AppIconButton(
                icon = Icons.Filled.MoreHoriz,
                contentDescription = "More options",
                onClick = { open = true },
                tint = AppTheme.colors.textSecondary,
            )
            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                BuiltInThemes.forEach { spec ->
                    DropdownMenuItem(
                        text = {
                            AppText(
                                spec.displayName,
                                AppTheme.typography.body,
                                color = if (spec.id == currentTheme.id) AppTheme.colors.accentOnSurface
                                else AppTheme.colors.textPrimary,
                            )
                        },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(AppTheme.spacing.md)
                                    .clip(CircleShape)
                                    .background(spec.colors.accent),
                            )
                        },
                        trailingIcon = {
                            if (spec.id == currentTheme.id) {
                                Icon(
                                    Icons.Filled.Check,
                                    contentDescription = null,
                                    tint = AppTheme.colors.accentOnSurface,
                                    modifier = Modifier.size(AppTheme.spacing.md),
                                )
                            }
                        },
                        onClick = {
                            onThemeSelected(spec)
                            open = false
                        },
                    )
                }
            }
        }
    }
}

// --- Card -------------------------------------------------------------------------------------

@Composable
private fun SkinCard(
    skin: PlayerSkin,
    track: NowPlaying,
    artBrush: Brush,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // A single static position for every preview — no per-card ticking clock.
    val previewScope = remember(skin, track) {
        SkinScope(
            state = track,
            position = { 0L },
            onSeek = {}, onPrev = {}, onPlayPause = {}, onNext = {},
        )
    }
    Column(
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
        modifier = modifier.clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(GalleryDefaults.FrameAspectRatio)
                .clip(AppTheme.shapes.large)
                .background(AppTheme.colors.surfaceElevated)
                .then(
                    if (selected) Modifier.border(
                        AppTheme.spacing.xs,
                        AppTheme.colors.accent,
                        AppTheme.shapes.large,
                    ) else Modifier,
                )
                .padding(AppTheme.spacing.xs)
                .clip(AppTheme.shapes.medium),
        ) {
            // Gradient album-art placeholder, then the live skin painted over it.
            Box(modifier = Modifier.fillMaxSize().background(artBrush))
            Box(modifier = Modifier.fillMaxSize()) { skin.content(previewScope) }

            if (selected) {
                InUsePill(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(AppTheme.spacing.sm),
                )
            }
        }
        AppText(
            skin.displayName,
            AppTheme.typography.artist,
            color = AppTheme.colors.textPrimary,
            maxLines = 1,
        )
        AppText(
            if (selected) "Currently in use" else "Tap to use this player",
            AppTheme.typography.timestamp,
            color = if (selected) AppTheme.colors.accentOnSurface else AppTheme.colors.textSecondary,
            maxLines = 1,
        )
    }
}

@Composable
private fun InUsePill(modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs),
        modifier = modifier
            .background(AppTheme.colors.accent, AppTheme.shapes.pill)
            .padding(horizontal = AppTheme.spacing.sm, vertical = AppTheme.spacing.xs),
    ) {
        Icon(
            Icons.Filled.Check,
            contentDescription = null,
            tint = AppTheme.colors.textOnAccent,
            modifier = Modifier.size(AppTheme.spacing.md),
        )
        AppText("In use", AppTheme.typography.label, color = AppTheme.colors.textOnAccent)
    }
}

// --- Footer -----------------------------------------------------------------------------------

@Composable
private fun GalleryFooter(selectedName: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
    ) {
        Box(
            modifier = Modifier
                .size(AppTheme.spacing.sm)
                .clip(CircleShape)
                .background(AppTheme.colors.accent),
        )
        AppText(
            "$selectedName is your lockscreen player",
            AppTheme.typography.body,
            color = AppTheme.colors.textSecondary,
            modifier = Modifier.weight(1f),
        )
    }
}

// --- Sample data & defaults -------------------------------------------------------------------

/** Layout constants for the carousel; relative/aspect values so the screen stays token-only on size. */
private object GalleryDefaults {
    const val CardWidthFraction = 0.66f
    const val FrameAspectRatio = 0.62f
}

/**
 * Hardcoded preview tracks for the gallery (no real media). Hoisted to a top-level constant so the
 * list is allocated once, never per recomposition. Album art is left null — each card paints a
 * remembered gradient placeholder behind the live skin preview.
 */
val SampleTracks: List<NowPlaying> = listOf(
    NowPlaying(
        isActive = true,
        title = "Midnight City",
        artist = "M83",
        isPlaying = true,
        positionMs = 78_000L,
        durationMs = 240_000L,
    ),
    NowPlaying(
        isActive = true,
        title = "Redbone",
        artist = "Childish Gambino",
        isPlaying = true,
        positionMs = 132_000L,
        durationMs = 327_000L,
    ),
    NowPlaying(
        isActive = true,
        title = "Nightcall",
        artist = "Kavinsky",
        isPlaying = true,
        positionMs = 54_000L,
        durationMs = 258_000L,
    ),
)
