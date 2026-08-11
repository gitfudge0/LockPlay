package com.lockplay.ui.lockscreen

import androidx.lifecycle.ViewModel
import com.lockplay.media.MediaRepository
import com.lockplay.model.NowPlaying
import kotlinx.coroutines.flow.StateFlow

/**
 * Thin MVVM bridge between the lockscreen UI and the process-wide [MediaRepository].
 *
 * Holds no state of its own: it re-exposes the repository's [NowPlaying] flow and forwards
 * transport commands. The auto-ticking progress is handled in the UI layer, not here.
 */
class LockscreenViewModel : ViewModel() {

    /** Current playback state, observed by the lockscreen. */
    val state: StateFlow<NowPlaying> = MediaRepository.nowPlaying

    fun seekTo(positionMs: Long) = MediaRepository.seekTo(positionMs)
    fun previous() = MediaRepository.previous()
    fun togglePlayPause() = MediaRepository.togglePlayPause()
    fun next() = MediaRepository.next()
}
