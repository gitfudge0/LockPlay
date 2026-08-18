package com.lockplay.ui.lockscreen.skin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Card (portrait, variant A) — a clean, light/dark card layout: centered album art with rounded
 * corners, large title, a stylised bar-meter seek strip, and a standard transport row. All colours
 * are resolved locally from a two-palette (light/dark) set; AppTheme / MaterialTheme tokens are
 * intentionally ignored so the skin looks identical regardless of app theming.
 *
 * Light palette: bg #FBFBFD, ink #14141A.
 * Dark palette:  bg #0E0E12, ink #F3F3F6.
 * Muted label colour: #9A9AA6 in both modes.
 */

/**
 * Deterministic normalised height for bar [k] in the Card skin's bar-meter progress strip.
 * Returns a value in 0f..1f derived from a fixed waveform so the silhouette looks natural without
 * any animation state.
 *
 * Formula: `h = 0.2 + |sin(k * 1.7) * cos(k * 0.6)| * 0.8`
 */
fun cardBarHeight(k: Int): Float =
    0.2f + abs(sin(k * 1.7) * cos(k * 0.6)).toFloat() * 0.8f

private val MutedColor = Color(0xFF9A9AA6)
private const val BAR_COUNT = 48

@Composable
fun CardSkin(scope: SkinScope) {
    val dark = isSystemInDarkTheme()
    val bg = if (dark) Color(0xFF0E0E12) else Color(0xFFFBFBFD)
    val ink = if (dark) Color(0xFFF3F3F6) else Color(0xFF14141A)
    // Play circle: inverted (dark ink circle in light mode, light circle in dark)
    val circleColor = if (dark) Color(0xFFF3F3F6) else Color(0xFF14141A)
    val circleIcon = if (dark) Color(0xFF0E0E12) else Color(0xFFFBFBFD)

    val clock = rememberClockText()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bg),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 26.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Top bar: chevron-down left, "Now Playing" centered, clock right
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Rounded.KeyboardArrowDown,
                    contentDescription = null,
                    tint = ink,
                    modifier = Modifier.size(24.dp),
                )
                Text(
                    text = "Now Playing",
                    color = ink,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = clock,
                    color = ink,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Spacer(Modifier.height(20.dp))

            // Album art: square, 26dp horizontal margin (already in parent padding), drop shadow
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
            ) {
                SkinAlbumArt(
                    scope = scope,
                    shape = RoundedCornerShape(26.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .shadow(elevation = 12.dp, shape = RoundedCornerShape(26.dp)),
                )
                scope.lyricsPill?.let { pill ->
                    Box(
                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp),
                    ) { pill() }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Title
            Text(
                text = scope.title,
                color = ink,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(6.dp))

            // Artist
            Text(
                text = scope.artist,
                color = MutedColor,
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(20.dp))

            // Bar-meter progress + times
            CardBarMeter(scope = scope, ink = ink)

            Spacer(Modifier.height(20.dp))

            // Controls row: prev / play/pause / next
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(36.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Rounded.SkipPrevious,
                    contentDescription = "Previous",
                    tint = ink,
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .clickable(onClick = scope.onPrev),
                )
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(circleColor)
                        .clickable(onClick = scope.onPlayPause),
                ) {
                    Icon(
                        if (scope.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (scope.isPlaying) "Pause" else "Play",
                        tint = circleIcon,
                        modifier = Modifier.size(30.dp),
                    )
                }
                Icon(
                    Icons.Rounded.SkipNext,
                    contentDescription = "Next",
                    tint = ink,
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .clickable(onClick = scope.onNext),
                )
            }
        }
    }
}

/**
 * Stylised bar-meter that doubles as the seek control. The bar heights are static and deterministic
 * (see [cardBarHeight]) — they represent the track "shape" visually but carry no real audio data.
 *
 * ponytail: bar heights are derived from a fixed sinusoidal formula and do NOT reflect actual audio
 * waveform or frequency content. This is a decorative progress indicator only.
 */
@Composable
private fun CardBarMeter(scope: SkinScope, ink: Color) {
    // Key on scope: a track change hands in a new SkinScope, and durationMs is a plain value
    // (not Compose state) — a keyless remember would keep dividing by the old track's length.
    val fraction by remember(scope) { derivedStateOf { scope.progressFraction() } }
    val duration = scope.durationMs
    val faintColor = ink.copy(alpha = 0.15f)

    Column(modifier = Modifier.fillMaxWidth()) {
        // Bar strip — tappable to seek, but only when the duration is known (live streams report 0).
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .pointerInput(duration) {
                    detectTapGestures { offset ->
                        if (duration > 0) scope.onSeek((offset.x / size.width * duration).toLong())
                    }
                },
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            val played = (fraction * BAR_COUNT).toInt()
            repeat(BAR_COUNT) { k ->
                val barFrac = cardBarHeight(k)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(barFrac)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (k < played) ink else faintColor),
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        // Times row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = formatTime(scope.position()),
                color = MutedColor,
                fontSize = 12.sp,
            )
            if (scope.durationMs > 0) {
                Text(
                    text = formatTime(scope.durationMs),
                    color = MutedColor,
                    fontSize = 12.sp,
                )
            }
        }
    }
}
