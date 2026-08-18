package com.lockplay.lyrics

/**
 * LRC parsing and playback-position maths for the lyrics overlay.
 *
 * Compose-free (A13) so it can be exercised on the JVM. Consumed by the lyrics overlay and by
 * whatever fetches lyrics; produces [LyricLine] values for [Lyrics].
 */

/**
 * A run of leading LRC tags: `[mm:ss.xx]`, `[mm:ss:xx]` or `[mm:ss]`. Metadata tags such as
 * `[ar:Artist]` or `[length:03:21]` do not match because the minutes group is digits-only.
 */
private val TimeTagRegex = Regex("""\[(\d{1,3}):(\d{1,2})(?:[.:](\d{1,3}))?]""")

/**
 * Parses LRC text into lines sorted ascending by [LyricLine.atMs].
 *
 * Anything malformed (unclosed or non-numeric tags, untimed or blank lines) is skipped rather
 * than failing the whole file; a timestamp with empty text is kept, because those mark
 * instrumental gaps. Never throws.
 */
fun parseLrc(raw: String): List<LyricLine> {
    if (raw.isBlank()) return emptyList()
    val out = ArrayList<LyricLine>()
    for (rawLine in raw.split('\n')) {
        val line = rawLine.trimEnd('\r')
        var cursor = 0
        val stamps = ArrayList<Long>()
        while (true) {
            val match = TimeTagRegex.find(line, cursor) ?: break
            // Only tags at the very front of the line are timestamps; a `[` later in the line is text.
            if (match.range.first != cursor) break
            stamps += timestampMs(match)
            cursor = match.range.last + 1
        }
        if (stamps.isEmpty()) continue
        val text = line.substring(cursor).trim()
        for (atMs in stamps) out += LyricLine(atMs, text)
    }
    out.sortBy { it.atMs }
    return out
}

/**
 * Milliseconds for one matched time tag. The fractional group is centiseconds in the 2-digit
 * form (`.34` is 340 ms, not 34) and milliseconds in the 3-digit form.
 */
private fun timestampMs(match: MatchResult): Long {
    val minutes = match.groupValues[1].toLong()
    val seconds = match.groupValues[2].toLong()
    val fraction = match.groupValues[3]
    val fractionMs = when (fraction.length) {
        1 -> fraction.toLong() * 100L
        2 -> fraction.toLong() * 10L
        3 -> fraction.toLong()
        else -> 0L
    }
    return minutes * 60_000L + seconds * 1_000L + fractionMs
}

/**
 * Index of the last line in [lines] (assumed sorted, as [parseLrc] returns) whose
 * [LyricLine.atMs] is `<= positionMs`, or -1 when no line is active yet.
 *
 * Binary search: this runs every frame.
 */
fun activeLineIndex(lines: List<LyricLine>, positionMs: Long): Int {
    if (lines.isEmpty() || positionMs < 0L) return -1
    var low = 0
    var high = lines.size - 1
    var found = -1
    while (low <= high) {
        val mid = (low + high) ushr 1
        if (lines[mid].atMs <= positionMs) {
            found = mid
            low = mid + 1
        } else {
            high = mid - 1
        }
    }
    return found
}

/** Normalises a raw playback position for lyric lookup: never negative, never past the track. */
fun lyricsPositionMs(positionMs: Long, durationMs: Long): Long {
    val floored = positionMs.coerceAtLeast(0L)
    // durationMs == 0 means the player reported no duration, which is common. Clamping to it
    // would pin every lyric to line one forever, so only clamp when a real duration exists.
    return if (durationMs > 0L) floored.coerceAtMost(durationMs) else floored
}
