package com.lockplay.ui.lockscreen

import android.os.SystemClock
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import com.lockplay.model.NowPlaying
import com.lockplay.ui.lockscreen.skin.PlayerSkin
import com.lockplay.ui.lockscreen.skin.SkinScope
import kotlinx.coroutines.delay

/** Upward drag distance (px) past which a release triggers unlock. */
private const val UnlockThresholdPx = -180f

/** Seed the ticker from the real sample extrapolated to now (see [NowPlaying.positionAt]). */
private fun seedPosition(state: NowPlaying): Long = state.positionAt(SystemClock.elapsedRealtime())

/**
 * The lockscreen surface. Hosts the cross-cutting behaviour every skin shares — an auto-ticking
 * playback position and swipe-up-to-unlock — then hands the full surface to the selected [skin] to
 * paint however it likes via a [SkinScope]. The skin owns all layout, colors and chrome.
 */
@Composable
fun LockscreenScreen(
    skin: PlayerSkin,
    state: NowPlaying,
    onSeek: (Long) -> Unit,
    onPrev: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onUnlock: () -> Unit,
) {
    // --- Auto-ticking position (hoisted ticker; skins read it lazily so reads stay scoped) ---
    var displayedPosition by remember { mutableLongStateOf(seedPosition(state)) }
    LaunchedEffect(state.isPlaying, state.positionMs, state.positionUpdateTime) {
        displayedPosition = seedPosition(state)
        if (state.isPlaying) {
            val step = (1000f * state.playbackSpeed).toLong()
            while (true) {
                delay(1000)
                displayedPosition = (displayedPosition + step)
                    .coerceAtMost(if (state.durationMs > 0) state.durationMs else Long.MAX_VALUE)
            }
        }
    }

    // --- Swipe-to-unlock drag tracking ---
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val offsetY by animateFloatAsState(
        targetValue = dragOffset,
        animationSpec = tween(150),
        label = "unlockDrag",
    )
    val dragModifier = Modifier.pointerInput(Unit) {
        detectVerticalDragGestures(
            onVerticalDrag = { _, dragAmount ->
                dragOffset = (dragOffset + dragAmount).coerceAtMost(0f)
            },
            onDragEnd = { if (dragOffset <= UnlockThresholdPx) onUnlock() else dragOffset = 0f },
            onDragCancel = { dragOffset = 0f },
        )
    }

    val scope = SkinScope(
        state = state,
        position = { displayedPosition },
        onSeek = onSeek,
        onPrev = onPrev,
        onPlayPause = onPlayPause,
        onNext = onNext,
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(dragModifier)
            .graphicsLayer { translationY = offsetY },
    ) {
        skin.content(scope)
    }
}
