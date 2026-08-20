package com.lockplay.lyrics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream

class LocalLyricsTest {

    @Test
    fun `id3v2 3 uslt with utf8 text is returned`() {
        val tag = id3Tag(major = 3, frames = listOf(usltFrame(major = 3, encoding = 3, text = "hello lyrics")))
        assertEquals("hello lyrics", embeddedLyrics(ByteArrayInputStream(tag), "Song.mp3"))
    }

    @Test
    fun `id3v2 4 uses syncsafe frame sizes`() {
        val tag = id3Tag(major = 4, frames = listOf(usltFrame(major = 4, encoding = 3, text = "v4 lyrics")))
        assertEquals("v4 lyrics", embeddedLyrics(ByteArrayInputStream(tag), "Song.MP3"))
    }

    @Test
    fun `id3 uslt with latin1 encoding decodes high bytes`() {
        val tag = id3Tag(major = 3, frames = listOf(usltFrame(major = 3, encoding = 0, text = "café")))
        assertEquals("café", embeddedLyrics(ByteArrayInputStream(tag), "Song.mp3"))
    }

    @Test
    fun `id3 uslt with utf16 bom decodes`() {
        val tag = id3Tag(major = 3, frames = listOf(usltFrame(major = 3, encoding = 1, text = "wide lyrics")))
        assertEquals("wide lyrics", embeddedLyrics(ByteArrayInputStream(tag), "Song.mp3"))
    }

    @Test
    fun `id3 uslt content descriptor is skipped`() {
        val frame = usltFrame(major = 3, encoding = 3, text = "after descriptor", descriptor = "desc")
        val tag = id3Tag(major = 3, frames = listOf(frame))
        assertEquals("after descriptor", embeddedLyrics(ByteArrayInputStream(tag), "Song.mp3"))
    }

    @Test
    fun `a non uslt frame before uslt is skipped by size`() {
        val title = frame(major = 3, id = "TIT2", body = byteArrayOf(3) + "Some Title".toByteArray())
        val tag = id3Tag(major = 3, frames = listOf(title, usltFrame(major = 3, encoding = 3, text = "found it")))
        assertEquals("found it", embeddedLyrics(ByteArrayInputStream(tag), "Song.mp3"))
    }

    @Test
    fun `a tag with no uslt frame yields empty`() {
        val title = frame(major = 3, id = "TIT2", body = byteArrayOf(3) + "Some Title".toByteArray())
        val tag = id3Tag(major = 3, frames = listOf(title))
        assertEquals("", embeddedLyrics(ByteArrayInputStream(tag), "Song.mp3"))
    }

    @Test
    fun `a truncated tag yields empty instead of throwing`() {
        val tag = id3Tag(major = 3, frames = listOf(usltFrame(major = 3, encoding = 3, text = "hello lyrics")))
        val truncated = tag.copyOf(tag.size - 5)
        assertEquals("", embeddedLyrics(ByteArrayInputStream(truncated), "Song.mp3"))
    }

    @Test
    fun `a tag level unsync flag yields empty because unsync decoding is not implemented`() {
        val tag = id3Tag(major = 3, frames = listOf(usltFrame(major = 3, encoding = 3, text = "hello lyrics")), unsync = true)
        assertEquals("", embeddedLyrics(ByteArrayInputStream(tag), "Song.mp3"))
    }

    @Test
    fun `a frame level unsync flag yields empty`() {
        val uslt = usltFrame(major = 4, encoding = 3, text = "hello lyrics")
        uslt[9] = 0x02
        val tag = id3Tag(major = 4, frames = listOf(uslt))
        assertEquals("", embeddedLyrics(ByteArrayInputStream(tag), "Song.mp3"))
    }

    @Test
    fun `bytes without an ID3 header yield empty`() {
        val notATag = ByteArray(64) { 0x11 }
        assertEquals("", embeddedLyrics(ByteArrayInputStream(notATag), "Song.mp3"))
    }

    @Test
    fun `flac vorbis comment LYRICS is returned`() {
        val flac = flacFile(listOf("TITLE=Song", "LYRICS=[00:01.00] line one"))
        assertEquals("[00:01.00] line one", embeddedLyrics(ByteArrayInputStream(flac), "Song.flac"))
    }

    @Test
    fun `flac falls back to UNSYNCEDLYRICS and matches keys case insensitively`() {
        val flac = flacFile(listOf("unsyncedlyrics=plain words"))
        assertEquals("plain words", embeddedLyrics(ByteArrayInputStream(flac), "Song.flac"))
    }

