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

/**
 * Lyrics embedded in the audio file behind [input], or `""` when the file carries none, is not a
 * supported container, or is malformed. [fileName] selects the parser by extension.
 *
 * Reads only the tag/metadata region, never the audio payload. Does not close [input].
 */
fun embeddedLyrics(input: InputStream, fileName: String): String =
    when (fileName.substringAfterLast('.', "").lowercase()) {
        "mp3" -> id3Lyrics(input)
        "flac" -> flacLyrics(input)
        // ponytail: mp3+flac only — add m4a ©lyr when someone asks.
        else -> ""
    }

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

private fun id3Lyrics(input: InputStream): String {
    val header = readFully(input, Id3HeaderSize) ?: return ""
    if (header[0] != 'I'.code.toByte() || header[1] != 'D'.code.toByte() || header[2] != '3'.code.toByte()) return ""
    val major = header[3].toInt() and 0xFF
    if (major != 3 && major != 4) return ""
    // ponytail: unsynchronisation decoding is not implemented — a tag or frame that sets the unsync
    // flag reads as "no lyrics"; implement the 0xFF 0x00 unstuffing if such files turn up in the wild.
    if (header[5].toInt() and 0x80 != 0) return ""
    val hasExtendedHeader = header[5].toInt() and 0x40 != 0
    val size = syncsafe(header, 6)
    if (size <= 0 || size > MaxMetadataBytes) return ""
    val body = readFully(input, size) ?: return ""

    var pos = if (hasExtendedHeader) extendedHeaderEnd(body, major) else 0
    if (pos < 0) return ""
    while (pos + Id3HeaderSize <= body.size) {
        if (body[pos].toInt() == 0) return ""
        val frameSize = if (major == 4) syncsafe(body, pos + 4) else bigEndian(body, pos + 4)
        if (frameSize <= 0 || pos + Id3HeaderSize + frameSize > body.size) return ""
        val isUslt = body[pos] == 'U'.code.toByte() && body[pos + 1] == 'S'.code.toByte() &&
            body[pos + 2] == 'L'.code.toByte() && body[pos + 3] == 'T'.code.toByte()
        if (isUslt) {
            if (major == 4 && body[pos + 9].toInt() and 0x02 != 0) return ""
            return usltText(body, pos + Id3HeaderSize, frameSize)
        }
        pos += Id3HeaderSize + frameSize
    }
    return ""
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

private fun flacLyrics(input: InputStream): String {
    val magic = readFully(input, 4) ?: return ""
    if (magic[0] != 'f'.code.toByte() || magic[1] != 'L'.code.toByte() ||
        magic[2] != 'a'.code.toByte() || magic[3] != 'C'.code.toByte()
    ) {
        return ""
    }
    while (true) {
        val header = readFully(input, 4) ?: return ""
        val type = header[0].toInt() and 0x7F
        val last = header[0].toInt() and 0x80 != 0
        val length = ((header[1].toInt() and 0xFF) shl 16) or
            ((header[2].toInt() and 0xFF) shl 8) or (header[3].toInt() and 0xFF)
        if (length > MaxMetadataBytes) return ""
        if (type == VorbisCommentBlockType) {
            val block = readFully(input, length) ?: return ""
            return vorbisLyrics(block)
        }
        if (last) return ""
        if (readFully(input, length) == null) return ""
    }
}

private fun vorbisLyrics(block: ByteArray): String {
    var pos = 0
    val vendorLength = littleEndian(block, pos) ?: return ""
    pos += 4 + vendorLength
    val count = littleEndian(block, pos) ?: return ""
    pos += 4
    var unsynced = ""
    repeat(count) {
        val length = littleEndian(block, pos) ?: return ""
        pos += 4
        if (length < 0 || pos + length > block.size) return ""
        val comment = String(block, pos, length, Charsets.UTF_8)
        pos += length
        val separator = comment.indexOf('=')
        if (separator > 0) {
            val key = comment.substring(0, separator).uppercase()
            val value = comment.substring(separator + 1)
            if (key == "LYRICS") return value
            if (key == "UNSYNCEDLYRICS" && unsynced.isEmpty()) unsynced = value
        }
    }
    return unsynced
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
