package com.musiclock.ui.permissions

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.musiclock.BuildConfig
import com.musiclock.design.AppThemeProvider
import com.musiclock.design.DefaultTheme
import com.musiclock.design.ThemeController
import com.musiclock.design.components.SolidBackground
import com.musiclock.ui.gallery.PlayerGalleryScreen
import com.musiclock.ui.lockscreen.skin.SkinController
import com.musiclock.ui.onboarding.OnboardingController
import com.musiclock.ui.onboarding.OnboardingFlow

/**
 * Launcher screen. First run shows the onboarding wizard ([OnboardingFlow]); once finished (persisted
 * via [OnboardingController]) it routes straight to the player gallery ([PlayerGalleryScreen]). Themed
 * by the persisted [ThemeController] selection so it restyles live; the skin selection
 * ([SkinController]) drives the lockscreen.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val themeController = ThemeController(applicationContext)
        val skinController = SkinController(applicationContext)
        val onboardingController = OnboardingController(applicationContext)

        setContent {
            val theme by themeController.theme.collectAsStateWithLifecycle(initialValue = DefaultTheme)
            // null until DataStore reports, so returning users don't flash the wizard before the gallery.
            val completed by onboardingController.completed.collectAsStateWithLifecycle(initialValue = null)
            // Local override: opening main from the wizard navigates instantly, ahead of the async write.
            var openedMain by rememberSaveable { mutableStateOf(false) }

            AppThemeProvider(theme) {
                // SolidBackground bleeds edge-to-edge; safeDrawingPadding keeps the UI clear of the
                // status/nav bars so the bottom CTAs aren't hidden under the system bars.
                SolidBackground {
                    Box(Modifier.fillMaxSize()) {
                        Box(Modifier.fillMaxSize().safeDrawingPadding()) {
                            when {
                                completed == true || openedMain ->
                                    PlayerGalleryScreen(
                                        skinController = skinController,
                                        themeController = themeController,
                                    )
                                completed == false ->
                                    OnboardingFlow(
                                        skinController = skinController,
                                        onOpenMain = { openedMain = true },
                                        onFinished = {},
                                    )
                                // completed == null → still loading; SolidBackground shows the themed backdrop.
                                else -> Unit
                            }
                        }
                        // Debug builds only: a corner button to open the skin lab for dev testing.
                        if (BuildConfig.DEBUG) {
                            com.musiclock.ui.dev.DevSkinLabOverlay()
                        }
                    }
                }
            }
        }
    }
}
