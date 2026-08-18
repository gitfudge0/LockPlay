package com.lockplay.ui.lockscreen

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lockplay.design.AppThemeProvider
import com.lockplay.design.DefaultTheme
import com.lockplay.design.ThemeController
import com.lockplay.lyrics.LyricsController
import com.lockplay.trigger.LockLauncher
import com.lockplay.ui.lockscreen.skin.DefaultSkin
import com.lockplay.ui.lockscreen.skin.SkinController
import com.lockplay.ui.lockscreen.skin.SkinOrientation
import kotlinx.coroutines.launch

/**
 * Activity shown over the keyguard when music is playing. It is purely a host: it configures the
 * show-when-locked window flags, wires the selected [com.lockplay.ui.lockscreen.skin.PlayerSkin] to
 * the [LockscreenViewModel], forces orientation to match the skin, and finishes (revealing the
 * underlying secure keyguard) on swipe-up or when playback stops.
 */
class LockscreenActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Must be set before content so the window appears above the keyguard and wakes the screen.
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Hide the status bar (and let it slide back in on swipe). Immersive, edge-to-edge lockscreen.
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.statusBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        // We're on screen now — clear the launch notification so it doesn't linger behind us.
        LockLauncher.cancel(this)

        val skinController = SkinController(applicationContext)
        val themeController = ThemeController(applicationContext)
        val lyricsController = LyricsController(applicationContext)

        setContent {
            val skin by skinController.skin.collectAsStateWithLifecycle(initialValue = DefaultSkin)
            val vm: LockscreenViewModel = viewModel()
            val state by vm.state.collectAsStateWithLifecycle()
            val theme by themeController.theme.collectAsStateWithLifecycle(initialValue = DefaultTheme)
            val lyricsEnabled by lyricsController.enabled.collectAsStateWithLifecycle(initialValue = false)
            val lyricsHintSeen by lyricsController.hintSeen.collectAsStateWithLifecycle(initialValue = false)
            val coachMarkSeen by lyricsController.coachMarkSeen.collectAsStateWithLifecycle(initialValue = false)
            val scope = rememberCoroutineScope()

            // Force the orientation the active skin was designed for. The built-in skins are all
            // portrait today; the LANDSCAPE arm stays for any future device-shaped skin.
            LaunchedEffect(skin.orientation) {
                requestedOrientation = when (skin.orientation) {
                    SkinOrientation.PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    SkinOrientation.LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                }
            }

            // Music stopped → leave the lockscreen (do NOT dismiss the secure keyguard).
            LaunchedEffect(state.isActive) {
                if (!state.isActive) finish()
            }

            // Tokens for the lyrics overlay only. Skins never read them (S6), so this does not
            // restyle any skin.
            AppThemeProvider(theme) {
                LockscreenScreen(
                    skin = skin,
                    state = state,
                    onSeek = vm::seekTo,
                    onPrev = vm::previous,
                    onPlayPause = vm::togglePlayPause,
                    onNext = vm::next,
                    onUnlock = { finish() },
                    lyricsEnabled = lyricsEnabled,
                    lyricsHintSeen = lyricsHintSeen,
                    onLyricsHintSeen = { scope.launch { lyricsController.markHintSeen() } },
                    coachMarkSeen = coachMarkSeen,
                    onCoachMarkSeen = { scope.launch { lyricsController.markCoachMarkSeen() } },
                )
            }
        }
    }
}
