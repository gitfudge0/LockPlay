package com.lockplay.lyrics

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.lyricsDataStore by preferencesDataStore(name = "lyrics")
private val Enabled = booleanPreferencesKey("enabled")
private val HintSeen = booleanPreferencesKey("hint_seen")
private val CoachMarkSeen = booleanPreferencesKey("coach_mark_seen")

/**
 * Persists whether the opt-in lyrics feature is on, and whether the one-time discoverability hint
 * has been seen. Mirrors [com.lockplay.ui.onboarding.OnboardingController] (DataStore-backed flags).
 *
 * Only preference booleans live here — never lyrics text, title, artist, or album (X3/X4).
 */
class LyricsController(private val context: Context) {

    /** Emits whether lyrics fetching is enabled. Defaults to false: off until explicitly turned on. */
    val enabled: Flow<Boolean> = context.lyricsDataStore.data.map { it[Enabled] ?: false }

    /** Emits whether the one-time discoverability hint has been shown. Defaults to false. */
    val hintSeen: Flow<Boolean> = context.lyricsDataStore.data.map { it[HintSeen] ?: false }

    /** Emits whether the one-time lyrics-pill coach mark has been shown. Defaults to false. */
    val coachMarkSeen: Flow<Boolean> = context.lyricsDataStore.data.map { it[CoachMarkSeen] ?: false }

    suspend fun setEnabled(value: Boolean) {
        context.lyricsDataStore.edit { it[Enabled] = value }
    }

    suspend fun markHintSeen() {
        context.lyricsDataStore.edit { it[HintSeen] = true }
    }

    suspend fun markCoachMarkSeen() {
        context.lyricsDataStore.edit { it[CoachMarkSeen] = true }
    }
}
