package com.lockplay.lyrics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LyricsLrcTest {

    @Test
    fun `two-digit fraction is centiseconds not milliseconds`() {
        // The classic bug: [00:12.34] must be 12340ms, not 34ms.
        val lines = parseLrc("[00:12.34]hello")
        assertEquals(listOf(LyricLine(12_340L, "hello")), lines)
    }

    @Test
    fun `three-digit fraction is milliseconds`() {
        val lines = parseLrc("[00:12.345]hello")
        assertEquals(listOf(LyricLine(12_345L, "hello")), lines)
    }

    @Test
    fun `bare mm ss with no fraction parses`() {
        val lines = parseLrc("[01:02]hello")
        assertEquals(listOf(LyricLine(62_000L, "hello")), lines)
    }

    @Test
    fun `multiple timestamps on one line each produce a line with the same text`() {
        val lines = parseLrc("[00:10.00][00:40.00]chorus")
        assertEquals(
            listOf(LyricLine(10_000L, "chorus"), LyricLine(40_000L, "chorus")),
            lines,
        )
    }

    @Test
    fun `metadata tags and untimed or blank lines are skipped`() {
        val raw = """
            [ar:Some Artist]
            [ti:Some Title]
            [length:03:21]
            [by:Someone]

            not a lyric line
            [00:01.00]real line
        """.trimIndent()
        val lines = parseLrc(raw)
        assertEquals(listOf(LyricLine(1_000L, "real line")), lines)
    }

    @Test
    fun `timestamp with empty text is kept for instrumental gaps`() {
        val lines = parseLrc("[00:05.00]")
        assertEquals(listOf(LyricLine(5_000L, "")), lines)
    }

    @Test
    fun `out of order input sorts ascending by atMs`() {
        val raw = "[00:20.00]second\n[00:10.00]first"
        val lines = parseLrc(raw)
        assertEquals(listOf(LyricLine(10_000L, "first"), LyricLine(20_000L, "second")), lines)
    }

    @Test
    fun `crlf input leaves no stray carriage return in text`() {
        val lines = parseLrc("[00:01.00]hello\r\n[00:02.00]world\r\n")
        assertEquals(listOf(LyricLine(1_000L, "hello"), LyricLine(2_000L, "world")), lines)
        lines.forEach { assertTrue(it.text.none { c -> c == '\r' }) }
    }

    @Test
    fun `non-ascii and rtl text is preserved`() {
        val lines = parseLrc("[00:01.00]مرحبا")
        assertEquals("مرحبا", lines.single().text)
    }

    @Test
    fun `malformed tags are skipped without throwing and surrounding lines still parse`() {
        val raw = """
            [99]bogus
            [aa:bb.cc]also bogus
            [00:12.34
            [00:01.00]ok
        """.trimIndent()
        val lines = parseLrc(raw)
        assertEquals(listOf(LyricLine(1_000L, "ok")), lines)
    }

    @Test
    fun `empty and blank input yield empty list`() {
        assertEquals(emptyList<LyricLine>(), parseLrc(""))
        assertEquals(emptyList<LyricLine>(), parseLrc("   \n  \n"))
    }

    @Test
    fun `bracket inside lyric body stays as text`() {
        val lines = parseLrc("[00:01.00]a [bracket] in the text")
        assertEquals("a [bracket] in the text", lines.single().text)
    }

    @Test
    fun `active line index picks boundary line and reports none before the first timestamp`() {
        val lines = listOf(LyricLine(10_000L, "a"), LyricLine(20_000L, "b"))
        assertEquals(0, activeLineIndex(lines, 19_999L))
        assertEquals(1, activeLineIndex(lines, 20_000L))
        assertEquals(1, activeLineIndex(lines, 20_001L))
        assertEquals(-1, activeLineIndex(lines, 0L))
        assertEquals(1, activeLineIndex(lines, 999_999L))
    }

    @Test
    fun `active line index handles empty list and negative position`() {
        assertEquals(-1, activeLineIndex(emptyList(), 0L))
        assertEquals(-1, activeLineIndex(listOf(LyricLine(10_000L, "a")), -1L))
    }

    @Test
    fun `active line index with duplicate timestamps returns the last matching index`() {
        val lines = listOf(LyricLine(10_000L, "a"), LyricLine(10_000L, "b"), LyricLine(20_000L, "c"))
        assertEquals(1, activeLineIndex(lines, 10_000L))
    }

    @Test
    fun `lyrics position clamps negative to zero and caps to a known duration`() {
        assertEquals(0L, lyricsPositionMs(-5L, 100_000L))
        assertEquals(100_000L, lyricsPositionMs(150_000L, 100_000L))
        assertEquals(50_000L, lyricsPositionMs(50_000L, 100_000L))
    }

    @Test
    fun `zero duration passes the position through unclamped, not pinned to zero`() {
        // Getting this backwards freezes every lyric on line one for players reporting no duration.
        assertEquals(50_000L, lyricsPositionMs(50_000L, 0L))
    }
}
