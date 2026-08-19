package com.lockplay.ui.gallery

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GallerySettingsTest {

    @Test
    fun `turning lyrics on requires confirmation`() {
        assertTrue(requiresConfirmation(true))
    }

    @Test
    fun `turning lyrics off does not require confirmation`() {
        assertFalse(requiresConfirmation(false))
    }

    @Test
    fun `local lyrics section shows when lyrics enabled`() {
        assertTrue(showLocalLyricsSection(true))
    }

    @Test
    fun `local lyrics section hidden when lyrics disabled`() {
        assertFalse(showLocalLyricsSection(false))
    }
}
