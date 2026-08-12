package com.lockplay.ui.lockscreen.skin

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.delay
import java.time.LocalTime

/**
 * Small, skin-agnostic building blocks shared by the player skins. Anything bigger or design-specific
 * lives inside the individual skin file — these are only the bits every other skin would otherwise
 * re-implement (spin, time/clock formatting, device volume, album art).
 */

/** Formats a millisecond duration as mm:ss (clamped at zero). */
fun formatTime(ms: Long): String {
    val totalSeconds = ms.coerceAtLeast(0L) / 1000L
    return "%d:%02d".format(totalSeconds / 60L, totalSeconds % 60L)
}

/** Volume level as 0f..1f from a stream's current/max, guarding max<=0. */
fun volumeFraction(current: Int, max: Int): Float =
    if (max <= 0) 0f else (current.toFloat() / max).coerceIn(0f, 1f)

/** 12-hour wall-clock string ("8:00", "12:05", "1:08") — no leading-zero hour, no AM/PM. */
fun formatClock(hour24: Int, minute: Int): String {
    val h = hour24 % 12
    return "%d:%02d".format(if (h == 0) 12 else h, minute)
}

/** The current wall-clock time via [formatClock], recomputed periodically. */
@Composable
fun rememberClockText(): String {
    // ponytail: 10s re-tick instead of aligning to the minute boundary — a lockscreen is up
    // briefly and a worst-case 10s stale minute is invisible; align to :00 only if it ever shows.
    var now by remember { mutableStateOf(LocalTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = LocalTime.now()
            delay(10_000)
        }
    }
    return formatClock(now.hour, now.minute)
}

/** Progress fraction in 0f..1f for the ticking [SkinScope.position] against the track duration. */
fun SkinScope.progressFraction(): Float {
    val d = durationMs.coerceAtLeast(1L)
    return (position().toFloat() / d).coerceIn(0f, 1f)
}

/**
 * Continuous rotation angle (degrees) that advances while [playing] and freezes when paused,
 * resuming from where it stopped. [periodMillis] is the time for one full turn. Returns a
 * `() -> Float` whose angle read happens only when the lambda is invoked — read it inside a
 * `Modifier.graphicsLayer { rotationZ = spin() }` so the per-frame state read invalidates the draw
 * layer alone and the caller does NOT recompose each frame.
 */
@Composable
fun rememberSpinAngle(playing: Boolean, periodMillis: Int): () -> Float {
    val angle = remember { Animatable(0f) }
    LaunchedEffect(playing, periodMillis) {
        if (playing) {
            angle.animateTo(
                targetValue = angle.value + 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(periodMillis, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart,
                ),
            )
        } else {
            angle.stop()
        }
    }
    return { angle.value % 360f }
}

/**
 * Album artwork from the session, cropped to [shape]. Falls back to a [fallback] brush fill (a tinted
 * gradient placeholder) when the session provides no bitmap, so device skins always have a cover.
 */
@Composable
fun SkinAlbumArt(
    scope: SkinScope,
    shape: Shape,
    modifier: Modifier = Modifier,
    fallback: Brush = Brush.linearGradient(listOf(Color(0xFF3A3A4A), Color(0xFF1A1A24))),
) {
    val bmp = scope.state.albumArt
    if (bmp != null) {
        Image(
            bitmap = bmp.asImageBitmap(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier.clip(shape),
        )
    } else {
        Box(modifier = modifier.clip(shape).background(fallback))
    }
}
