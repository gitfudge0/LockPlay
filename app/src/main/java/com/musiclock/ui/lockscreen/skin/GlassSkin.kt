package com.musiclock.ui.lockscreen.skin

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Glass (portrait, variant C) — full-bleed album art over the top ~46% of the screen with the
 * clock overlaid, a thin coral scrubber line at the art's bottom edge, and a frosted gradient
 * control panel filling the rest. All colours are resolved locally from a two-palette (light/dark)
 * set; AppTheme / MaterialTheme tokens are intentionally ignored so the skin looks identical
 * regardless of app theming.
 *
 * Light panel: #EEF0F6 → #DFE3EE, ink #1C2333.
 * Dark panel:  #1B1E29 → #11131A, ink #E7EBF6.
 * Seek fill: #FF5A5F. Track: #FFFFFF40.
 */
@Composable
fun GlassSkin(scope: SkinScope) {
    val dark = isSystemInDarkTheme()
    val panelStart = if (dark) Color(0xFF1B1E29) else Color(0xFFEEF0F6)
    val panelEnd = if (dark) Color(0xFF11131A) else Color(0xFFDFE3EE)
    val ink = if (dark) Color(0xFFE7EBF6) else Color(0xFF1C2333)
    val inkMuted = ink.copy(alpha = 0.6f)

    val clock = rememberClockText()

    Column(modifier = Modifier.fillMaxSize()) {
        // Art section — full-bleed square (1:1)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
        ) {
            SkinAlbumArt(
                scope = scope,
                shape = RectangleShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(),
            )

            // Clock over the photo, top-start with status bar padding
            Text(
                text = clock,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
            )

            // Thin 3dp scrubber line at the photo's bottom edge
            GlassScrubber(
                scope = scope,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }

        // Frosted control panel — fills the remaining space below the square art
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Brush.verticalGradient(listOf(panelStart, panelEnd)))
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Times row hugs the scrubber just above it…
            GlassTimesRow(scope = scope, muted = inkMuted)

            // …then the title/controls cluster centres in the remaining space.
            Spacer(Modifier.weight(1f))

            // Title
            Text(
                text = scope.title,
                color = ink,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(4.dp))

            // Artist
            Text(
                text = scope.artist,
                color = inkMuted,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(20.dp))

            // Transport controls: prev / play-pause / next
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(48.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Rounded.SkipPrevious,
                    contentDescription = "Previous",
                    tint = ink,
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .clickable(onClick = scope.onPrev),
                )
                Icon(
                    if (scope.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (scope.isPlaying) "Pause" else "Play",
                    tint = ink,
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .clickable(onClick = scope.onPlayPause),
                )
                Icon(
                    Icons.Rounded.SkipNext,
                    contentDescription = "Next",
                    tint = ink,
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .clickable(onClick = scope.onNext),
                )
            }

            Spacer(Modifier.weight(1f))
        }
    }
}

/** Thin 3dp scrubber strip at the bottom of the art section; tappable to seek. */
@Composable
private fun GlassScrubber(scope: SkinScope, modifier: Modifier = Modifier) {
    val fraction by remember(scope) { derivedStateOf { scope.progressFraction() } }
    val duration = scope.durationMs

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(3.dp)
            .pointerInput(duration) {
                detectTapGestures { offset ->
                    // Seek only when duration is known (live streams report 0).
                    if (duration > 0) scope.onSeek((offset.x / size.width * duration).toLong())
                }
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        // Track
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .background(Color(0x40FFFFFF)),
        )
        // Fill
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction)
                .fillMaxHeight()
                .background(Color(0xFFFF5A5F)),
        )
    }
}

/** Times row: elapsed left, remaining right (omitted when duration is unknown). */
@Composable
private fun GlassTimesRow(scope: SkinScope, muted: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = formatTime(scope.position()),
            color = muted,
            fontSize = 12.sp,
        )
        if (scope.durationMs > 0) {
            Text(
                text = "-" + formatTime(scope.durationMs - scope.position()),
                color = muted,
                fontSize = 12.sp,
            )
        }
    }
}
