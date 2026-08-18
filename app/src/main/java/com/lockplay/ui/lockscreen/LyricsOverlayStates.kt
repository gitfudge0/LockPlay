package com.lockplay.ui.lockscreen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.lockplay.design.AppTheme

/**
 * The non-[com.lockplay.lyrics.LyricsStatus.Synced] faces of [LyricsOverlay], split out of
 * `LyricsOverlay.kt` purely to keep that file under the 400-line ceiling (S9). Same package, same
 * visibility, no abstraction introduced — mechanical division only.
 *
 * Everything here reads tokens through [AppTheme] (S7) and never logs a word of the lyrics (X3).
 */

/** Unsynced lyrics: readable prose, no highlighting and no seek — there is no timing to seek to. */
@Composable
internal fun PlainLyrics(plainText: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(AppTheme.spacing.screenPadding),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
    ) {
        QuietPill(text = "Not synced for this track")
        Text(
            text = plainText,
            style = AppTheme.typography.body,
            color = AppTheme.colors.textSecondary,
        )
    }
}

/** Deliberately quiet: a spinner on a lockscreen reads as an error, and the wait is usually short. */
@Composable
internal fun LoadingLyrics() {
    CenteredState {
        Text(
            text = "Looking for lyrics",
            style = AppTheme.typography.label,
            color = AppTheme.colors.textTertiary,
            textAlign = TextAlign.Center,
        )
    }
}

/** A normal outcome, not a failure — most live takes and remixes simply are not in the database. */
@Composable
internal fun NotFoundLyrics() {
    CenteredState {
        StateHeadline("No lyrics for this one")
        StateBody(
            "LRCLIB doesn't have a match for this title and artist. " +
                "That's common for live takes, remixes and small releases.",
        )
    }
}

/** Distinct from [NotFoundLyrics] on purpose: this one fixes itself, so the copy says so. */
@Composable
internal fun OfflineLyrics() {
    CenteredState {
        StateHeadline("No connection")
        StateBody("Lyrics will load by themselves once you're back online.")
    }
}

/**
 * Shown in every state, including Idle: the match is by title and artist, so it can be wrong, and
 * the user is owed both the source and that caveat.
 */
@Composable
internal fun LyricsAttribution(modifier: Modifier = Modifier) {
    Text(
        text = "Lyrics from LRCLIB — matched by title and artist, so they may be wrong",
        style = AppTheme.typography.timestamp,
        color = AppTheme.colors.textTertiary,
        textAlign = TextAlign.Center,
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
private fun CenteredState(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(AppTheme.spacing.screenPadding),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
        ) {
            content()
        }
    }
}

@Composable
private fun StateHeadline(text: String) {
    Text(
        text = text,
        style = AppTheme.typography.title,
        color = AppTheme.colors.textPrimary,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun StateBody(text: String) {
    Text(
        text = text,
        style = AppTheme.typography.body,
        color = AppTheme.colors.textSecondary,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun QuietPill(text: String) {
    Text(
        text = text,
        style = AppTheme.typography.label,
        color = AppTheme.colors.textTertiary,
        modifier = Modifier
            .background(AppTheme.colors.fillGhost, AppTheme.shapes.pill)
            .padding(horizontal = AppTheme.spacing.md, vertical = AppTheme.spacing.sm),
    )
}
