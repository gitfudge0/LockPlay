package com.lockplay.ui.lockscreen.skin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CardSkinTest {

    @Test
    fun `cardBarHeight returns value in 0f to 1f for all bar indices`() {
        repeat(48) { k ->
            val h = cardBarHeight(k)
            assertTrue("bar $k height $h out of range", h in 0f..1f)
        }
    }

    @Test
    fun `cardBarHeight minimum is at least 0_2`() {
        repeat(48) { k ->
            val h = cardBarHeight(k)
            assertTrue("bar $k height $h below 0.2", h >= 0.2f)
        }
    }

    @Test
    fun `cardBarHeight is deterministic`() {
        repeat(48) { k ->
            assertEquals(cardBarHeight(k), cardBarHeight(k), 0f)
        }
    }

    @Test
    fun `cardBarHeight k=0 equals 0_2`() {
        // sin(0)*cos(0) = 0, so h = 0.2 + 0 = 0.2
        assertEquals(0.2f, cardBarHeight(0), 0.0001f)
    }
}
