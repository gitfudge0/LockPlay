package com.musiclock.ui.onboarding

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.onboardingDataStore by preferencesDataStore(name = "onboarding")
private val COMPLETED = booleanPreferencesKey("completed")

/**
 * Persists whether the onboarding wizard has been finished, so later launches can skip straight to
 * the gallery. Mirrors [com.musiclock.design.ThemeController] (a single DataStore-backed flag).
 */
class OnboardingController(private val context: Context) {

    /** Emits true once the user has opened the main app from the end of the wizard. */
    val completed: Flow<Boolean> = context.onboardingDataStore.data.map { it[COMPLETED] ?: false }

    suspend fun markComplete() {
        context.onboardingDataStore.edit { it[COMPLETED] = true }
    }
}
