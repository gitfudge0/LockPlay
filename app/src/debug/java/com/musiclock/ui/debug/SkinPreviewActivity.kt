package com.musiclock.ui.debug

import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.musiclock.R
import com.musiclock.model.NowPlaying
import com.musiclock.ui.lockscreen.skin.SkinScope
import com.musiclock.ui.lockscreen.skin.skinById

/**
 * Debug-only harness to render a single skin full-screen with realistic sample data, so design
 * screenshots can be captured per skin and per light/dark mode without driving the real lockscreen
 * (which needs media playback + notification access). Not part of the release build.
 *
 * Launch: `adb shell am start -n com.musiclock.debug/com.musiclock.ui.debug.SkinPreviewActivity -e skin <id>`
 * where <id> is a BuiltInSkins id (card, turntable, glass). Light/dark follows the device ui-mode.
 */
class SkinPreviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Match the real LockscreenActivity: hide the status bar so the in-skin clock is the only one.
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView)
            .hide(WindowInsetsCompat.Type.statusBars())

        val art = BitmapFactory.decodeResource(resources, R.drawable.sample_album)
        val sample = NowPlaying(
            isActive = true,
            title = "Redbone",
            artist = "Childish Gambino",
            albumArt = art,
            isPlaying = true,
            positionMs = 64_000L,
            durationMs = 326_000L,
        )
        val skin = skinById(intent.getStringExtra("skin"))

        setContent {
            Box(Modifier.fillMaxSize()) {
                skin.content(
                    SkinScope(
                        state = sample,
                        position = { 64_000L },
                        onSeek = {}, onPrev = {}, onPlayPause = {}, onNext = {},
                    ),
                )
            }
        }
    }
}
