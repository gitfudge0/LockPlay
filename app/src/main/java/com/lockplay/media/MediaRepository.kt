package com.lockplay.media

import android.media.session.MediaController
import androidx.annotation.MainThread
import com.lockplay.model.NowPlaying
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Process-wide singleton holding the current [NowPlaying] state and a handle to the active
 * [MediaController] for transport commands.
 *
 * The [MediaListenerService] writes state and sets the controller; the lockscreen UI and the
 * screen-off trigger read state, and the UI issues transport commands. A plain object is the
 * lazy-correct choice — there is exactly one media state per process and no need for DI.
 */
object MediaRepository {
    private val _nowPlaying = MutableStateFlow(NowPlaying.EMPTY)
    val nowPlaying: StateFlow<NowPlaying> = _nowPlaying.asStateFlow()

    /** The controller currently driving [nowPlaying]; null when nothing is playing. */
    @Volatile
    private var controller: MediaController? = null

    fun update(state: NowPlaying) {
        _nowPlaying.value = state
    }

    fun setController(controller: MediaController?) {
        this.controller = controller
    }

    fun clear() {
        controller = null
        _nowPlaying.value = NowPlaying.EMPTY
    }

    /** True when something is actively playing — the gate for launching the lockscreen on screen-off. */
    fun isPlaying(): Boolean = _nowPlaying.value.let { it.isActive && it.isPlaying }

    // --- Transport (no-ops when no controller is attached) ---
    @MainThread fun play() = controller?.transportControls?.play()

    @MainThread fun pause() = controller?.transportControls?.pause()

    @MainThread fun next() = controller?.transportControls?.skipToNext()

    @MainThread fun previous() = controller?.transportControls?.skipToPrevious()

    @MainThread fun seekTo(positionMs: Long) = controller?.transportControls?.seekTo(positionMs)

    @MainThread fun togglePlayPause() {
        if (isPlaying()) pause() else play()
    }
}
