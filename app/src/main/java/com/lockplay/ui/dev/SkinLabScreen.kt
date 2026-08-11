package com.lockplay.ui.dev

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lockplay.ui.gallery.SampleTracks
import com.lockplay.ui.lockscreen.skin.BuiltInSkins
import com.lockplay.ui.lockscreen.skin.SkinScope
import kotlinx.coroutines.delay

/**
 * Dev harness. Renders one skin full-screen (real size, not a scaled card) with a live ticking
 * position + play/pause so we can eyeball edits without walking onboarding. Tap the top pill to cycle
 * skins, ✕ to close. Reached only in debug builds via [DevSkinLabOverlay].
 */
@Composable
fun SkinLabScreen(onClose: () -> Unit = {}) {
    var skinIndex by rememberSaveable { mutableIntStateOf(BuiltInSkins.size - 1) } // start on the last (newest) skin
    val skin = BuiltInSkins[skinIndex]
    val base = SampleTracks[skinIndex % SampleTracks.size]

    var playing by remember(skinIndex) { mutableStateOf(true) }
    var position by remember(skinIndex) { mutableLongStateOf(base.positionMs) }

    LaunchedEffect(skinIndex, playing) {
        while (playing) {
            delay(1000)
            position = (position + 1000).let { if (base.durationMs > 0) it % base.durationMs else it }
        }
    }

    val scope = remember(skin, playing) {
        SkinScope(
            state = base.copy(isPlaying = playing),
            position = { position },
            onSeek = { position = it },
            onPrev = { position = 0L },
            onPlayPause = { playing = !playing },
            onNext = { position = 0L },
        )
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        skin.content(scope)

        // Skin switcher pill, floating on top so its taps win over the skin's transport.
        Box(
            modifier = Modifier
                .safeDrawingPadding()
                .padding(8.dp)
                .align(Alignment.TopCenter),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                LabButton("‹") { skinIndex = (skinIndex - 1 + BuiltInSkins.size) % BuiltInSkins.size }
                LabButton("${skin.displayName}  ${skinIndex + 1}/${BuiltInSkins.size}") {
                    skinIndex = (skinIndex + 1) % BuiltInSkins.size
                }
                LabButton("›") { skinIndex = (skinIndex + 1) % BuiltInSkins.size }
                LabButton("✕", onClose)
            }
        }
    }
}

/**
 * Debug-only overlay: a small corner button that opens the full-screen [SkinLabScreen]. Drop this into
 * the launcher (guarded by `BuildConfig.DEBUG`) so the normal welcome/gallery flow ships unchanged but
 * the lab stays one tap away in debug builds.
 */
@Composable
fun DevSkinLabOverlay() {
    var open by rememberSaveable { mutableStateOf(false) }
    if (open) {
        SkinLabScreen(onClose = { open = false })
        return
    }
    Box(Modifier.fillMaxSize().safeDrawingPadding()) {
        Text(
            text = "🧪",
            fontSize = 15.sp,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp)
                .background(Color(0x66000000), RoundedCornerShape(8.dp))
                .clickable { open = true }
                .padding(horizontal = 9.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun LabButton(label: String, onClick: () -> Unit) {
    Text(
        text = label,
        color = Color.White,
        fontSize = 13.sp,
        fontFamily = FontFamily.Monospace,
        modifier = Modifier
            .clickable(onClick = onClick)
            .background(Color(0xCC000000), RoundedCornerShape(6.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
    )
}
