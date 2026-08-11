package com.lockplay.ui.lockscreen.skin

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.skinDataStore by preferencesDataStore(name = "skin")
private val SKIN_ID = stringPreferencesKey("skin_id")

/**
 * Persists and exposes the selected [PlayerSkin]. The lockscreen observes [skin] to render and to
 * decide its orientation; the launcher screen writes via [select]. Mirrors
 * [com.lockplay.design.ThemeController] (skin and color theme are orthogonal choices).
 */
class SkinController(private val context: Context) {

    val skin: Flow<PlayerSkin> = context.skinDataStore.data.map { skinById(it[SKIN_ID]) }

    suspend fun select(skin: PlayerSkin) {
        context.skinDataStore.edit { it[SKIN_ID] = skin.id }
    }
}
