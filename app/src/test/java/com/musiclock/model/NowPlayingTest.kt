package com.musiclock.model

import org.junit.Assert.assertEquals
import org.junit.Test

class NowPlayingTest {

    @Test
    fun `extrapolates position while playing`() {
        val s = NowPlaying(
            isActive = true, isPlaying = true,
            positionMs = 10_000, durationMs = 200_000,
            positionUpdateTime = 1_000, playbackSpeed = 1f,
        )
        // 5s of wall-clock since the sample -> +5000ms.
        assertEquals(15_000, s.positionAt(6_000))
    }

    @Test
    fun `scales by playback speed`() {
        val s = NowPlaying(
            isActive = true, isPlaying = true,
            positionMs = 0, durationMs = 200_000,
            positionUpdateTime = 1_000, playbackSpeed = 2f,
        )
        assertEquals(20_000, s.positionAt(11_000)) // 10s * 2x
    }

    @Test
    fun `paused position does not advance`() {
        val s = NowPlaying(
            isActive = true, isPlaying = false,
            positionMs = 42_000, durationMs = 200_000,
            positionUpdateTime = 1_000,
        )
        assertEquals(42_000, s.positionAt(999_000))
    }

    @Test
    fun `clamps to duration`() {
        val s = NowPlaying(
            isActive = true, isPlaying = true,
            positionMs = 190_000, durationMs = 200_000,
            positionUpdateTime = 1_000, playbackSpeed = 1f,
        )
        assertEquals(200_000, s.positionAt(1_000_000))
    }

    @Test
    fun `unknown duration does not clamp`() {
        val s = NowPlaying(
            isActive = true, isPlaying = true,
            positionMs = 1_000, durationMs = 0,
            positionUpdateTime = 1_000, playbackSpeed = 1f,
        )
        assertEquals(11_000, s.positionAt(11_000))
    }
}
