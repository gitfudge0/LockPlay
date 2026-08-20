package com.lockplay.lyrics

import java.io.InputStream

/**
 * Reading lyrics that already live on the device: embedded in the audio file's tags, or in a
 * sidecar `.lrc` next to it.
 *
 * Compose-free and Android-free (A13) so it can be exercised on the JVM with byte arrays. Performs
 * no file lookup itself — the caller opens the stream and hands it over. Absence is `""`, never
 * null (A7); malformed input is absence, not an exception.
 */

/** ID3v2 tag header, and every ID3v2 frame header, is 10 bytes. */
private const val Id3HeaderSize = 10

/** Tag/metadata regions this size or larger are treated as malformed rather than allocated. */
private const val MaxMetadataBytes = 16 * 1024 * 1024

private const val VorbisCommentBlockType = 4
private const val StreamInfoBlockType = 0
private const val StreamInfoSize = 34

/**
 * Tags read straight out of the audio file, bypassing whatever MediaStore did or did not extract.
 * Absence is `""` / `0L`, never null (A7).
 */
data class EmbeddedTags(
    val title: String,
    val artist: String,
    val durationMs: Long,
    val lyrics: String,
) {
    companion object {
        val EMPTY = EmbeddedTags("", "", 0L, "")
    }
}

/**
 * Tags embedded in the audio file behind [input], or [EmbeddedTags.EMPTY] when the file carries
 * none, is not a supported container, or is malformed. [fileName] selects the parser by extension.
 *
 * Reads only the tag/metadata region, never the audio payload. Does not close [input].
 */
fun embeddedTags(input: InputStream, fileName: String): EmbeddedTags =
    when (fileName.substringAfterLast('.', "").lowercase()) {
        "mp3" -> id3Tags(input)
        "flac" -> flacTags(input)
        // ponytail: mp3+flac only — add m4a ©lyr when someone asks.
        else -> EmbeddedTags.EMPTY
    }

/**
 * Lyrics embedded in the audio file behind [input], or `""` when the file carries none, is not a
 * supported container, or is malformed. [fileName] selects the parser by extension.
 */
fun embeddedLyrics(input: InputStream, fileName: String): String = embeddedTags(input, fileName).lyrics

/** Sidecar LRC name for an audio file: `Song.mp3` -> `Song.lrc`, `Song` -> `Song.lrc`. */
fun lrcFileNameFor(audioDisplayName: String): String {
    val dot = audioDisplayName.lastIndexOf('.')
    return if (dot > 0) audioDisplayName.substring(0, dot) + ".lrc" else "$audioDisplayName.lrc"
}

/**
 * Turns raw lyrics text into [Lyrics]: timestamped when [parseLrc] finds lines, plain otherwise,
 * [Lyrics.EMPTY] when blank.
 */
fun lyricsFromText(raw: String): Lyrics {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return Lyrics.EMPTY
    val lines = parseLrc(trimmed)
    return if (lines.isNotEmpty()) {
        Lyrics(LyricsStatus.Synced, lines, "")
    } else {
        Lyrics(LyricsStatus.Plain, emptyList(), trimmed)
    }
}