    @Test
    fun `flac prefers LYRICS over UNSYNCEDLYRICS regardless of order`() {
        val flac = flacFile(listOf("UNSYNCEDLYRICS=plain words", "LYRICS=synced words"))
        assertEquals("synced words", embeddedLyrics(ByteArrayInputStream(flac), "Song.flac"))
    }

    @Test
    fun `flac with no lyrics comment yields empty`() {
        val flac = flacFile(listOf("TITLE=Song", "ARTIST=Someone"))
        assertEquals("", embeddedLyrics(ByteArrayInputStream(flac), "Song.flac"))
    }

    @Test
    fun `bytes without the fLaC magic yield empty`() {
        assertEquals("", embeddedLyrics(ByteArrayInputStream(ByteArray(32)), "Song.flac"))
    }

    @Test
    fun `an unsupported extension yields empty without reading`() {
        val flac = flacFile(listOf("LYRICS=hidden"))
        assertEquals("", embeddedLyrics(ByteArrayInputStream(flac), "Song.m4a"))
        assertEquals("", embeddedLyrics(ByteArrayInputStream(flac), "Song"))
    }

    @Test
    fun `lrc file name swaps the extension or appends one`() {
        assertEquals("Song.lrc", lrcFileNameFor("Song.mp3"))
        assertEquals("Song.lrc", lrcFileNameFor("Song.flac"))
        assertEquals("Song.lrc", lrcFileNameFor("Song"))
        assertEquals("My.Song.lrc", lrcFileNameFor("My.Song.mp3"))
    }

    @Test
    fun `timestamped text becomes synced lyrics`() {
        val lyrics = lyricsFromText("[00:01.00]one\n[00:02.00]two\n")
        assertEquals(LyricsStatus.Synced, lyrics.status)
        assertEquals(2, lyrics.lines.size)
        assertEquals("", lyrics.plainText)
    }

    @Test
    fun `untimestamped text becomes plain lyrics`() {
        val lyrics = lyricsFromText("  just some words\nand more  ")
        assertEquals(LyricsStatus.Plain, lyrics.status)
        assertTrue(lyrics.lines.isEmpty())
        assertEquals("just some words\nand more", lyrics.plainText)
    }

    @Test
    fun `blank text becomes the empty sentinel`() {
        assertEquals(Lyrics.EMPTY, lyricsFromText("   \n\t "))
        assertEquals(Lyrics.EMPTY, lyricsFromText(""))
    }

    @Test
    fun `flac embedded tags carry title artist lyrics and the streaminfo duration`() {
        val flac = flacFile(
            comments = listOf("TITLE=Good Goodbye", "ARTIST=HWASA", "LYRICS=[00:01.00] line one"),
            streamInfo = streamInfo(sampleRate = 44_100, totalSamples = 44_100L * 185),
        )
        val tags = embeddedTags(ByteArrayInputStream(flac), "01. Good Goodbye.flac")
        assertEquals("Good Goodbye", tags.title)
        assertEquals("HWASA", tags.artist)
        assertEquals("[00:01.00] line one", tags.lyrics)
        assertEquals(185_000L, tags.durationMs)
    }

    @Test
    fun `flac with a zero sample rate reports duration zero instead of dividing by zero`() {
        val flac = flacFile(listOf("TITLE=Song"))
        assertEquals(0L, embeddedTags(ByteArrayInputStream(flac), "Song.flac").durationMs)
    }

    @Test
    fun `id3v2 3 embedded tags carry title and artist with duration zero`() {
        val tag = id3Tag(
            major = 3,
            frames = listOf(
                frame(major = 3, id = "TIT2", body = byteArrayOf(3) + "Rockstar".toByteArray()),
                frame(major = 3, id = "TPE1", body = byteArrayOf(3) + "Someone".toByteArray()),
                usltFrame(major = 3, encoding = 3, text = "words"),
            ),
        )
        val tags = embeddedTags(ByteArrayInputStream(tag), "01. Rockstar.mp3")
        assertEquals("Rockstar", tags.title)
        assertEquals("Someone", tags.artist)
        assertEquals("words", tags.lyrics)
        assertEquals(0L, tags.durationMs)
    }

    @Test
    fun `id3 text frames honour the encoding byte`() {
        val tag = id3Tag(
            major = 3,
            frames = listOf(frame(major = 3, id = "TIT2", body = byteArrayOf(0) + "café".toByteArray(Charsets.ISO_8859_1))),
        )
        assertEquals("café", embeddedTags(ByteArrayInputStream(tag), "Song.mp3").title)
    }

