package com.lockplay.lyrics

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import java.io.FileNotFoundException
import java.io.IOException
import kotlin.math.abs

/**
 * On-device lyrics lookup: the offline half of the lyrics feature, tried before anything leaves
 * the device. Resolves the now-playing track to a MediaStore row, then reads embedded tags first
 * and a user-picked `.lrc` folder second.
 *
 * Compose-free (A13) and does its own I/O, so every seam degrades to [Lyrics.EMPTY] rather than
 * throwing. Nothing here logs track metadata (X3) — messages are static, the exception carries
 * the detail.
 */
private const val TAG = "LocalLyricsSource"

// ponytail: duplicates the 2s tolerance in LyricsMatch.kt, which keeps its copy private — unify
// the two the moment either one is exported or the value needs tuning.
private const val DurationMatchToleranceMs = 2_000L

private val AudioProjection = arrayOf(
    MediaStore.Audio.Media._ID,
    MediaStore.Audio.Media.DISPLAY_NAME,
    MediaStore.Audio.Media.RELATIVE_PATH,
    MediaStore.Audio.Media.DURATION,
    MediaStore.Audio.Media.TITLE,
    MediaStore.Audio.Media.ARTIST,
)

private const val MusicSelection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
private const val MusicAndTitleSelection = "$MusicSelection AND ${MediaStore.Audio.Media.TITLE} LIKE ?"
private const val MusicAndFileNameSelection =
    "$MusicSelection AND ${MediaStore.Audio.Media.DISPLAY_NAME} LIKE ? ESCAPE '\\'"

// ponytail: at most this many filename candidates get their real tags read — upgrade path is a
// persisted tag index built once, instead of a bounded scan per lookup.
private const val MaxTagVerifyCandidates = 10

object LocalLyricsSource {

    /**
     * Lyrics for the given track from local storage, or [Lyrics.EMPTY] when nothing usable is
     * found — a missing permission, no matching row, and an unreadable file are all the same
     * calm answer to the caller, which then falls through to the network provider.
     *
     * [lyricsFolderUri] is the user's `ACTION_OPEN_DOCUMENT_TREE` grant; null disables the `.lrc`
     * step entirely.
     */
    fun lookup(
        context: Context,
        title: String,
        artist: String,
        durationMs: Long,
        lyricsFolderUri: Uri?,
    ): Lyrics {
        if (context.checkSelfPermission(Manifest.permission.READ_MEDIA_AUDIO) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return Lyrics.EMPTY
        }

        val match = findRow(context, title, artist, durationMs, prefilterTitle = true)
            ?: findRow(context, title, artist, durationMs, prefilterTitle = false)

        if (match == null) {
            // Devices whose MediaStore never extracted tags report every row as title=filename stem,
            // artist="<unknown>", so the column match above can never hit; verify tags in-file instead.
            return verifyByTags(context, title, artist, durationMs, lyricsFolderUri) ?: Lyrics.EMPTY
        }

        embedded(context, match)?.let { return it }
        if (lyricsFolderUri != null) {
            lrc(context, match, lyricsFolderUri)?.let { return it }
        }
        return Lyrics.EMPTY
    }

