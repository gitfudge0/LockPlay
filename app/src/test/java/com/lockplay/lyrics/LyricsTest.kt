package com.lockplay.lyrics

import org.junit.Assert.assertEquals
import org.junit.Test

class LyricsTest {

    @Test
    fun `EMPTY has no source`() {
        assertEquals(LyricsSource.None, Lyrics.EMPTY.source)
    }

    @Test
    fun `source defaults to None when not specified`() {
        val lyrics = Lyrics(LyricsStatus.NotFound, emptyList(), "")
        assertEquals(LyricsSource.None, lyrics.source)
    }
}