/** Duration stays 0: mp3 has no cheap exact length, and the matcher treats 0 as a wildcard. */
private fun id3Tags(input: InputStream): EmbeddedTags {
    val header = readFully(input, Id3HeaderSize) ?: return EmbeddedTags.EMPTY
    if (header[0] != 'I'.code.toByte() || header[1] != 'D'.code.toByte() || header[2] != '3'.code.toByte()) {
        return EmbeddedTags.EMPTY
    }
    val major = header[3].toInt() and 0xFF
    if (major != 3 && major != 4) return EmbeddedTags.EMPTY
    // ponytail: unsynchronisation decoding is not implemented — a tag or frame that sets the unsync
    // flag reads as "no lyrics"; implement the 0xFF 0x00 unstuffing if such files turn up in the wild.
    if (header[5].toInt() and 0x80 != 0) return EmbeddedTags.EMPTY
    val hasExtendedHeader = header[5].toInt() and 0x40 != 0
    val size = syncsafe(header, 6)
    if (size <= 0 || size > MaxMetadataBytes) return EmbeddedTags.EMPTY
    val body = readFully(input, size) ?: return EmbeddedTags.EMPTY

    var title = ""
    var artist = ""
    var lyrics = ""
    var pos = if (hasExtendedHeader) extendedHeaderEnd(body, major) else 0
    if (pos < 0) return EmbeddedTags.EMPTY
    while (pos + Id3HeaderSize <= body.size) {
        if (body[pos].toInt() == 0) break
        val frameSize = if (major == 4) syncsafe(body, pos + 4) else bigEndian(body, pos + 4)
        if (frameSize <= 0 || pos + Id3HeaderSize + frameSize > body.size) break
        val id = String(body, pos, 4, Charsets.ISO_8859_1)
        val unsyncedFrame = major == 4 && body[pos + 9].toInt() and 0x02 != 0
        val start = pos + Id3HeaderSize
        if (!unsyncedFrame) {
            when (id) {
                "USLT" -> if (lyrics.isEmpty()) lyrics = usltText(body, start, frameSize)
                "TIT2" -> if (title.isEmpty()) title = textFrame(body, start, frameSize)
                "TPE1" -> if (artist.isEmpty()) artist = textFrame(body, start, frameSize)
            }
        }
        pos += Id3HeaderSize + frameSize
    }
    return EmbeddedTags(title, artist, 0L, lyrics)
}

/** ID3 text frame: one encoding byte then the text, minus any trailing terminator. */
private fun textFrame(body: ByteArray, start: Int, frameSize: Int): String {
    val end = start + frameSize
    if (end > body.size || frameSize < 2) return ""
    val charset = id3Charset(body[start].toInt() and 0xFF) ?: return ""
    return String(body, start + 1, end - start - 1, charset).trimEnd(' ', '\u0000')
}

private fun id3Charset(encoding: Int): java.nio.charset.Charset? = when (encoding) {
    0 -> Charsets.ISO_8859_1
    1 -> Charsets.UTF_16
    2 -> Charsets.UTF_16BE
    3 -> Charsets.UTF_8
    else -> null
}

/** Offset of the first frame past an extended header, or -1 when the declared size does not fit. */
private fun extendedHeaderEnd(body: ByteArray, major: Int): Int {
    if (body.size < 4) return -1
    // v2.4 declares a syncsafe size that includes the size field; v2.3 a plain size that excludes it.
    val declared = if (major == 4) syncsafe(body, 0) else bigEndian(body, 0)
    if (declared <= 0) return -1
    val end = if (major == 4) declared else declared + 4
    return if (end in 0..body.size) end else -1
}

private fun usltText(body: ByteArray, start: Int, frameSize: Int): String {
    val end = start + frameSize
    // 1 encoding byte + 3 language bytes + at least the descriptor terminator.
    if (end > body.size || frameSize < 5) return ""
    val encoding = body[start].toInt() and 0xFF
    val wide = encoding == 1 || encoding == 2
    var cursor = start + 4
    while (cursor < end) {
        if (wide) {
            if (cursor + 1 >= end) return ""
            if (body[cursor].toInt() == 0 && body[cursor + 1].toInt() == 0) {
                cursor += 2
                break
            }
            cursor += 2
        } else {
            if (body[cursor].toInt() == 0) {
                cursor += 1
                break
            }
            cursor += 1
        }
    }
    if (cursor > end) return ""
    val charset = when (encoding) {
        0 -> Charsets.ISO_8859_1
        1 -> Charsets.UTF_16
        2 -> Charsets.UTF_16BE
        3 -> Charsets.UTF_8
        else -> return ""
    }
    return String(body, cursor, end - cursor, charset)
}

private fun flacTags(input: InputStream): EmbeddedTags {
    val magic = readFully(input, 4) ?: return EmbeddedTags.EMPTY
    if (magic[0] != 'f'.code.toByte() || magic[1] != 'L'.code.toByte() ||
        magic[2] != 'a'.code.toByte() || magic[3] != 'C'.code.toByte()
    ) {
        return EmbeddedTags.EMPTY
    }
    var tags = EmbeddedTags.EMPTY
    var durationMs = 0L
    while (true) {
        val header = readFully(input, 4) ?: return tags.copy(durationMs = durationMs)
        val type = header[0].toInt() and 0x7F
        val last = header[0].toInt() and 0x80 != 0
        val length = ((header[1].toInt() and 0xFF) shl 16) or
            ((header[2].toInt() and 0xFF) shl 8) or (header[3].toInt() and 0xFF)
        if (length > MaxMetadataBytes) return tags.copy(durationMs = durationMs)
        val block = readFully(input, length) ?: return tags.copy(durationMs = durationMs)
        if (type == StreamInfoBlockType) durationMs = streamInfoDurationMs(block)
        if (type == VorbisCommentBlockType) tags = vorbisTags(block)
        if (last) return tags.copy(durationMs = durationMs)
    }
}