    @Test
    fun `an unsupported extension yields empty tags`() {
        val flac = flacFile(listOf("TITLE=hidden"))
        assertEquals(EmbeddedTags.EMPTY, embeddedTags(ByteArrayInputStream(flac), "Song.m4a"))
    }

    private fun id3Tag(major: Int, frames: List<ByteArray>, unsync: Boolean = false): ByteArray {
        val body = frames.fold(ByteArray(0)) { acc, f -> acc + f }
        val header = ByteArray(10)
        header[0] = 'I'.code.toByte()
        header[1] = 'D'.code.toByte()
        header[2] = '3'.code.toByte()
        header[3] = major.toByte()
        header[4] = 0
        header[5] = if (unsync) 0x80.toByte() else 0
        writeSyncsafe(header, 6, body.size)
        return header + body
    }

    private fun frame(major: Int, id: String, body: ByteArray): ByteArray {
        val header = ByteArray(10)
        id.toByteArray(Charsets.ISO_8859_1).copyInto(header, 0)
        if (major == 4) writeSyncsafe(header, 4, body.size) else writeBigEndian(header, 4, body.size)
        return header + body
    }

    private fun usltFrame(major: Int, encoding: Int, text: String, descriptor: String = ""): ByteArray {
        val charset = when (encoding) {
            0 -> Charsets.ISO_8859_1
            1 -> Charsets.UTF_16
            2 -> Charsets.UTF_16BE
            else -> Charsets.UTF_8
        }
        val terminator = if (encoding == 1 || encoding == 2) byteArrayOf(0, 0) else byteArrayOf(0)
        val descriptorBytes = if (descriptor.isEmpty()) ByteArray(0) else descriptor.toByteArray(charset)
        val body = byteArrayOf(encoding.toByte()) + "eng".toByteArray(Charsets.ISO_8859_1) +
            descriptorBytes + terminator + text.toByteArray(charset)
        return frame(major, "USLT", body)
    }

    private fun writeSyncsafe(target: ByteArray, offset: Int, value: Int) {
        target[offset] = ((value shr 21) and 0x7F).toByte()
        target[offset + 1] = ((value shr 14) and 0x7F).toByte()
        target[offset + 2] = ((value shr 7) and 0x7F).toByte()
        target[offset + 3] = (value and 0x7F).toByte()
    }

    private fun writeBigEndian(target: ByteArray, offset: Int, value: Int) {
        target[offset] = ((value shr 24) and 0xFF).toByte()
        target[offset + 1] = ((value shr 16) and 0xFF).toByte()
        target[offset + 2] = ((value shr 8) and 0xFF).toByte()
        target[offset + 3] = (value and 0xFF).toByte()
    }

    private fun flacFile(comments: List<String>, streamInfo: ByteArray = ByteArray(34)): ByteArray =
        "fLaC".toByteArray(Charsets.US_ASCII) +
            metadataBlock(type = 0, last = false, data = streamInfo) +
            metadataBlock(type = 4, last = true, data = vorbisComment(comments))

    private fun streamInfo(sampleRate: Int, totalSamples: Long): ByteArray {
        val data = ByteArray(34)
        val bits = (sampleRate.toLong() shl 44) or (2L shl 41) or (15L shl 36) or totalSamples
        for (i in 0 until 8) data[10 + i] = ((bits shr (56 - i * 8)) and 0xFF).toByte()
        return data
    }

    private fun metadataBlock(type: Int, last: Boolean, data: ByteArray): ByteArray {
        val header = ByteArray(4)
        header[0] = (type or if (last) 0x80 else 0).toByte()
        header[1] = ((data.size shr 16) and 0xFF).toByte()
        header[2] = ((data.size shr 8) and 0xFF).toByte()
        header[3] = (data.size and 0xFF).toByte()
        return header + data
    }

    private fun vorbisComment(comments: List<String>): ByteArray {
        val vendor = "test".toByteArray(Charsets.UTF_8)
        var out = littleEndianBytes(vendor.size) + vendor + littleEndianBytes(comments.size)
        for (comment in comments) {
            val bytes = comment.toByteArray(Charsets.UTF_8)
            out += littleEndianBytes(bytes.size) + bytes
        }
        return out
    }

    private fun littleEndianBytes(value: Int): ByteArray = byteArrayOf(
        (value and 0xFF).toByte(),
        ((value shr 8) and 0xFF).toByte(),
        ((value shr 16) and 0xFF).toByte(),
        ((value shr 24) and 0xFF).toByte(),
    )
}
