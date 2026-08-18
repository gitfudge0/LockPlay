package com.lockplay.lyrics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LyricsMatchTest {

    @Test
    fun `normalize lowercases trims collapses whitespace and strips noise qualifiers`() {
        assertEquals("song title", normalizeForMatch("  Song   Title  "))
        assertEquals("song title", normalizeForMatch("Song Title (Remastered)"))
        assertEquals("song title", normalizeForMatch("Song Title (Live)"))
        assertEquals("song title", normalizeForMatch("Song Title [Deluxe]"))
        assertEquals("song title", normalizeForMatch("Song Title (feat. Someone)"))
        assertEquals("song title", normalizeForMatch("Song Title (Radio Edit)"))
    }

    @Test
    fun `normalize preserves a meaningful parenthetical not on the noise list`() {
        assertEquals(
            "song title (acoustic version)",
            normalizeForMatch("Song Title (Acoustic Version)"),
        )
    }

    @Test
    fun `acceptable match skips the check entirely when requested duration is unknown`() {
        // requestedDurationMs <= 0 must be treated as a skipped check (true), not a failure.
        assertTrue(isAcceptableMatch(0L, 999L))
        assertTrue(isAcceptableMatch(-1L, 0L))
    }

    @Test
    fun `acceptable match honors the plus or minus two second tolerance boundary`() {
        assertTrue(isAcceptableMatch(200_000L, 200L))
        assertTrue(isAcceptableMatch(200_000L, 198L))
        assertTrue(isAcceptableMatch(200_000L, 202L))
        assertFalse(isAcceptableMatch(200_000L, 197L))
        assertFalse(isAcceptableMatch(200_000L, 203L))
    }

    @Test
    fun `lrclib url encodes spaces as percent20 never plus`() {
        val url = lrclibGetUrl("My Song", "My Artist", "", 0L)
        assertTrue(url.contains("track_name=My%20Song"))
        assertTrue(url.contains("artist_name=My%20Artist"))
        assertFalse(url.contains("+"))
    }

    @Test
    fun `lrclib url duration is seconds and omitted when unknown`() {
        val withDuration = lrclibGetUrl("t", "a", "", 205_000L)
        assertTrue(withDuration.contains("duration=205"))

        val zeroDuration = lrclibGetUrl("t", "a", "", 0L)
        assertFalse(zeroDuration.contains("duration="))

        val negativeDuration = lrclibGetUrl("t", "a", "", -1L)
        assertFalse(negativeDuration.contains("duration="))
    }

    @Test
    fun `lrclib url omits album when blank but includes it when present`() {
        val blankAlbum = lrclibGetUrl("t", "a", "  ", 0L)
        assertFalse(blankAlbum.contains("album_name="))

        val withAlbum = lrclibGetUrl("t", "a", "My Album", 0L)
        assertTrue(withAlbum.contains("album_name=My%20Album"))
    }

    @Test
    fun `lrclib url encodes special characters instead of injecting them raw`() {
        val url = lrclibGetUrl("Rock & Roll?", "O'Brien", "Café", 0L)
        assertFalse(url.contains(" "))
        assertTrue(url.contains("%26")) // &
        assertTrue(url.contains("%3F")) // ?
        assertTrue(url.contains("%27")) // '
        assertTrue(url.contains("%C3%A9")) // é
    }

    @Test
    fun `http status maps and keeps not found distinct from server failure states`() {
        assertEquals(LyricsStatus.Synced, statusForHttpCode(200))
        assertEquals(LyricsStatus.NotFound, statusForHttpCode(404))
        assertEquals(LyricsStatus.Offline, statusForHttpCode(500))
        assertEquals(LyricsStatus.Offline, statusForHttpCode(503))
        assertEquals(LyricsStatus.Offline, statusForHttpCode(429))
        assertTrue(statusForHttpCode(404) != statusForHttpCode(500))
    }

    @Test
    fun `disabled setting blocks fetch for every title and artist combination`() {
        assertFalse(shouldFetch(enabled = false, title = "", artist = ""))
        assertFalse(shouldFetch(enabled = false, title = "Song", artist = "Artist"))
        assertFalse(shouldFetch(enabled = false, title = "Song", artist = ""))
        assertFalse(shouldFetch(enabled = false, title = "", artist = "Artist"))
    }

    @Test
    fun `enabled fetch still requires both title and artist to be non-blank`() {
        assertFalse(shouldFetch(enabled = true, title = "  ", artist = "Artist"))
        assertFalse(shouldFetch(enabled = true, title = "Song", artist = "  "))
        assertTrue(shouldFetch(enabled = true, title = "Song", artist = "Artist"))
    }

    @Test
    fun `hint shows only while disabled and unseen`() {
        assertTrue(shouldShowHint(enabled = false, hintSeen = false))
        assertFalse(shouldShowHint(enabled = false, hintSeen = true))
        assertFalse(shouldShowHint(enabled = true, hintSeen = false))
        assertFalse(shouldShowHint(enabled = true, hintSeen = true))
    }

    @Test
    fun `coach mark shows only while enabled unseen and lyrics closed`() {
        assertTrue(shouldShowCoachMark(lyricsEnabled = true, coachMarkSeen = false, lyricsVisible = false))
        assertFalse(shouldShowCoachMark(lyricsEnabled = false, coachMarkSeen = false, lyricsVisible = false))
        assertFalse(shouldShowCoachMark(lyricsEnabled = true, coachMarkSeen = true, lyricsVisible = false))
        assertFalse(shouldShowCoachMark(lyricsEnabled = true, coachMarkSeen = false, lyricsVisible = true))
        assertFalse(shouldShowCoachMark(lyricsEnabled = true, coachMarkSeen = true, lyricsVisible = true))
        assertFalse(shouldShowCoachMark(lyricsEnabled = false, coachMarkSeen = true, lyricsVisible = true))
    }

    @Test
    fun `horizontal consume for clearly diagonal drags picks the dominant axis`() {
        assertTrue(shouldConsumeHorizontal(50f, 5f))
        assertFalse(shouldConsumeHorizontal(5f, 50f))
    }

    @Test
    fun `exact tie resolves to vertical because unlock must never lose`() {
        assertFalse(shouldConsumeHorizontal(20f, 20f))
    }

    @Test
    fun `axis decision uses magnitude of negative deltas not sign`() {
        assertTrue(shouldConsumeHorizontal(-50f, 5f))
        assertFalse(shouldConsumeHorizontal(-5f, -50f))
    }
}
