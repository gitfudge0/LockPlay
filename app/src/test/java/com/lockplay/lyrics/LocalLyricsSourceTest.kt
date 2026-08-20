package com.lockplay.lyrics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalLyricsSourceTest {

    @Test
    fun `album subfolder candidate comes before the flat folder candidate`() {
        assertEquals(
            listOf("primary:Music/Album/song.lrc", "primary:Music/song.lrc"),
            lrcDocIdCandidates("primary:Music", "Music/Album/", "song.lrc"),
        )
    }

    @Test
    fun `only the flat candidate when the track sits directly in the tree folder`() {
        assertEquals(
            listOf("primary:Music/song.lrc"),
            lrcDocIdCandidates("primary:Music", "Music/", "song.lrc"),
        )
    }

    @Test
    fun `only the flat candidate when the relative path does not share the tree prefix`() {
        assertEquals(
            listOf("primary:Lyrics/song.lrc"),
            lrcDocIdCandidates("primary:Lyrics", "Music/Album/", "song.lrc"),
        )
    }

    @Test
    fun `a partial folder name is not treated as a shared prefix`() {
        assertEquals(
            listOf("primary:Music/song.lrc"),
            lrcDocIdCandidates("primary:Music", "MusicOld/Album/", "song.lrc"),
        )
    }

    @Test
    fun `nested subfolders are preserved in the derived candidate`() {
        assertEquals(
            listOf("primary:Music/Artist/Album/song.lrc", "primary:Music/song.lrc"),
            lrcDocIdCandidates("primary:Music", "Music/Artist/Album/", "song.lrc"),
        )
    }

    @Test
    fun `like escaping neutralises wildcards and the escape character itself`() {
        assertEquals("100\\% Pure", escapeLike("100% Pure"))
        assertEquals("A\\_B", escapeLike("A_B"))
        assertEquals("back\\\\slash", escapeLike("back\\slash"))
        assertEquals("Rockstar", escapeLike("Rockstar"))
    }

    @Test
    fun `a zero duration on either side is a wildcard`() {
        assertTrue(rowMatches("Song", "Artist", 0L, "Song", "Artist", 200_000L))
        assertTrue(rowMatches("Song", "Artist", 200_000L, "Song", "Artist", 0L))
        assertTrue(rowMatches("Song", "Artist", 200_000L, "Song", "Artist", 201_000L))
    }

    @Test
    fun `two known durations that differ beyond tolerance do not match`() {
        assertFalse(rowMatches("Song", "Artist", 200_000L, "Song", "Artist", 240_000L))
    }

    @Test
    fun `missing relative path falls back to the flat candidate only`() {
        assertEquals(
            listOf("primary:Music/song.lrc"),
            lrcDocIdCandidates("primary:Music", "", "song.lrc"),
        )
    }
}
