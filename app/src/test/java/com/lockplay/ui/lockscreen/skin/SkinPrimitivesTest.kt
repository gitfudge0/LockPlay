package com.lockplay.ui.lockscreen.skin

import org.junit.Assert.assertEquals
import org.junit.Test

class SkinPrimitivesTest {

    // ── volumeFraction ────────────────────────────────────────────────────────

    @Test
    fun `volumeFraction returns 0 when max is zero`() {
        assertEquals(0f, volumeFraction(5, 0), 0f)
    }

    @Test
    fun `volumeFraction returns 0 when max is negative`() {
        assertEquals(0f, volumeFraction(5, -1), 0f)
    }

    @Test
    fun `volumeFraction clamps above 1 when current exceeds max`() {
        assertEquals(1f, volumeFraction(20, 15), 0f)
    }

    @Test
    fun `volumeFraction clamps to 0 for negative current`() {
        assertEquals(0f, volumeFraction(-3, 15), 0f)
    }

    @Test
    fun `volumeFraction returns correct fraction for mid-range value`() {
        assertEquals(0.5f, volumeFraction(7, 14), 0.0001f)
    }

    @Test
    fun `volumeFraction returns 1 when current equals max`() {
        assertEquals(1f, volumeFraction(15, 15), 0f)
    }

    // ── formatClock ───────────────────────────────────────────────────────────

    @Test
    fun `formatClock 8h00m returns 8 colon 00`() {
        assertEquals("8:00", formatClock(8, 0))
    }

    @Test
    fun `formatClock midnight with offset returns 12 colon 05`() {
        assertEquals("12:05", formatClock(0, 5))
    }

    @Test
    fun `formatClock 13h08m returns 1 colon 08`() {
        assertEquals("1:08", formatClock(13, 8))
    }

    @Test
    fun `formatClock 10h08m returns 10 colon 08`() {
        assertEquals("10:08", formatClock(10, 8))
    }

    @Test
    fun `formatClock noon returns 12 colon 00`() {
        assertEquals("12:00", formatClock(12, 0))
    }

    @Test
    fun `formatClock 23h59m returns 11 colon 59`() {
        assertEquals("11:59", formatClock(23, 59))
    }
}
