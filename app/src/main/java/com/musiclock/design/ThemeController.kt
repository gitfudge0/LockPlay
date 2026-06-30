package com.musiclock.design

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.themeDataStore by preferencesDataStore(name = "theme")
private val THEME_ID = stringPreferencesKey("theme_id")

/**
 * Persists and exposes the selected app-wide [ThemeSpec]. Both the main app and the lockscreen read
 * the same flow, so a theme change is reflected everywhere.
 */
class ThemeController(private val context: Context) {

    val theme: Flow<ThemeSpec> = context.themeDataStore.data.map { themeById(it[THEME_ID]) }

    suspend fun select(spec: ThemeSpec) {
        context.themeDataStore.edit { it[THEME_ID] = spec.id }
    }
}
