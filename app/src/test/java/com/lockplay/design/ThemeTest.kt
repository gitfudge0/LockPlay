package com.lockplay.design

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeTest {

    @Test
    fun `themeById returns the matching theme`() {
        assertSame(Glassmorphism, themeById("glassmorphism"))
        assertSame(MinimalistDark, themeById("minimalist_dark"))
    }

    @Test
    fun `themeById falls back to default for unknown or null`() {
        assertSame(DefaultTheme, themeById(null))
        assertSame(DefaultTheme, themeById("nope"))
    }

    @Test
    fun `built-in theme ids are unique and non-empty`() {
        val ids = BuiltInThemes.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        assertTrue(ids.all { it.isNotBlank() })
    }

    @Test
    fun `glass theme uses glass background style`() {
        assertEquals(BackgroundStyle.GLASS, Glassmorphism.backgroundStyle)
        assertEquals(BackgroundStyle.SOLID, MinimalistDark.backgroundStyle)
    }
}
