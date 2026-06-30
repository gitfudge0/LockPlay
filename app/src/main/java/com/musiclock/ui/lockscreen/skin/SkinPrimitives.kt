package com.musiclock.ui.lockscreen.skin

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Small, skin-agnostic building blocks shared by the player skins. Anything bigger or design-specific
 * lives inside the individual skin file — these are only the bits every other skin would otherwise
 * re-implement (spin, equalizer bars, time formatting, album art).
 */

/** Formats a millisecond duration as mm:ss (clamped at zero). */
fun formatTime(ms: Long): String {
    val totalSeconds = ms.coerceAtLeast(0L) / 1000L
    return "%d:%02d".format(totalSeconds / 60L, totalSeconds % 60L)
}

/** Progress fraction in 0f..1f for the ticking [SkinScope.position] against the track duration. */
fun SkinScope.progressFraction(): Float {
    val d = durationMs.coerceAtLeast(1L)
    return (position().toFloat() / d).coerceIn(0f, 1f)
}

/**
 * Continuous rotation angle (degrees) that advances while [playing] and freezes when paused,
 * resuming from where it stopped. [periodMillis] is the time for one full turn. Drive a spinning
 * record / reel / disc with `Modifier.rotate(rememberSpin(...))`.
 */
@Composable
fun rememberSpin(playing: Boolean, periodMillis: Int): Float {
    val angle = remember { Animatable(0f) }
    LaunchedEffect(playing, periodMillis) {
        if (playing) {
            // Animate by a full turn on infinite restart; the 360°→0° wrap is visually identical,
            // so the restart is invisible while the rotation stays continuous from the current angle.
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
    return angle.value % 360f
}

/**
 * A simple animated equalizer: [bars] vertical bars whose heights oscillate out of phase while
 * [playing], collapsing to a thin idle line when paused. Fills the given [modifier]'s box.
 */
@Composable
fun EqualizerBars(
    bars: Int,
    color: Color,
    playing: Boolean,
    modifier: Modifier = Modifier,
    barShape: Shape = androidx.compose.foundation.shape.RoundedCornerShape(2.dp),
) {
    val transition = rememberInfiniteTransition(label = "eq")
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        repeat(bars) { i ->
            // Each bar gets its own period/phase so the row never looks like it pulses in unison.
            val frac by transition.animateFloat(
                initialValue = 0.2f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(420 + (i * 53) % 360, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "bar$i",
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(if (playing) frac else 0.08f)
                    .clip(barShape)
                    .background(color),
            )
        }
    }
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

/** A faint corner time chip — the only clock a skin shows, deliberately demoted. */
@Composable
fun TinyTime(time: String, color: Color, modifier: Modifier = Modifier) {
    androidx.compose.material3.Text(
        text = time,
        color = color,
        style = androidx.compose.ui.text.TextStyle(
            fontSize = 11.sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
        ),
        modifier = modifier,
    )
}
