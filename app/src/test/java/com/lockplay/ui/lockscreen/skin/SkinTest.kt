package com.lockplay.ui.lockscreen.skin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class SkinTest {

    @Test
    fun `skinById returns the matching skin`() {
        assertSame(CardSkinSpec, skinById("card"))
        assertSame(GlassSkinSpec, skinById("glass"))
    }

    @Test
    fun `skinById falls back to default for unknown or null`() {
        assertSame(DefaultSkin, skinById(null))
        assertSame(DefaultSkin, skinById("nope"))
    }

    @Test
    fun `built-in skin ids are unique and non-empty`() {
        val ids = BuiltInSkins.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        assertTrue(ids.all { it.isNotBlank() })
    }

    @Test
    fun `default skin is in the catalogue`() {
        assertTrue(BuiltInSkins.contains(DefaultSkin))
    }

    @Test
    fun `all three built-in skins are portrait`() {
        assertEquals(SkinOrientation.PORTRAIT, CardSkinSpec.orientation)
        assertEquals(SkinOrientation.PORTRAIT, TurntableSkinSpec.orientation)
        assertEquals(SkinOrientation.PORTRAIT, GlassSkinSpec.orientation)
    }
}