/** STREAMINFO: sample rate is 20 bits at byte 10, total samples the 36 bits that follow. */
private fun streamInfoDurationMs(block: ByteArray): Long {
    if (block.size < StreamInfoSize) return 0L
    var bits = 0L
    for (i in 10 until 18) bits = (bits shl 8) or (block[i].toLong() and 0xFF)
    val sampleRate = (bits ushr 44) and 0xFFFFF
    val totalSamples = bits and 0xFFFFFFFFFL
    if (sampleRate == 0L) return 0L
    return totalSamples * 1000L / sampleRate
}

private fun vorbisTags(block: ByteArray): EmbeddedTags {
    var pos = 0
    val vendorLength = littleEndian(block, pos) ?: return EmbeddedTags.EMPTY
    pos += 4 + vendorLength
    val count = littleEndian(block, pos) ?: return EmbeddedTags.EMPTY
    pos += 4
    var title = ""
    var artist = ""
    var lyrics = ""
    var unsynced = ""
    repeat(count) {
        val length = littleEndian(block, pos) ?: return EmbeddedTags(title, artist, 0L, lyrics.ifEmpty { unsynced })
        pos += 4
        if (length < 0 || pos + length > block.size) {
            return EmbeddedTags(title, artist, 0L, lyrics.ifEmpty { unsynced })
        }
        val comment = String(block, pos, length, Charsets.UTF_8)
        pos += length
        val separator = comment.indexOf('=')
        if (separator > 0) {
            val value = comment.substring(separator + 1)
            when (comment.substring(0, separator).uppercase()) {
                "TITLE" -> if (title.isEmpty()) title = value
                "ARTIST" -> if (artist.isEmpty()) artist = value
                "LYRICS" -> if (lyrics.isEmpty()) lyrics = value
                "UNSYNCEDLYRICS" -> if (unsynced.isEmpty()) unsynced = value
            }
        }
    }
    return EmbeddedTags(title, artist, 0L, lyrics.ifEmpty { unsynced })
}

/** Reads exactly [count] bytes, or null when the stream ends first. */
private fun readFully(input: InputStream, count: Int): ByteArray? {
    if (count < 0 || count > MaxMetadataBytes) return null
    val out = ByteArray(count)
    var read = 0
    while (read < count) {
        val n = input.read(out, read, count - read)
        if (n <= 0) return null
        read += n
    }
    return out
}

/** ID3 syncsafe integer: four bytes carrying 7 bits each. */
private fun syncsafe(bytes: ByteArray, offset: Int): Int {
    if (offset + 4 > bytes.size) return -1
    var value = 0
    for (i in 0 until 4) {
        val b = bytes[offset + i].toInt() and 0xFF
        if (b and 0x80 != 0) return -1
        value = (value shl 7) or b
    }
    return value
}

private fun bigEndian(bytes: ByteArray, offset: Int): Int {
    if (offset + 4 > bytes.size) return -1
    val value = ((bytes[offset].toInt() and 0xFF).toLong() shl 24) or
        ((bytes[offset + 1].toInt() and 0xFF).toLong() shl 16) or
        ((bytes[offset + 2].toInt() and 0xFF).toLong() shl 8) or
        (bytes[offset + 3].toInt() and 0xFF).toLong()
    return if (value > Int.MAX_VALUE) -1 else value.toInt()
}

private fun littleEndian(bytes: ByteArray, offset: Int): Int? {
    if (offset < 0 || offset + 4 > bytes.size) return null
    val value = ((bytes[offset].toInt() and 0xFF).toLong()) or
        ((bytes[offset + 1].toInt() and 0xFF).toLong() shl 8) or
        ((bytes[offset + 2].toInt() and 0xFF).toLong() shl 16) or
        ((bytes[offset + 3].toInt() and 0xFF).toLong() shl 24)
    return if (value > MaxMetadataBytes) null else value.toInt()
}
