package com.lockplay.lyrics

import androidx.compose.runtime.Immutable

/**
 * One timestamped lyric line. [atMs] is milliseconds from track start.
 *
 * Produced by [parseLrc]; selected by [activeLineIndex].
 */
data class LyricLine(val atMs: Long, val text: String)

/**
 * What the lyrics overlay should currently render.
 *
 * [Synced] means [Lyrics.lines] is populated; [Plain] means only [Lyrics.plainText] is.
 * [NotFound] is a calm, normal state (the provider has no lyrics for this track), distinct
 * from [Offline] (we could not reach the provider). See [statusForHttpCode].
 */
enum class LyricsStatus { Idle, Loading, Synced, Plain, NotFound, Offline }

/** Where [Lyrics] came from, for attribution in the overlay. [None] is the default for anything status-only. */
enum class LyricsSource { None, Local, Lrclib }

/**
 * Lyrics state for the current track, read by the lyrics overlay and written by the lyrics
 * fetcher. Absence is [EMPTY], never null (A7).
 *
 * [Immutable] so Compose can skip recomposition when an unchanged instance is re-emitted.
 */
// ponytail: [lines] is a read-only List, not a truly immutable one, so the @Immutable promise
// rests on nobody mutating the backing list — upgrade to a persistent/immutable collection only
// if a caller ever needs to hand this class a list it still holds a mutable reference to.
@Immutable
data class Lyrics(
    val status: LyricsStatus,
    val lines: List<LyricLine>,
    val plainText: String,
    val source: LyricsSource = LyricsSource.None,
) {
    companion object {
        /** Nothing loaded and nothing requested — the sentinel for "no lyrics", mirroring `NowPlaying.EMPTY`. */
        val EMPTY = Lyrics(
            status = LyricsStatus.Idle,
            lines = emptyList(),
            plainText = "",
        )
    }
}
