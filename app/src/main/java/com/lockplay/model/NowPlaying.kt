package com.lockplay.model

import android.graphics.Bitmap
import androidx.compose.runtime.Immutable

/**
 * Single source of truth for what is currently playing, exposed by [com.lockplay.media.MediaRepository].
 *
 * [Immutable] so Compose can skip recomposition when an unchanged instance is re-emitted.
 */
@Immutable
data class NowPlaying(
    val isActive: Boolean = false,
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val albumArt: Bitmap? = null,
    val isPlaying: Boolean = false,
    /** Position reported by the session, in ms, at the instant [positionUpdateTime] was captured. */
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    /** [android.os.SystemClock.elapsedRealtime] when [positionMs] was sampled; lets the UI extrapolate. */
    val positionUpdateTime: Long = 0L,
    /** Playback speed (1.0 = normal); used to extrapolate position between updates. */
    val playbackSpeed: Float = 1f,
) {
    /**
     * Position extrapolated to [elapsedRealtimeNow] (an [android.os.SystemClock.elapsedRealtime]
     * value): the reported [positionMs] plus the playback-rate-scaled time since it was sampled,
     * clamped to [0, durationMs]. When paused or unsampled, returns [positionMs] unchanged.
     */
    fun positionAt(elapsedRealtimeNow: Long): Long {
        val advance = if (isPlaying && positionUpdateTime > 0L) {
            ((elapsedRealtimeNow - positionUpdateTime).coerceAtLeast(0L) * playbackSpeed).toLong()
        } else {
            0L
        }
        val upper = if (durationMs > 0L) durationMs else Long.MAX_VALUE
        return (positionMs + advance).coerceIn(0L, upper)
    }

    companion object {
        val EMPTY = NowPlaying()
    }
}
