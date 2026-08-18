package com.lockplay.lyrics

import java.net.URLEncoder
import kotlin.math.abs

/**
 * Matching, request-building, failure classification and gesture/privacy gating for lyrics.
 *
 * Compose-free (A13). Consumed by the lyrics fetcher (URL + acceptance + status) and by the
 * lockscreen overlay (hint and axis-lock decisions). Performs no I/O itself.
 */

private const val LrclibGetBase = "https://lrclib.net/api/get"

// ponytail: 2s is a starting guess chosen to absorb encoder/gapless rounding — tune it from
// observed match rates once real mismatches show up, don't theorise it.
private const val MatchToleranceMs = 2_000L

private val WhitespaceRunRegex = Regex("""\s+""")

/** Trailing `(...)` or `[...]` qualifier, which we only drop when it holds a known noise word. */
private val TrailingQualifierRegex = Regex("""\s*[(\[][^()\[\]]*[)\]]$""")

private val NoiseQualifiers = listOf(
    "remaster",
    "remastered",
    "live",
    "deluxe",
    "explicit",
    "feat.",
    "featuring",
    "radio edit",
    "single version",
)

/**
 * Canonical form of a title or artist for comparison: lowercased, whitespace-collapsed, with
 * trailing edition/version qualifiers removed. Meaningful parentheticals are left alone.
 */
// ponytail: substring match on a fixed noise list — swap for a real normaliser only if match
// rate proves bad in practice.
fun normalizeForMatch(value: String): String {
    var result = value.lowercase().replace(WhitespaceRunRegex, " ").trim()
    while (true) {
        val match = TrailingQualifierRegex.find(result) ?: break
        if (NoiseQualifiers.none { it in match.value }) break
        result = result.removeRange(match.range).trim()
    }
    return result
}

/**
 * Whether a provider result whose duration is [resultDurationSec] seconds plausibly matches a
 * track of [requestedDurationMs] milliseconds.
 */
fun isAcceptableMatch(requestedDurationMs: Long, resultDurationSec: Long): Boolean {
    // No duration reported by the player is common; skipping the check accepts the result rather
    // than rejecting it, because failing here would disable lyrics for those players entirely.
    if (requestedDurationMs <= 0L) return true
    return abs(requestedDurationMs - resultDurationSec * 1_000L) <= MatchToleranceMs
}

/**
 * LRCLIB `get` URL for one track. `album_name` is omitted when blank and `duration` (seconds) is
 * omitted when unknown, since `duration=0` would be read as a real, never-matching value.
 */
fun lrclibGetUrl(title: String, artist: String, album: String, durationMs: Long): String {
    val params = ArrayList<String>(4)
    params += "artist_name=" + encode(artist)
    params += "track_name=" + encode(title)
    if (album.isNotBlank()) params += "album_name=" + encode(album)
    if (durationMs > 0L) params += "duration=" + (durationMs / 1_000L)
    return LrclibGetBase + "?" + params.joinToString("&")
}

/** URLEncoder emits `+` for a space, which is form encoding, not query-value encoding. */
private fun encode(value: String): String = URLEncoder.encode(value, "UTF-8").replace("+", "%20")

/**
 * Classifies an HTTP response code. 404 means the provider genuinely has no lyrics — a calm
 * state — and must stay distinct from a transport or server failure, which is retryable later.
 */
fun statusForHttpCode(code: Int): LyricsStatus = when (code) {
    200 -> LyricsStatus.Synced
    404 -> LyricsStatus.NotFound
    else -> LyricsStatus.Offline
}

/**
 * The privacy gate: the only place that authorises sending track metadata off-device. When the
 * setting is off, nothing is requested (a deliberate, opt-in carve-out from X3).
 */
fun shouldFetch(enabled: Boolean, title: String, artist: String): Boolean =
    enabled && title.isNotBlank() && artist.isNotBlank()

/** One-time discoverability hint: shown only while the feature is off and unseen. */
fun shouldShowHint(enabled: Boolean, hintSeen: Boolean): Boolean = !enabled && !hintSeen

/** One-time coach mark on the lyrics pill: shown only while the feature is on, unseen, and closed. */
fun shouldShowCoachMark(lyricsEnabled: Boolean, coachMarkSeen: Boolean, lyricsVisible: Boolean): Boolean =
    lyricsEnabled && !coachMarkSeen && !lyricsVisible

/**
 * Axis lock for the lyrics flip gesture: true when the horizontal flip claims the drag, false
 * when it falls through to the vertical swipe-to-unlock.
 */
fun shouldConsumeHorizontal(dragX: Float, dragY: Float): Boolean =
    // A perfect diagonal resolves to vertical: unlock is the app's most important gesture, so it
    // wins every ambiguous drag.
    abs(dragX) > abs(dragY)
