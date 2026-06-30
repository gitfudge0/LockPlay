package com.musiclock.media

import com.musiclock.model.NowPlaying
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaRepositoryTest {

    @After
    fun tearDown() = MediaRepository.clear()

    @Test
    fun `starts empty and not playing`() {
        MediaRepository.clear()
        assertEquals(NowPlaying.EMPTY, MediaRepository.nowPlaying.value)
        assertFalse(MediaRepository.isPlaying())
    }

    @Test
    fun `isPlaying requires both active and playing`() {
        MediaRepository.update(NowPlaying(isActive = true, isPlaying = false))
        assertFalse(MediaRepository.isPlaying())

        MediaRepository.update(NowPlaying(isActive = false, isPlaying = true))
        assertFalse(MediaRepository.isPlaying())

        MediaRepository.update(NowPlaying(isActive = true, isPlaying = true))
        assertTrue(MediaRepository.isPlaying())
    }

    @Test
    fun `clear resets state`() {
        MediaRepository.update(NowPlaying(isActive = true, isPlaying = true, title = "x"))
        MediaRepository.clear()
        assertEquals(NowPlaying.EMPTY, MediaRepository.nowPlaying.value)
    }

    @Test
    fun `transport is a no-op without a controller`() {
        MediaRepository.clear()
        // Should not throw when nothing is attached.
        MediaRepository.play()
        MediaRepository.pause()
        MediaRepository.next()
        MediaRepository.previous()
        MediaRepository.seekTo(1000)
        MediaRepository.togglePlayPause()
    }
}
