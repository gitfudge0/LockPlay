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

    @Test
    fun `only the cassette rotates its own content`() {
        assertEquals(90f, CassetteSkinSpec.contentRotation, 0f)
        assertEquals(0f, CardSkinSpec.contentRotation, 0f)
        assertEquals(0f, TurntableSkinSpec.contentRotation, 0f)
        assertEquals(0f, GlassSkinSpec.contentRotation, 0f)
        assertEquals(0f, EditorialSkinSpec.contentRotation, 0f)
    }

    @Test
    fun `every skin declares a quarter-turn multiple for its content rotation`() {
        assertTrue(BuiltInSkins.all { it.contentRotation % 90f == 0f })
    }
}
