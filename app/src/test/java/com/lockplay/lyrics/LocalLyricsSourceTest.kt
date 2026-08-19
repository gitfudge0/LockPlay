package com.lockplay.lyrics

import org.junit.Assert.assertEquals
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
    fun `missing relative path falls back to the flat candidate only`() {
        assertEquals(
            listOf("primary:Music/song.lrc"),
            lrcDocIdCandidates("primary:Music", "", "song.lrc"),
        )
    }
}