    private fun verifyByTags(
        context: Context,
        title: String,
        artist: String,
        durationMs: Long,
        lyricsFolderUri: Uri?,
    ): Lyrics? {
        if (title.isBlank()) return null
        return try {
            context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                AudioProjection,
                MusicAndFileNameSelection,
                arrayOf("%${escapeLike(title)}%"),
                null,
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
                val pathColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.RELATIVE_PATH)
                var checked = 0
                while (cursor.moveToNext() && checked < MaxTagVerifyCandidates) {
                    checked++
                    val row = AudioRow(
                        id = cursor.getLong(idColumn),
                        displayName = cursor.getString(nameColumn) ?: "",
                        relativePath = cursor.getString(pathColumn) ?: "",
                    )
                    val tags = readTags(context, row) ?: continue
                    if (!rowMatches(tags.title, tags.artist, tags.durationMs, title, artist, durationMs)) continue
                    if (tags.lyrics.isNotBlank()) {
                        lyricsFromText(tags.lyrics).takeIf { it != Lyrics.EMPTY }?.let { return@use it }
                    }
                    if (lyricsFolderUri != null) {
                        lrc(context, row, lyricsFolderUri)?.let { return@use it }
                    }
                }
                null
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "MediaStore filename query denied — READ_MEDIA_AUDIO revoked?", e)
            null
        }
    }

    private fun readTags(context: Context, row: AudioRow): EmbeddedTags? {
        val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, row.id)
        return try {
            context.contentResolver.openInputStream(uri)?.use { embeddedTags(it, row.displayName) }
        } catch (e: SecurityException) {
            Log.w(TAG, "embedded tag read denied", e)
            null
        } catch (e: FileNotFoundException) {
            Log.w(TAG, "embedded tag source missing", e)
            null
        } catch (e: IOException) {
            Log.w(TAG, "embedded tag read failed", e)
            null
        }
    }

    private fun findRow(
        context: Context,
        title: String,
        artist: String,
        durationMs: Long,
        prefilterTitle: Boolean,
    ): AudioRow? {
        val selection = if (prefilterTitle) MusicAndTitleSelection else MusicSelection
        val args = if (prefilterTitle) arrayOf("%$title%") else null
        return try {
            context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                AudioProjection,
                selection,
                args,
                null,
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
                val pathColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.RELATIVE_PATH)
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                while (cursor.moveToNext()) {
                    val rowTitle = cursor.getString(titleColumn) ?: ""
                    val rowArtist = cursor.getString(artistColumn) ?: ""
                    val rowDuration = cursor.getLong(durationColumn)
                    if (!rowMatches(rowTitle, rowArtist, rowDuration, title, artist, durationMs)) continue
                    return@use AudioRow(
                        id = cursor.getLong(idColumn),
                        displayName = cursor.getString(nameColumn) ?: "",
                        relativePath = cursor.getString(pathColumn) ?: "",
                    )
                }
                null
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "MediaStore audio query denied — READ_MEDIA_AUDIO revoked?", e)
            null
        }
    }

    private fun embedded(context: Context, row: AudioRow): Lyrics? {
        val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, row.id)
        val text = try {
            context.contentResolver.openInputStream(uri)?.use { embeddedLyrics(it, row.displayName) }
        } catch (e: SecurityException) {
            Log.w(TAG, "embedded lyrics read denied", e)
            null
        } catch (e: FileNotFoundException) {
            Log.w(TAG, "embedded lyrics source missing", e)
            null
        } catch (e: IOException) {
            Log.w(TAG, "embedded lyrics read failed", e)
            null
        }
        if (text.isNullOrBlank()) return null
        return lyricsFromText(text).takeIf { it != Lyrics.EMPTY }
    }

    // ponytail: direct docId lookup, no tree walk — add DocumentFile recursion if users' folders don't align
    private fun lrc(context: Context, row: AudioRow, lyricsFolderUri: Uri): Lyrics? {
        val treeDocId = try {
            DocumentsContract.getTreeDocumentId(lyricsFolderUri)
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "lyrics folder uri is not a tree uri", e)
            return null
        }
        val candidates = lrcDocIdCandidates(treeDocId, row.relativePath, lrcFileNameFor(row.displayName))
        for (candidate in candidates) {
            val uri = DocumentsContract.buildDocumentUriUsingTree(lyricsFolderUri, candidate)
            val text = try {
                context.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
            } catch (e: SecurityException) {
                Log.w(TAG, "lrc read denied — folder grant lost?", e)
                continue
            } catch (e: FileNotFoundException) {
                Log.w(TAG, "no lrc at candidate path", e)
                continue
            } catch (e: IOException) {
                Log.w(TAG, "lrc read failed", e)
                continue
            }
            if (text.isNullOrBlank()) continue
            lyricsFromText(text).takeIf { it != Lyrics.EMPTY }?.let { return it }
        }
        return null
    }

    private data class AudioRow(val id: Long, val displayName: String, val relativePath: String)
}

/**
 * Whether a library entry (`row*`) is the track that is playing. Pure and Compose-free so the
 * matching rules are testable without a ContentResolver.
 *
 * A duration of 0 on either side is a wildcard: MediaStore reports NULL durations on devices that
 * never scanned the tags, and mp3 tags carry no duration at all.
 */
fun rowMatches(
    rowTitle: String,
    rowArtist: String,
    rowDurationMs: Long,
    title: String,
    artist: String,
    durationMs: Long,
): Boolean {
    if (normalizeForMatch(rowTitle) != normalizeForMatch(title)) return false
    val normalizedRowArtist = normalizeForMatch(rowArtist)
    val normalizedArtist = normalizeForMatch(artist)
    val artistMatches = normalizedRowArtist == normalizedArtist ||
        // "feat." credits land in the artist tag for one side only often enough that a
        // containment check recovers more matches than it costs in false positives.
        (normalizedArtist.isNotEmpty() && normalizedArtist in normalizedRowArtist) ||
        (normalizedRowArtist.isNotEmpty() && normalizedRowArtist in normalizedArtist)
    if (!artistMatches) return false
    if (durationMs > 0L && rowDurationMs > 0L && abs(rowDurationMs - durationMs) > DurationMatchToleranceMs) {
        return false
    }
    return true
}

/** Escapes a value for a SQL `LIKE ? ESCAPE '\'` argument so wildcards in it match literally. */
fun escapeLike(value: String): String =
    value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")

/**
 * Document ids to try inside the user's lyrics tree, most specific first: the album subfolder
 * mirrored from the track's MediaStore `RELATIVE_PATH`, then the flat folder root.
 *
 * [treeDocId] looks like `primary:Music`; only the part after the colon is a path, and it is the
 * prefix that [relativePath] (`Music/Album/`) shares with the tree. Pure and Compose-free so the
 * string surgery is testable without a ContentResolver.
 */
fun lrcDocIdCandidates(treeDocId: String, relativePath: String, lrcFileName: String): List<String> {
    val flat = "$treeDocId/$lrcFileName"
    val treePath = treeDocId.substringAfterLast(':').trim('/')
    val trackPath = relativePath.trim('/')
    if (treePath.isEmpty() || trackPath.isEmpty()) return listOf(flat)
    if (trackPath != treePath && !trackPath.startsWith("$treePath/")) return listOf(flat)
    val remainder = trackPath.removePrefix(treePath).trim('/')
    if (remainder.isEmpty()) return listOf(flat)
    return listOf("$treeDocId/$remainder/$lrcFileName", flat)
}
